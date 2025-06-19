package com.example.safetyhelper

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.safetyhelper.databinding.ActivitySetLockPwScreenBinding
import utils.ThemeHelper

class SetLockPwScreen : AppCompatActivity() {

    private lateinit var binding: ActivitySetLockPwScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyDarkMode(this)
        super.onCreate(savedInstanceState)
        binding = ActivitySetLockPwScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etPassword1.apply {
            inputType = android.text.InputType.TYPE_NULL
            isCursorVisible = false
            isFocusable = true
            isFocusableInTouchMode = true
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                showSoftInputOnFocus = false
            }
        }

        binding.etPassword2.apply {
            inputType = android.text.InputType.TYPE_NULL
            isCursorVisible = false
            isFocusable = true
            isFocusableInTouchMode = true
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                showSoftInputOnFocus = false
            }
        }

        val prefs = getSharedPreferences("lock_pref", Context.MODE_PRIVATE)

        var password1 = ""
        var password2 = ""

        // 버튼 입력 처리
        val passwordBuilder1 = StringBuilder()
        val passwordBuilder2 = StringBuilder()

        val numberButtons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9
        )

        numberButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                if (binding.etPassword1.hasFocus()) {
                    if (passwordBuilder1.length < 4) {
                        passwordBuilder1.append(index)
                        binding.etPassword1.setText(passwordBuilder1.toString())
                    }
                } else if (binding.etPassword2.hasFocus()) {
                    if (passwordBuilder2.length < 4) {
                        passwordBuilder2.append(index)
                        binding.etPassword2.setText(passwordBuilder2.toString())
                    }
                }
            }
        }

        // 삭제 버튼
        binding.btnDelete.setOnClickListener {
            if (binding.etPassword1.hasFocus() && passwordBuilder1.isNotEmpty()) {
                passwordBuilder1.deleteAt(passwordBuilder1.length - 1)
                binding.etPassword1.setText(passwordBuilder1.toString())
            } else if (binding.etPassword2.hasFocus() && passwordBuilder2.isNotEmpty()) {
                passwordBuilder2.deleteAt(passwordBuilder2.length - 1)
                binding.etPassword2.setText(passwordBuilder2.toString())
            }
        }

        // 저장 버튼
        binding.btnEnter.setOnClickListener {
            password1 = binding.etPassword1.text.toString()
            password2 = binding.etPassword2.text.toString()

            if (password1.length == 4 && password1 == password2) {
                prefs.edit().putString("app_password", password1).apply()
                Toast.makeText(this, "비밀번호가 저장되었습니다", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "비밀번호가 일치하지 않거나 4자리가 아닙니다", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
