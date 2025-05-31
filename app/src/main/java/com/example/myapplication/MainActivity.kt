package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.home.setOnClickListener(){
            val intent = Intent(this, home::class.java)
            startActivity(intent)
        }
        binding.counsel.setOnClickListener(){
            val intent = Intent(this,consuelAct::class.java)
            startActivity(intent)
        }
        binding.setting.setOnClickListener(){
            val intent = Intent(this,SettingAct::class.java)
            startActivity(intent)
        }
        binding.writeCompl.setOnClickListener(){

        }
        binding.goAnn.setOnClickListener(){
            val url = "https://share.siheung.go.kr/board/board_list.do?gidx=0001&pageIndex=1&key=105000"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
        binding.gotalk.setOnClickListener(){
            val url = "https://talk.siheung.go.kr"
            val sitalk = Intent(Intent.ACTION_VIEW,Uri.parse(url))
            startActivity(sitalk)

        }
    }

}