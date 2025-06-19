package com.example.safetyhelper

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.safetyhelper.databinding.ActivityFindAccountBinding
import com.google.firebase.auth.FirebaseAuth
import android.view.inputmethod.EditorInfo
import utils.ThemeHelper

class FindAccountActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyDarkMode(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityFindAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.findAccountEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                binding.findAccount.performClick()
                true
            } else {
                false
            }
        }

        binding.findAccount.setOnClickListener {
            val email = binding.findAccountEditText.text.toString()
            Log.d("EmailCheck", "입력된 이메일: $email")
            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val intent = Intent(this, FindAccountActivity2::class.java)
                            startActivity(intent)
                            finish()
                        } else {

                            Toast.makeText(this, "비밀번호 재설정에 실패했습니다: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "이메일을 입력하세요!", Toast.LENGTH_SHORT).show()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.findAccount) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}