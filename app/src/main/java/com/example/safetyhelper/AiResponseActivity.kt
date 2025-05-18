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
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.safetyhelper.databinding.ActivityAiResponseBigBinding
import com.example.safetyhelper.databinding.ActivityAiResponseBinding
import com.example.safetyhelper.databinding.DialogFullscreenImageBinding
import com.google.android.material.button.MaterialButton
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
//firebase에 이미지를 보낼 때 사용하는 import문
// import com.google.firebase.storage.ktx.storage



class AiResponseActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME        = "app_settings"
        private const val KEY_BIG_TEXT_MODE = "big_text_mode"
    }

    private fun animateTyping(
        editText: EditText,
        scrollView: ScrollView,
        text: String,
        charDelay:Long = 25L
    ) {
        editText.setText("")
        lifecycleScope.launch {
            text.forEach { ch ->
                editText.append(ch.toString())
                scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
                delay(charDelay)
            }
        }
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var photoPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>

    private var tempImageUri: Uri? = null
    private var selectedImageUri: Uri? = null
    private var locationString: String = ""
    private val name = "설현우"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Firebase 초기화
        FirebaseApp.initializeApp(this)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isBig = prefs.getBoolean(KEY_BIG_TEXT_MODE, false)

        // 위치 권한 요청
        locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                fetchLocation()
            } else {
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        window.statusBarColor = Color.WHITE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        // 레이아웃 선택 후 툴바 설정
        val rootView: View = if (!isBig) {
            val binding = ActivityAiResponseBinding.inflate(layoutInflater)
            setContentView(binding.root)
            binding.root
        } else {
            val bigBinding = ActivityAiResponseBigBinding.inflate(layoutInflater)
            setContentView(bigBinding.root)
            bigBinding.root
        }


        // 공통 뷰 초기화
        setupViewsCommon(rootView)
    }


    override fun onBackPressed() {
        // MainScreen으로 돌아가는 인텐트 생성
        val intent = Intent(this, MainScreen::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)

        // 반드시 super.onBackPressed() 호출해서
        // 기본 뒤로가기 동작(현재 액티비티 종료)을 수행합니다.
        super.onBackPressed()
    }


    private fun setupViewsCommon(root: View) {
        val scrollView        = root.findViewById<ScrollView>(R.id.scrollView)
        val selectedImageView = root.findViewById<ImageView>(R.id.selectedImageView)
        val loadBtn           = root.findViewById<FrameLayout>(R.id.loadImageButton)
        val issueInput        = root.findViewById<EditText>(R.id.issueInput)
        val sendButton        = root.findViewById<MaterialButton>(R.id.sendButton)
        val responseText      = root.findViewById<EditText>(R.id.responseText)
        val sendImageBtn      = root.findViewById<Button>(R.id.sendImageButton)

        sendImageBtn.setOnClickListener {
            val textToSave = responseText.text.toString().trim()
            if (textToSave.isEmpty()) {
                Toast.makeText(this, "전송할 민원이 없습니다. 내용을 작성 후 전송 버튼을 눌러주세요", Toast.LENGTH_SHORT).show()
            } else {
                saveResponseToInternalStorage(textToSave)
                //uploadResponseToFirebase(textToSave)  //테스트 중 서버 트래픽 방지
                Toast.makeText(this, "민원을 성공적으로 전송했습니다.", Toast.LENGTH_SHORT).show()

                // 텍스트 초기화 및 MainScreen으로 이동
                responseText.setText("")
                val intent = Intent(this@AiResponseActivity, MainScreen::class.java)
                startActivity(intent)
                finish()
            }
        }

        sendButton.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(issueInput.windowToken, 0)

            val issue = issueInput.text.toString().trim()
            if (issue.isEmpty()) {
                responseText.setText("이슈를 입력해주세요")
                return@setOnClickListener
            }
            lifecycleScope.launch {
                responseText.setText("요청 중")
                try {
                    val resp = RetrofitClient.apiService.getLLMResponse(
                        ApiRequest(locationString, name, issue)
                    )
                    if (resp.isSuccessful && resp.body() != null) {
                        val result = resp.body()!!.result
                        animateTyping(editText = responseText,
                            scrollView = scrollView,
                            text = result,
                            charDelay = 20L)
                    } else {
                        val err = getString(R.string.error_server, resp.code())
                        responseText.setText(err)
                    }
                } catch (e: Exception) {
                    val err = getString(R.string.error_network, e.localizedMessage)
                    responseText.setText(err)
                }
            }
        }

        issueInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrBlank()
                sendButton.isEnabled = hasText
                sendImageBtn.isEnabled = hasText

            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 위치 권한 외의 기존 권한/뷰 초기화 로직 유지
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

    /**
     * 위치 정보를 가져와 주소 문자열로 변환
     */
    private fun fetchLocation() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        val lastLoc: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        lastLoc?.let {
            val lat = it.latitude
            val lng = it.longitude
            val geocoder = Geocoder(this, Locale.KOREA)
            val list = geocoder.getFromLocation(lat, lng, 1)  // 네트워크 기반 리버스 지오코딩
            if (!list.isNullOrEmpty()) {
                val addr = list[0]
                // 1) 도로명
                val roadName      = addr.thoroughfare ?: ""
                // 2) 건물 번호
                val buildingNum   = addr.subThoroughfare ?: ""
                // 3) 동·읍·면 정보 (필요시)
                val subLocality   = addr.subLocality   ?: ""
                // 4) 시·도, 구·군
                val locality      = addr.locality      ?: ""
                val adminArea     = addr.adminArea     ?: ""

                // 원하는 포맷으로 조합
                locationString = buildString {
                    if (adminArea.isNotEmpty()) append(adminArea)
                    if (locality   .isNotEmpty()) append(" $locality")
                    if (subLocality.isNotEmpty()) append(" $subLocality")
                    if (roadName   .isNotEmpty()) append(" $roadName")
                    if (buildingNum.isNotEmpty()) append(" $buildingNum")
                }.trim()

                Log.d("AiResponseActivity", "locationString = $locationString")
            }
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
            (responseText.layoutParams as? ViewGroup.LayoutParams)?.let { params ->
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
            //.addOnSuccessListener { documentRef ->
            //            // ---- 이미지 업로드 기능 (현재는 사용하지 않음) ----
            //            //
            //            // selectedImageUri?.let { uri ->
            //            //     val storage   = Firebase.storage
            //            //     val imageRef  = storage.reference.child("responses/images/${documentRef.id}.jpg")
            //            //     imageRef.putFile(uri)
            //            //         .addOnSuccessListener {
            //            //             imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
            //            //                 documentRef.update("imageUrl", downloadUri.toString())
            //            //             }
            //            //         }
            //            //         .addOnFailureListener { e ->
            //            //             Toast.makeText(this, "이미지 업로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            //            //         }
            //            // }
            //            // ---------------------------------------------
            .addOnFailureListener { e -> Toast.makeText(this, "민원 전송 실패: ${e.message}", Toast.LENGTH_SHORT).show() }
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
