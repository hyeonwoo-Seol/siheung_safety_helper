package com.example.safetyhelper

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.safetyhelper.databinding.ActivitySplashBinding
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1) SplashScreen API 초기화
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // 2) Edge-to-edge 설정
        enableEdgeToEdge()

        // 3) 뷰 바인딩
        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 4) 시스템 바 인셋 처리 (옵션)
        ViewCompat.setOnApplyWindowInsetsListener(binding.splash) { view, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(sysBars.left, sysBars.top, sysBars.right, sysBars.bottom)
            insets
        }

        // 5) 로그인 및 이름 입력 여부 확인
        val auth = FirebaseAuth.getInstance()
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val savedName = prefs.getString("KEY_USER_NAME", null)

        // 6) 분기할 Intent 결정
        val targetIntent = when {
            auth.currentUser == null -> {
                // 로그인 안 된 경우
                Intent(this, SignInActivity::class.java)
            }
            savedName.isNullOrEmpty() -> {
                // 로그인 O, 이름 미입력
                Intent(this, NameActivity::class.java)
            }
            else -> {
                // 로그인 O, 이름 O → 메인 화면
                Intent(this, MainScreen::class.java)
            }
        }.apply {
            // 기존 스택을 모두 지우고 새 태스크로 실행
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // 7) 화면 전환 및 Splash 종료
        startActivity(targetIntent)
        finish()
    }
}
