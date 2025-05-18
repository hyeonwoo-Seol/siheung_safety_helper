package com.example.safetyhelper

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PreAiResponseOneActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pre_ai_response_one)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tv = findViewById<TextView>(R.id.content1)

        val fullText = "생성형 AI는 아직 완벽하지 않습니다\n\n생성된 민원이 부정확하거나 필요한 정보가 누락될 수 있습니다\n생성된 민원 글에 대해 검토를 부탁드립니다\n\n생성된 민원 글은 직접 수정하실 수 있습니다"
        val spannable = SpannableStringBuilder(fullText)
        spannable.setSpan(
            AbsoluteSizeSpan(24, true),
            0,
            20,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            AbsoluteSizeSpan(16, true),
            20,
            fullText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tv.text = spannable

        val btnNext = findViewById<MaterialButton>(R.id.btnNext)
        btnNext.setOnClickListener {
            startActivity(Intent(this, AiResponseActivity::class.java))
        }

        val tvTyping = findViewById<TextView>(R.id.content2)
        val textToShow = "키보드 왼쪽 하단에 있는 마이크 모양을 터치하시면 말로 글자를 입력하실 수 있습니다"

        lifecycleScope.launch {
            tvTyping.text = ""
            textToShow.forEach { char ->
                tvTyping.append(char.toString())
                delay(40)
            }
        }
    }
}