package com.example.safetyhelper

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.safetyhelper.databinding.ActivityAiResponseBigBinding
import com.example.safetyhelper.databinding.ActivityAiResponseBinding
import com.example.safetyhelper.databinding.DialogFullscreenImageBinding
import kotlinx.coroutines.launch
import java.io.File

class AiResponseActivity : AppCompatActivity() {
    companion object {
        private const val PREFS_NAME        = "app_settings"
        private const val KEY_BIG_TEXT_MODE = "big_text_mode"
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var photoPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) SharedPreferences 읽기
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isBig = prefs.getBoolean(KEY_BIG_TEXT_MODE, false)

        if (!isBig) {
            // 2a) 일반 레이아웃 바인딩
            val binding = ActivityAiResponseBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupToolbar(binding.toolbar, isBig)
            setupViews(binding)
        } else {
            // 2b) 큰글씨 레이아웃 바인딩
            val binding = ActivityAiResponseBigBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupToolbar(binding.toolbar, isBig)
            setupViews(binding)
        }
    }

    /** 툴바 클릭 시 모드 토글 & Activity 재실행 */
    private fun setupToolbar(toolbar: Toolbar, isBig: Boolean) {
        setSupportActionBar(toolbar)
        toolbar.setOnClickListener {
            prefs.edit()
                .putBoolean(KEY_BIG_TEXT_MODE, !isBig)
                .apply()
            recreate()  // onCreate() 다시 실행
        }
    }

    /** 일반 모드 및 큰글씨 모드 공통 뷰 초기화 (ActivityAiResponseBinding) */
    private fun setupViews(binding: ActivityAiResponseBinding) {
        // 뷰 참조
        val scrollView         = binding.scrollView
        val selectedImageView  = binding.selectedImageView
        val loadBtn            = binding.loadImageButton
        val issueInput         = binding.issueInput
        val sendButton         = binding.sendButton
        val responseText       = binding.responseText

        // 테스트용 위치·이름 (원래는 Intent로 받으세요)
        val location = "시흥시 정왕동 121"
        val name     = "설현우"

        // 서버 요청 버튼
        sendButton.setOnClickListener {
            val issue = issueInput.text.toString().trim()
            if (issue.isEmpty()) {
                responseText.text = "이슈를 입력해주세요."
                return@setOnClickListener
            }
            lifecycleScope.launch {
                responseText.text = "요청 중..."
                try {
                    val resp = RetrofitClient.apiService.getLLMResponse(
                        ApiRequest(location, name, issue)
                    )
                    if (resp.isSuccessful && resp.body() != null) {
                        val result = resp.body()!!.result
                        responseText.text = result
                        saveResponseToInternalStorage(result)
                        updateResponseText(responseText, scrollView, result)
                    } else {
                        val err = getString(R.string.error_server, resp.code())
                        responseText.text = err
                        saveResponseToInternalStorage(err)
                    }
                } catch (e: Exception) {
                    val err = getString(R.string.error_network, e.localizedMessage)
                    responseText.text = err
                    saveResponseToInternalStorage(err)
                }
            }
        }

        // 권한 런처 등록
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { perms ->
            val cameraOk     = perms[Manifest.permission.CAMERA] ?: false
            val readImagesOk = perms[Manifest.permission.READ_MEDIA_IMAGES] ?: false
            when {
                cameraOk     -> launchCamera()
                readImagesOk -> galleryLauncher.launch("image/*")
                else         -> showPermissionDeniedDialog()
            }
        }
        galleryLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri -> uri?.let { handleImageUri(binding, scrollView, it) } }
        photoPickerLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri -> uri?.let { handleImageUri(binding, scrollView, it) } }
        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && tempImageUri != null) {
                handleImageUri(binding, scrollView, tempImageUri!!)
            }
        }

        // 이미지 불러오기 버튼
        loadBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("이미지 불러오기")
                .setItems(arrayOf("갤러리에서 선택", "카메라로 촬영")) { _, which ->
                    when (which) {
                        0 -> when {
                            Build.VERSION.SDK_INT >= 34 -> photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> requestPermissionLauncher.launch(
                                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                            )
                            else -> galleryLauncher.launch("image/*")
                        }
                        1 -> checkCameraPermissionAndLaunch()
                    }
                }
                .show()
        }

        // 전체화면 보기
        selectedImageView.setOnClickListener {
            val dialogBinding = DialogFullscreenImageBinding.inflate(layoutInflater)
            dialogBinding.fullscreenImageView.setImageDrawable(selectedImageView.drawable)
            val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(dialogBinding.root)
            dialog.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.parseColor("#F3F4F6")))
                decorView.setPadding(0, 0, 0, 0)
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
            dialog.show()
        }
    }

    /** 큰글씨 모드 레이아웃 초기화 (ActivityAiResponseBigBinding) */
    private fun setupViews(binding: ActivityAiResponseBigBinding) {
        // 동일한 IDs이므로, 호출하는 메서드만 바인딩 타입이 다릅니다.
        // 나머지 코드는 setupViews(ActivityAiResponseBinding)와 완전히 동일합니다.
        // 따라서 아래처럼 호출해도 좋습니다:
        // (단, 임시 Uri는 class 멤버로 유지)
        @Suppress("UNCHECKED_CAST")
        setupViews(binding as ActivityAiResponseBinding)
    }

    // 이하 모든 private 메서드는 기존과 동일하게 유지됩니다.
    private var tempImageUri: Uri? = null

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> launchCamera()
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ->
                showPermissionRationaleDialog()
            else ->
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
        }
    }

    private fun launchCamera() {
        tempImageUri = createImageUri()
        tempImageUri?.let { uri ->
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            packageManager.queryIntentActivities(captureIntent, PackageManager.MATCH_DEFAULT_ONLY)
                .forEach {
                    grantUriPermission(
                        it.activityInfo.packageName,
                        uri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            cameraLauncher.launch(uri)
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("권한 필요")
            .setMessage("카메라 촬영을 위해 카메라 권한이 필요합니다.")
            .setPositiveButton("확인") { _, _ ->
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("권한 거부됨")
            .setMessage("카메라 권한이 거부되어 기능을 사용할 수 없습니다. 설정에서 권한을 허용해주세요.")
            .setPositiveButton("확인", null)
            .show()
    }

    private fun createImageUri(): Uri {
        val timestamp = System.currentTimeMillis()
        val imagesDir = File(cacheDir, "images").apply { if (!exists()) mkdirs() }
        val imageFile = File(imagesDir, "IMG_$timestamp.jpg")
        val authority = "$packageName.provider"
        return FileProvider.getUriForFile(this, authority, imageFile)
    }

    private fun handleImageUri(
        binding: ActivityAiResponseBinding,
        scrollView: ScrollView,
        uri: Uri
    ) {
        binding.selectedImageView.apply {
            visibility = View.VISIBLE
            setImageURI(uri)
        }
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun updateResponseText(
        responseText: TextView,
        scrollView: ScrollView,
        text: String
    ) {
        responseText.text = text
        responseText.post {
            val lines     = responseText.lineCount
            val lineHeight = responseText.lineHeight
            val padding    = responseText.compoundPaddingTop +
                    responseText.compoundPaddingBottom
            (responseText.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
                params.height = lines * lineHeight + padding
                responseText.layoutParams = params
            }
        }
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun saveResponseToInternalStorage(response: String) {
        val timestamp = System.currentTimeMillis()
        val filename  = "response_$timestamp.txt"
        openFileOutput(filename, Context.MODE_PRIVATE).use { fos ->
            fos.write(response.toByteArray())
        }
    }

    private fun loadAllSavedResponses(): List<String> =
        filesDir.listFiles()
            ?.filter { it.name.startsWith("response_") && it.name.endsWith(".txt") }
            ?.map { file -> file.readText() }
            ?: emptyList()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_overflow -> true
            else                 -> super.onOptionsItemSelected(item)
        }
}
