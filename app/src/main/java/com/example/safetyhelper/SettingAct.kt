package com.example.safetyhelper

import android.content.Intent
import android.content.SharedPreferences
import com.example.safetyhelper.FindAccountActivity
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.safetyhelper.databinding.ActivitySettingBinding
import utils.ThemeHelper

class SettingAct : AppCompatActivity() {

    companion object {
        private const val PREFS_USER = "user_prefs"
        private const val KEY_USER_NAME = "KEY_USER_NAME"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
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
        val savedUserName: String? = userPrefs.getString(KEY_USER_NAME, "")
        // NULL이거나 빈 문자열인 경우에도 안전하게 처리
        binding.userName.text = savedUserName ?: ""

        binding.darkMode.setOnClickListener(){
            val nowDark = ThemeHelper.isDarkMode(this)
            ThemeHelper.setDarkMode(this, !nowDark)

            AppCompatDelegate.setDefaultNightMode(
                if (!nowDark) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )

            // 현재 화면만 즉시 재적용
            recreate()
        }
        binding.changePw.setOnClickListener(){
            val intent = Intent(this, FindAccountActivity::class.java)
            startActivity(intent)
        }
        binding.alarm.setOnClickListener(){
            val intent = Intent(this,AlarmAct::class.java)
            startActivity(intent)

        }

    }

}