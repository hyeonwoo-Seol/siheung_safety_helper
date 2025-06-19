package com.example.safetyhelper

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.safetyhelper.databinding.ActivityNameBinding
import utils.ThemeHelper

class NameActivity : AppCompatActivity() {
    companion object {
        private const val PREFS_USER    = "user_prefs"    // SharedPreferences 파일명
        private const val KEY_USER_NAME = "KEY_USER_NAME" // 사용자 이름 저장 키
    }

    // SharedPreferences 인스턴스를 전역처럼 사용하기 위해 선언
    private lateinit var userPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyDarkMode(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 뷰 바인딩 설정
        val binding = ActivityNameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1) SharedPreferences 초기화
        userPrefs = getSharedPreferences(PREFS_USER, MODE_PRIVATE)

        // 2) 저장된 이름이 있으면 바로 MainScreen으로 이동 (이름 입력 화면 건너뜀)
        val savedName = userPrefs.getString(KEY_USER_NAME, null)
        if (!savedName.isNullOrEmpty()) {
            startActivity(Intent(this, MainScreen::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        // 3) 시스템 바 인셋 처리
        ViewCompat.setOnApplyWindowInsetsListener(binding.name) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 4) 키보드 엔터 키 처리
        binding.nameEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                binding.startButton.performClick()
                true
            } else {
                false
            }
        }

        // 5) 이름 저장 및 화면 전환
        binding.startButton.setOnClickListener {
            val inputName = binding.nameEditText.text.toString().trim()
            if (inputName.isEmpty()) {
                binding.nameEditText.error = "이름을 입력해주세요!"
                return@setOnClickListener
            }

            // 5-1) 입력된 이름을 SharedPreferences에 저장
            userPrefs.edit()
                .putString(KEY_USER_NAME, inputName)
                .apply()

            // 5-2) MainScreen으로 이동하며 백스택 정리
            startActivity(Intent(this, MainScreen::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }
}
