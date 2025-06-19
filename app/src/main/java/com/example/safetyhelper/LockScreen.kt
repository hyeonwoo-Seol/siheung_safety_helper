package com.example.safetyhelper

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.safetyhelper.databinding.ActivityLockScreenBinding
import utils.ThemeHelper


class LockScreen : AppCompatActivity() {

    private lateinit var binding: ActivityLockScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyDarkMode(this)
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra("SKIP_LOCK", false)) {
            finish()
            return
        }

        val fromMain = intent.getBooleanExtra("FROM_MAIN", false)
        if (!fromMain) {
            finish()
            return
        }

        binding = ActivityLockScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.etPassword.apply {
            inputType = android.text.InputType.TYPE_NULL
            isCursorVisible = false
            isFocusable = true
            isFocusableInTouchMode = true

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                showSoftInputOnFocus = false
            }
        }
        val prefs = getSharedPreferences("lock_pref", Context.MODE_PRIVATE)
        val savedPassword = prefs.getString("app_password", null)

        if (savedPassword == null) {
            Toast.makeText(this, "비밀번호가 설정되어 있지 않습니다", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val inputBuilder = StringBuilder()

        val numberButtons = mapOf(
            0 to binding.btn0,
            1 to binding.btn1,
            2 to binding.btn2,
            3 to binding.btn3,
            4 to binding.btn4,
            5 to binding.btn5,
            6 to binding.btn6,
            7 to binding.btn7,
            8 to binding.btn8,
            9 to binding.btn9,
        )

        numberButtons.forEach { (number, button) ->
            button.setOnClickListener {
                if (inputBuilder.length < 4) {
                    inputBuilder.append(number)
                    binding.etPassword.setText(inputBuilder.toString())
                }
            }
        }

        binding.btnDelete.setOnClickListener {
            if (inputBuilder.isNotEmpty()) {
                inputBuilder.deleteAt(inputBuilder.length - 1)
                binding.etPassword.setText(inputBuilder.toString())
            }
        }

        binding.btnEnter.setOnClickListener {
            val input = inputBuilder.toString()
            if (input == savedPassword) {
                Toast.makeText(this, "잠금 해제 성공", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show()
                inputBuilder.clear()
                binding.etPassword.text.clear()
            }
        }
        binding.btnVerifyToReset.setOnClickListener {
            val prefs = getSharedPreferences("lock_pref", MODE_PRIVATE)
            val storedPin = prefs.getString("app_password", null)
            val inputPin = binding.etPassword.text.toString()

            if (inputPin == storedPin) {
                Toast.makeText(this, "비밀번호가 확인되었습니다. 재설정 화면으로 이동합니다.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, SetLockPwScreen::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
