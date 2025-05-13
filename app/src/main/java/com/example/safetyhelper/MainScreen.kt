package com.example.safetyhelper

import android.content.Intent
import android.content.SharedPreferences
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
    private lateinit var btnBigString: Button

    companion object {
        private const val PREFS_NAME   = "notices_prefs"
        private const val KEY_ANNOUNCE = "latest_notice"
        private const val KEY_BIG_TEXT_MODE = "big_text_mode"  // ★추가
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        // ★ SharedPreferences 초기화 및 큰 글씨 모드 여부 확인
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isBig = prefs.getBoolean(KEY_BIG_TEXT_MODE, false)

         // ★ 큰 글씨 모드에 따라 사용할 레이아웃 선택
        if (isBig) {
                setContentView(R.layout.activity_main_screen_big)   // ★변경
            } else {
                setContentView(R.layout.activity_main_screen)
            }
        announcementTv = findViewById(R.id.announcement)
        btnBigString   = findViewById(R.id.btnBigString)

        // ★ 큰 글씨 모드 토글 버튼 리스너
        btnBigString.setOnClickListener {
                prefs.edit()
                    .putBoolean(KEY_BIG_TEXT_MODE, !isBig)
                    .apply()
                recreate()
        }


        // 1) 샘플 공지 생성 & 저장
        saveSampleNotice(prefs)

        // 2) 저장된 공지 불러와서 화면에 반영
        loadNoticeIntoView(prefs)

        // (임시) 버튼 뷰 참조 ----------------------------------------------------------------------
        val btnAiResponse = findViewById<Button>(R.id.btnWrite)
        btnAiResponse.setOnClickListener{
            val intent = Intent(this, AiResponseActivity::class.java)
            startActivity(intent)
        }
    }
    private fun saveSampleNotice(prefs: SharedPreferences) {
        // 임의로 만든 공지 예시
        val sample = "🎉 앱 버전 1.1 출시! 새로운 기능을 확인해 보세요.\n(발행일: ${todayDate()})"
        val prefs  = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_ANNOUNCE, sample)
            .apply()
    }

    private fun loadNoticeIntoView(prefs: SharedPreferences) {
        val noticeStr = prefs.getString(KEY_ANNOUNCE, "현재 공지사항이 없습니다.")
        announcementTv.text = noticeStr
    }

    private fun todayDate(): String {
        val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        return sdf.format(Date())
    }
}