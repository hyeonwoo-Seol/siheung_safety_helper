package com.example.safetyhelper

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.safetyhelper.databinding.ActivitySettingBinding
import utils.ThemeHelper

class SettingAct : AppCompatActivity() {

    companion object {
        private const val PREFS_USER = "user_prefs"
        private const val KEY_USER_NAME = "KEY_USER_NAME"
    }

    private var isUserTogglingLock = false
    private var isLockChecked = false
    override fun onCreate(savedInstanceState: Bundle?) {
        val skipLock = intent.getBooleanExtra("SKIP_LOCK", false) ||
                savedInstanceState?.getBoolean("SKIP_LOCK") == true
        ThemeHelper.applyDarkMode(this)
        if (ThemeHelper.isDarkMode(this)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val userPrefs: SharedPreferences = getSharedPreferences(PREFS_USER, MODE_PRIVATE)
        val lockPrefs = getSharedPreferences("lock_pref", MODE_PRIVATE)
        val savedUserName: String? = userPrefs.getString(KEY_USER_NAME, "")
        val isLockEnabled = lockPrefs.getBoolean("lock_enabled", false)



        if (!skipLock && isLockEnabled) {
            startActivity(Intent(this, LockScreen::class.java).apply {
                putExtra("SKIP_LOCK", true) // 되돌아올 때 중복 방지
            })
            finish()
            return
        }

        binding.userName.text = savedUserName ?: ""
        binding.switchAppLock.isChecked = isLockEnabled

        // 다크모드 설정
        binding.darkMode.setOnClickListener {
            val nowDark = ThemeHelper.isDarkMode(this)
            ThemeHelper.setDarkMode(this, !nowDark)

            AppCompatDelegate.setDefaultNightMode(
                if (!nowDark) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )

            val restartIntent = Intent(this, SettingAct::class.java)
            restartIntent.putExtra("SKIP_LOCK", true)  // 잠금화면 생략
            restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(restartIntent)
            finish()

        }

        // 비밀번호 설정 화면
        binding.switchAppLock.setOnCheckedChangeListener { _, isChecked ->
            if (!isUserTogglingLock) return@setOnCheckedChangeListener
            lockPrefs.edit().putBoolean("lock_enabled", isChecked).apply()
            if (isChecked) {
                startActivity(Intent(this, SetLockPwScreen::class.java))
            }
        }

        // 사용자 토글 감지
        binding.switchAppLock.setOnTouchListener { _, _ ->
            isUserTogglingLock = true
            false
        }

        // 기타 화면 이동 시 lock 방지 intent 추가
        binding.version.setOnClickListener {
            startActivity(Intent(this, VersionAct::class.java).apply {
                putExtra("SKIP_LOCK", true)
            })
        }

        binding.alarm.setOnClickListener {
            startActivity(Intent(this, AlarmAct::class.java).apply {
                putExtra("SKIP_LOCK", true)
            })
        }

        binding.changePw.setOnClickListener {
            startActivity(Intent(this, FindAccountActivity::class.java).apply {
                putExtra("SKIP_LOCK", true)
            })
        }

        binding.logout.setOnClickListener {
            userPrefs.edit().clear().apply()
            val logoutIntent = Intent(this, SignInActivity::class.java)
            logoutIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(logoutIntent)
            finish()
        }
        binding.consuel.setOnClickListener {
            val intent = Intent(this, ContactAct::class.java)
            startActivity(intent)
        }
    }
    override fun onResume() {
        super.onResume()

        val lockPrefs = getSharedPreferences("lock_pref", MODE_PRIVATE)
        val isLockEnabled = lockPrefs.getBoolean("lock_enabled", false)
        val isPasswordSet = lockPrefs.getString("app_password", null) != null

        val skipLock = intent.getBooleanExtra("SKIP_LOCK", false)

        if (isLockEnabled && isPasswordSet && !isLockChecked && !skipLock) {
            isLockChecked = true
            val intent = Intent(this, LockScreen::class.java)
            intent.putExtra("SKIP_LOCK", true)
            startActivity(intent)
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("SKIP_LOCK", true)
    }
}
