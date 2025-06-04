package com.example.safetyhelper

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.databinding.ActivitySettingBinding
import utils.ThemeHelper

class SettingAct : AppCompatActivity() {
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
            val intent = Intent(Intent.ACTION_VIEW,)
        }
        binding.alarm.setOnClickListener(){
            val intent = Intent(this,AlarmAct::class.java)
            startActivity(intent)

        }

    }

}