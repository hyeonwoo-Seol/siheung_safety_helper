package com.example.safetyhelper

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainScreen : AppCompatActivity() {

    private lateinit var announcementTv: TextView
    private val PREFS_NAME    = "notices_prefs"
    private val KEY_ANNOUNCE  = "latest_notice"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_screen)
        announcementTv = findViewById(R.id.announcement)

        // 1) 샘플 공지 생성 & 저장
        saveSampleNotice()

        // 2) 저장된 공지 불러와서 화면에 반영
        loadNoticeIntoView()

        // (임시) 버튼 뷰 참조 ----------------------------------------------------------------------
        val btnAiResponse = findViewById<Button>(R.id.btnWrite)
        btnAiResponse.setOnClickListener{
            val intent = Intent(this, AiResponseActivity::class.java)
            startActivity(intent)
        }
    }
    private fun saveSampleNotice() {
        // 임의로 만든 공지 예시
        val sample = "🎉 앱 버전 1.1 출시! 새로운 기능을 확인해 보세요.\n(발행일: ${todayDate()})"
        val prefs  = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_ANNOUNCE, sample)
            .apply()
    }

    private fun loadNoticeIntoView() {
        val prefs     = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val noticeStr = prefs.getString(KEY_ANNOUNCE, "현재 공지사항이 없습니다.")
        announcementTv.text = noticeStr
    }

    private fun todayDate(): String {
        val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        return sdf.format(Date())
    }
}