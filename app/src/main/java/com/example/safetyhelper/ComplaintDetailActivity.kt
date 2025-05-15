package com.example.safetyhelper

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity

class ComplaintDetailActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_BIG_TEXT_MODE = "big_text_mode"
    }
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // SharedPreferences 초기화
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isBig = prefs.getBoolean(KEY_BIG_TEXT_MODE, false)

        // big 모드에 따라 알맞은 레이아웃 선택
        if (isBig) {
            setContentView(R.layout.activity_complaint_detail_big)
        } else {
            setContentView(R.layout.activity_complaint_detail)
        }

        window.statusBarColor = Color.WHITE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        // 툴바 설정 & 클릭 시 모드 토글
        val toolbar = findViewById<Toolbar>(R.id.toolbar_list)
        setSupportActionBar(toolbar)
        toolbar.setOnClickListener {
            prefs.edit()
                .putBoolean(KEY_BIG_TEXT_MODE, !isBig)
                .apply()
            recreate()
        }

        val title   = intent.getStringExtra("complaint_title")   ?: "제목 없음"
        val content = intent.getStringExtra("complaint_content") ?: "내용 없음"

        findViewById<TextView>(R.id.tvTitle).text   = title
        findViewById<TextView>(R.id.tvContent).text = content
    }
}

