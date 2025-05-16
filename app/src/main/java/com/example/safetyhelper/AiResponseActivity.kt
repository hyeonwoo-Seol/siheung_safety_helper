package com.example.safetyhelper

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
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
import android.widget.Toast
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
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
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
    private var tempImageUri: Uri? = null
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Firebase 초기화
        FirebaseApp.initializeApp(this)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isBig = prefs.getBoolean(KEY_BIG_TEXT_MODE, false)

        window.statusBarColor = Color.WHITE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        // 레이아웃 선택 후 툴바 설정
        val rootView: View = if (!isBig) {
            val binding = ActivityAiResponseBinding.inflate(layoutInflater)
            setContentView(binding.root)
            setupToolbar(binding.toolbar, isBig)
            binding.root
        } else {
            val bigBinding = ActivityAiResponseBigBinding.inflate(layoutInflater)
            setContentView(bigBinding.root)
            setupToolbar(bigBinding.toolbar, isBig)
            bigBinding.root
        }

        // 공통 뷰 초기화
        setupViewsCommon(rootView)
    }

    private fun setupToolbar(toolbar: Toolbar, isBig: Boolean) {
        setSupportActionBar(toolbar)
        toolbar.setOnClickListener {
            prefs.edit()
                .putBoolean(KEY_BIG_TEXT_MODE, !isBig)
                .apply()
            recreate()
        }
    }

    private fun setupViewsCommon(root: View) {
        val scrollView        = root.findViewById<ScrollView>(R.id.scrollView)
        val selectedImageView = root.findViewById<ImageView>(R.id.selectedImageView)
        val loadBtn           = root.findViewById<Button>(R.id.loadImageButton)
        val issueInput        = root.findViewById<EditText>(R.id.issueInput)
        val sendButton        = root.findViewById<Button>(R.id.sendButton)
        val responseText      = root.findViewById<TextView>(R.id.responseText)
        val sendImageBtn      = root.findViewById<Button>(R.id.sendImageButton)

        // 테스트용 데이터
        val location = "시흥시 정왕동 121"
        val name     = "설현우"

        sendImageBtn.setOnClickListener {
            val textToSave = responseText.text.toString().trim()
            if (textToSave.isEmpty()) {
                Toast.makeText(this, "저장할 텍스트가 없습니다.", Toast.LENGTH_SHORT).show()
            } else {
                saveResponseToInternalStorage(textToSave)
                uploadResponseToFirebase(textToSave)
                Toast.makeText(this, "텍스트가 저장되고 Firebase에 업로드되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }

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
                        updateResponseText(responseText, scrollView, result)
                    } else {
                        val err = getString(R.string.error_server, resp.code())
                        responseText.text = err
                    }
                } catch (e: Exception) {
                    val err = getString(R.string.error_network, e.localizedMessage)
                    responseText.text = err
                }
            }
        }

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
        ) { uri -> uri?.let { handleImageUri(root, scrollView, it) } }

        photoPickerLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri -> uri?.let { handleImageUri(root, scrollView, it) } }

        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && tempImageUri != null) {
                handleImageUri(root, scrollView, tempImageUri!!)
            }
        }

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

    private fun handleImageUri(root: View, scrollView: ScrollView, uri: Uri) {
        selectedImageUri = uri
        root.findViewById<ImageView>(R.id.selectedImageView).apply {
            visibility = View.VISIBLE
            setImageURI(uri)
        }
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun updateResponseText(responseText: TextView, scrollView: ScrollView, text: String) {
        responseText.text = text
        responseText.post {
            val lines      = responseText.lineCount
            val lineHeight = responseText.lineHeight
            val padding    = responseText.compoundPaddingTop + responseText.compoundPaddingBottom
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

    /**
     * Firebase Firestore에 텍스트 저장
     */
    private fun uploadResponseToFirebase(text: String) {
        val db = Firebase.firestore
        val timestamp = System.currentTimeMillis()
        val data = mapOf(
            "text" to text,
            "timestamp" to timestamp
        )
        db.collection("responses")
            .add(data)
            .addOnSuccessListener { Toast.makeText(this, "Firebase 저장 성공", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(this, "Firebase 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_complaint_list -> {
                val intent = Intent(this, ComplaintListActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

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
}