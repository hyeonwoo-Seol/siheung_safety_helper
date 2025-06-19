package com.example.safetyhelper

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.safetyhelper.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import utils.ThemeHelper

class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyDarkMode(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.signUp) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.signUpButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val confirmPassword = binding.passwordCheckEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "모든 입력란을 채워주세요!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            signUp(email, password)
        }
        binding.passwordCheckEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                binding.signUpButton.performClick()
                true
            } else {
                false
            }
        }
    }
    private fun signUp(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "회원가입 성공!", Toast.LENGTH_SHORT).show()

                    // 회원가입 성공 후 로그인 페이지로 이동 가능
                    val intent = Intent(this, SignInActivity::class.java)
                    startActivity(intent)
                    finish()

                } else {
                    val errorMessage = task.exception?.message
                    Toast.makeText(this, "회원가입 실패: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
    }
}