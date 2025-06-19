package com.example.safetyhelper

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.safetyhelper.databinding.ActivityAlarmBinding
import utils.NotificationHelper
import utils.ThemeHelper

class AlarmAct : AppCompatActivity() {

    private val CHANNEL_SOUND = "channel_sound"
    private val CHANNEL_SILENT = "channel_silent"
    private lateinit var binding: ActivityAlarmBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyDarkMode(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestNotificationPermission()
        createNotificationChannel(soundOff = false)

        val prefs = getSharedPreferences("setting", Context.MODE_PRIVATE)

        //상태 복원 췤
        binding.switchSound.isChecked = prefs.getBoolean("sound_enabled", false)
        binding.switchVibration.isChecked = prefs.getBoolean("vibration_enabled", false)

        //사운드 설정
        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("sound_enabled", isChecked).apply()
            createNotificationChannel(soundOff = !isChecked) // true일 때 sound 켜야 하므로 반전
        }


        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("vibration_enabled", isChecked).apply()
        }

        //알림 테스트 버튼
        binding.testNotificationBtn.setOnClickListener {
            val isVibration = prefs.getBoolean("vibration_enabled", false)
            val isSound = prefs.getBoolean("sound_enabled", false)
            val channelId = if (isSound) CHANNEL_SOUND else CHANNEL_SILENT

            NotificationHelper.send(this, "테스트 알림", "설정에 따라 작동합니다", channelId)


            if (isVibration) {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }
            }
        }
        binding.switchPreview.isChecked = prefs.getBoolean("preview_enabled", true)

        binding.switchPreview.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("preview_enabled", isChecked).apply()
        }

    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }

    private fun createNotificationChannel(soundOff: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val soundChannel = NotificationChannel(
                CHANNEL_SOUND,
                "소리 알림",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "소리를 포함한 알림 채널입니다."
            }

            val silentChannel = NotificationChannel(
                CHANNEL_SILENT,
                "무음 알림",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
                enableVibration(false)
                description = "소리와 진동이 없는 무음 알림 채널입니다."
            }

            manager.createNotificationChannel(soundChannel)
            manager.createNotificationChannel(silentChannel)
        }
    }
}