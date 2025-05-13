package com.example.safetyhelper

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.safetyhelper.repository.ComplaintRepository

class ComplaintDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complaint_detail)

        val title   = intent.getStringExtra("complaint_title")   ?: "제목 없음"
        val content = intent.getStringExtra("complaint_content") ?: "내용 없음"

        findViewById<TextView>(R.id.tvTitle).text   = title
        findViewById<TextView>(R.id.tvContent).text = content
    }
}

