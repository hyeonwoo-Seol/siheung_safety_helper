package com.example.safetyhelper

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ComplaintListActivity : AppCompatActivity() {
    companion object {
        private const val PREFS_NAME        = "app_settings"                // ★추가
        private const val KEY_BIG_TEXT_MODE = "big_text_mode"               // ★추가
    }

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1) SharedPreferences 준비 및 큰 글씨 모드 여부 확인
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)              // ★추가
        val isBig = prefs.getBoolean(KEY_BIG_TEXT_MODE, false)              // ★추가

        // 2) isBig 에 따라 알맞은 레이아웃 선택
        if (isBig) {
            setContentView(R.layout.activity_complaint_list_big)            // ★변경 (big 전용 레이아웃)
        } else {
            setContentView(R.layout.activity_complaint_list)
        }



        // 3) 툴바 셋업 & 클릭 리스너로 모드 토글
        val toolbar = findViewById<Toolbar>(R.id.toolbar_list)             // ★추가
        setSupportActionBar(toolbar)                                       // ★추가
        toolbar.setOnClickListener {                                       // ★추가
            prefs.edit().putBoolean(KEY_BIG_TEXT_MODE, !isBig).apply()
            recreate()
        }




        // 1) 내부 저장소에 있는 response_*.txt 파일 읽기
        val files = filesDir
            .listFiles()
            ?.filter { it.name.startsWith("response_") && it.name.endsWith(".txt") }
            ?: emptyList()

        // 2) 파일마다 Complaint 객체로 변환
        val complaints = files.mapIndexed { idx, file ->
            // ① 파일명에서 timestamp(밀리초) 추출
            val ts = file.name
                .removePrefix("response_")
                .removeSuffix(".txt")
                .toLongOrNull()
                ?: System.currentTimeMillis()

            // ② 작성 일시를 "yyyy.MM.dd HH:mm" 형식으로 포맷
            val title = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
                .format(Date(ts))

            // ③ 파일 전체 내용 읽기
            val content = file.readText().trim()

            Complaint(id = idx, title = title, content = content)
        }

        val normalTextSp = 14f
        val bigTextSp = normalTextSp * 2

        // 3) 동적으로 버튼 생성해서 화면에 붙이기
        val container = findViewById<LinearLayout>(R.id.llContainer)
        container.removeAllViews()
        complaints.forEach { c ->
            val btn = Button(this).apply {
                text = c.title
                textSize = if (isBig) bigTextSp else normalTextSp
                setBackgroundResource(R.drawable.ripple_content_round)
                backgroundTintList    = null
                elevation             = 0f
                stateListAnimator     = null
                setTextColor(ContextCompat.getColor(context, R.color.title_text_color))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = dpToPx(8) }
                setOnClickListener {
                    // 상세 보기: ComplaintDetailActivity 로 content 전달
                    val intent = Intent(this@ComplaintListActivity, ComplaintDetailActivity::class.java)
                        .putExtra("complaint_title", c.title)
                        .putExtra("complaint_content", c.content)
                    startActivity(intent)
                }
            }
            container.addView(btn)
        }
    }

    // dp→px 변환 헬퍼
    private fun Context.dpToPx(dp: Int) =
        (dp * resources.displayMetrics.density).toInt()
}

