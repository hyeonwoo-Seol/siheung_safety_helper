package com.example.safetyhelper

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.safetyhelper.databinding.ActivityVersionBinding

class VersionAct : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityVersionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnLicense.setOnClickListener(){
            val intent = Intent(this,SettingAct::class.java)
            startActivity(intent)
        }
    }
}