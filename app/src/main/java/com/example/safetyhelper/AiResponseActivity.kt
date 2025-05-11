package com.example.safetyhelper


import android.Manifest
import androidx.core.content.FileProvider
import java.io.File
import android.content.Intent
import android.app.AlertDialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ScrollView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.safetyhelper.databinding.ActivityAiResponseBinding
//import com.example.safetyhelper.DialogFullscreenImageBinding
import com.example.safetyhelper.databinding.DialogFullscreenImageBinding
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.view.updatePadding
import androidx.core.view.WindowCompat
import android.view.Menu
import android.view.MenuItem
import android.app.Dialog
import android.content.Context
import android.view.Window

class AiResponseActivity : AppCompatActivity() {
    private lateinit var binding : ActivityAiResponseBinding
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private var imageUri: Uri? = null
    private var tempImageUri: Uri? = null
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var photoPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAiResponseBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val scrollView = binding.scrollView
        val selectedImageView = binding.selectedImageView
        val loadBtn = binding.loadImageButton

        val toolbar = binding.toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "민원 작성하기"





        //다른 액티비티에서 전달 받기
        //val location = intent.getStringExtra("location") ?: ""
        //val name     = intent.getStringExtra("name")     ?: ""

        //테스트 용도
        val location = "시흥시 정왕동 121"
        val name = "설현우"

        binding.sendButton.setOnClickListener {
            val issue = binding.issueInput.text.toString().trim()
            if (issue.isEmpty()) {
                binding.responseText.text = "이슈를 입력해주세요."
                return@setOnClickListener
            }
            lifecycleScope.launch {
                binding.responseText.text = "요청 중..."
                try {
                    val resp = RetrofitClient.apiService.getLLMResponse(
                        ApiRequest(location, name, issue)
                    )
                    if (resp.isSuccessful && resp.body() != null) {
                        val result = resp.body()!!.result
                        binding.responseText.text = result

                        saveResponseToInternalStorage(result)
                        updateResponseText(result)
                    } else {
                        val err = getString(R.string.error_server, resp.code())
                        binding.responseText.text = err
                        saveResPonseToInternalStorage(err)
                    }
                } catch (e: Exception) {
                    val err = getString(R.string.error_network, e.localizedMessage)
                    binding.responseText.text = err
                    saveResponseToInternalStorage(err)
                }
            }
        }

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { perms ->
            val cameraOk = perms[Manifest.permission.CAMERA]?:false
            val readImagesOk = perms[Manifest.permission.READ_MEDIA_IMAGES]?:false

            when {
                cameraOk -> {
                    launchCamera()
                }

                readImagesOk -> {
                    galleryLauncher.launch("image/*")
                }
                else -> {
                    showPermissionDeniedDialog()
                }
            }
        }
        // 갤러리에서 이미지 가져오기
        galleryLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) {uri ->
            uri?.let {handleImageUri(it)}
        }

        photoPickerLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            uri?.let {handleImageUri(it)}
        }

        // 카메라로 사진 찍기
        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && tempImageUri  != null) {
                handleImageUri(tempImageUri!!)
            }
        }
        //버튼 클릭
        binding.loadImageButton.setOnClickListener{
            AlertDialog.Builder(this)
                .setTitle("이미지 불러오기")
                .setItems(arrayOf("갤러리에서 선택", "카메라로 촬영")) { _, which ->
                    when (which) {
                        0 -> {
                            when {
                                Build.VERSION.SDK_INT >= 34 -> {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                                    requestPermissionLauncher.launch(arrayOf(
                                        Manifest.permission.READ_MEDIA_IMAGES
                                    ))
                                }
                                else -> {
                                    galleryLauncher.launch("image/*")
                                }
                            }
                        }

                        1 -> {
                            if (ContextCompat.checkSelfPermission(
                                    this, Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                launchCamera()
                            } else {
                                requestPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.CAMERA
                                    )
                                )
                            }
                        }
                    }
                }
                .show()
                }

        // 전체화면 보기
        binding.selectedImageView.setOnClickListener{
            val dialogBinding = DialogFullscreenImageBinding.inflate(layoutInflater)
            dialogBinding.fullscreenImageView.setImageDrawable(binding.selectedImageView.drawable)

            val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(dialogBinding.root)

            dialog.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.parseColor("#F3F4F6")))
                decorView.setPadding(0,0,0,0)
                setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            dialog.show()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // res/menu/toolbar_menu.xml 을 툴바 메뉴로 인플레이트
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_overflow -> {
                // TODO: Overflow 아이콘 클릭 시 동작 구현
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
            val resList = packageManager.queryIntentActivities(captureIntent, PackageManager.MATCH_DEFAULT_ONLY)
            resList.forEach{
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
            .setMessage("카메라 권한이 거부되어 기능을 사용할 수 없습니다.설정에서 권한을 허용해주세요.")
            .setPositiveButton("확인", null)
            .show()
    }

    private fun createImageUri(): Uri {
        // 1) 파일명도 실제 타임스탬프가 들어가도록 수정
        val timestamp = System.currentTimeMillis()
        val imagesDir = File(cacheDir, "images").apply { if (!exists()) mkdirs() }
        val imageFile = File(imagesDir, "IMG_${timestamp}.jpg")

        // 2) authority에 실제 패키지명이 들어가도록 문자열 템플릿 사용
        val authority = "$packageName.provider"
        return FileProvider.getUriForFile(
            this,
            authority,
            imageFile
        )
    }

    //선택된 이미지 Uri 처리
    private fun handleImageUri(uri: Uri) {
        binding.selectedImageView.apply {
            visibility = View.VISIBLE
            setImageURI(uri)
        }
        binding.scrollView.post { binding.scrollView.fullScroll(View.FOCUS_DOWN)}
    }

    private fun updateResponseText(text: String){
        binding.responseText.text = text

        binding.responseText.post {
            val lines = binding.responseText.lineCount
            val lineHeight = binding.responseText.lineHeight
            val padding = binding.responseText.compoundPaddingTop +
                    binding.responseText.compoundPaddingBottom

            (binding.responseText.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
                params.height = lines * lineHeight + padding
                binding.responseText.layoutParams = params
            }
        }

        binding.scrollView.post { binding.scrollView.fullScroll(View.FOCUS_DOWN)}
    }
    
    //내용 저장하기
    private fun saveResponseToInternalStorage(response: String) {
        val timestamp = System.currentTimeMillis()
        val filename = "response_${timestamp}.txt"
        openFileOutput(filename, Context.MODE_PRIVATE).use { fos ->
            fos.write(response.toByteArray())
        }
    }
    
    // 저장된 내용을 불러오기
    private fun loadAllSavedResponses(): List<String> {
        return filesDir.listFiles()
            ?.filter { it.name.startsWith("response_") && it.name.endsWith(".txt") }
            ?.map { file ->
                file.readText()
            }
            ?: emptyList()
    }

}