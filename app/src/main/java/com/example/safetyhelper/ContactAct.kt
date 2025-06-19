package com.example.safetyhelper

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.safetyhelper.databinding.ActivityContactBinding
import utils.ThemeHelper

class ContactAct : AppCompatActivity() {
    private lateinit var binding: ActivityContactBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyDarkMode(this)
        super.onCreate(savedInstanceState)
        binding = ActivityContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.sendBtn.setOnClickListener {
            val message = binding.inputMessage.text.toString()
            if (message.isNotBlank()) {
                // 실제로는 서버로 전송 or 이메일로 전송 처리
                Toast.makeText(this, "문의가 전송되었습니다", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "내용을 입력해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }
}