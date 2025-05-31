package com.example.myapplication

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
import androidx.core.app.NotificationCompat
import com.example.myapplication.databinding.ActivityAlarmBinding
import utils.NotificationHelper

class AlarmAct : AppCompatActivity() {

    private val CHANNEL_SOUND = "channel_sound"
    private val CHANNEL_SILENT = "channel_silent"
    private val NOTI_ID = 1
    private lateinit var binding: ActivityAlarmBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)


        requestNotificationPermission()


        createNotificationChannel(soundOff = false)


        binding.home.setOnClickListener {
            val intent = Intent(this, SettingAct::class.java)
            startActivity(intent)
        }

        // ✅ 스위치 눌렀을 때 알림 테스트 전송
        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            val prefs = getSharedPreferences("setting", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("sound_enabled", isChecked).apply()

            // ✅ 채널 재생성 (설정 반영)
            createNotificationChannel(soundOff = isChecked)

        }

        // ✅ [알림 보내기] 버튼 눌렀을 때
        binding.btnNotify.setOnClickListener {
            NotificationHelper.send(this, "테스트 알림", "소리 여부 설정에 따라 전송됨")
            // SharedPreferences에 진동 설정 저장
            val isChecked = binding.switchVibration.isChecked
            getSharedPreferences("setting", Context.MODE_PRIVATE)
                .edit().putBoolean("vibration_enabled", isChecked).apply()

            // 진동 울리기 (테스트용)
            if (isChecked) {
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
        binding.switchVibration.setOnClickListener(){
            NotificationHelper.send(this, "테스트 알림", "설정에 따라 작동합니다")
        }
    }

    // ✅ 권한 요청 함수 (Android 13 이상만 해당)
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

    // ✅ 알림 채널 생성 함수 (soundOff: true면 무음 설정)
    private fun createNotificationChannel(soundOff: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 채널 1: 소리 있음
            val soundChannel = NotificationChannel(
                CHANNEL_SOUND,
                "소리 알림",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            // 채널 2: 무음
            val silentChannel = NotificationChannel(
                CHANNEL_SILENT,
                "무음 알림",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setSound(null, null)
                enableVibration(false)
            }

            manager.createNotificationChannel(soundChannel)
            manager.createNotificationChannel(silentChannel)
        }
    }
}
