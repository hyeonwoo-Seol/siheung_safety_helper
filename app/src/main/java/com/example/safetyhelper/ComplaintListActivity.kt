package com.example.safetyhelper

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ComplaintListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complaint_list)

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

        // 3) 동적으로 버튼 생성해서 화면에 붙이기
        val container = findViewById<LinearLayout>(R.id.llContainer)
        container.removeAllViews()
        complaints.forEach { c ->
            val btn = Button(this).apply {
                text = c.title
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

