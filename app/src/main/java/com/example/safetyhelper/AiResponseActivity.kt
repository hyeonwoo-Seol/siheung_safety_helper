package com.example.safetyhelper

import android.app.AlertDialog
import android.content.ContentValues
import android.net.Uri
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.safetyhelper.databinding.ActivityAiResponseBinding

class AiResponseActivity : AppCompatActivity() {
    private lateinit var binding : ActivityAiResponseBinding
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAiResponseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val scrollView = binding.scrollView
        val selectedImageView = binding.selectedImageView
        val loadBtn = binding.loadImageButton


        // 갤러리에서 이미지 가져오기
        galleryLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) {uri ->
            uri?.let {handleImageUri(it, selectedImageView, scrollView)}
        }

        // 카메라로 사진 찍기
        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                imageUri?.let { handleImageUri(it, selectedImageView, scrollView)}
            }
        }
        //버튼 클릭
        loadBtn.setOnClickListener{
            val options = arrayOf("갤러리에서 선택", "카메라로 촬영")
            AlertDialog.Builder(this)
                .setTitle("이미지 불러오기")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            //이미지 타입 필터
                            galleryLauncher.launch("image/*")
                        }
                        1 -> {
                            //MediaStore 에 빈 Uri 생성 후 카메라 촬영
                            imageUri = createImageUri()
                            imageUri?.let { cameraLauncher.launch(it)}
                        }
                    }
                }
                .show()
        }
    }

    private fun createImageUri(): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        return contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )!!
    }

    //선택된 이미지 Uri 처리
    private fun handleImageUri(uri: Uri, selectedImageView: android.widget.ImageView,
                               scrollView: android.widget.ScrollView) {
        selectedImageView.apply {
            visibility = View.VISIBLE
            setImageURI(uri)
        }
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN)}

    }
}