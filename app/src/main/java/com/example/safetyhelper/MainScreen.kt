package com.example.safetyhelper

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.net.Uri

class MainScreen : AppCompatActivity() {

    private lateinit var announcementTv: TextView
    private lateinit var btnBigString: Button

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_ANNOUNCE = "latest_notice"
        private const val KEY_BIG_TEXT_MODE = "big_text_mode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // SharedPreferences 초기화 및 큰 글씨 모드 여부 확인
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isBig = prefs.getBoolean(KEY_BIG_TEXT_MODE, false)

        // 레이아웃 선택
        if (isBig) {
            setContentView(R.layout.activity_main_screen_big)
        } else {
            setContentView(R.layout.activity_main_screen)
        }

        // ① 툴바를 액션바로 설정
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            // 필요시 뒤로가기 버튼 설정
            setDisplayHomeAsUpEnabled(false)
            // 툴바 타이틀은 XML의 app:title 속성으로 이미 지정되어 있습니다
        }

        // 뷰 바인딩
        announcementTv = findViewById(R.id.announcement)
        btnBigString = findViewById(R.id.btnBigString)

        // 큰 글씨 모드 토글
        btnBigString.setOnClickListener {
            prefs.edit()
                .putBoolean(KEY_BIG_TEXT_MODE, !isBig)
                .apply()
            recreate()
        }

        // 공지 저장 및 로드
        saveSampleNotice(prefs)
        loadNoticeIntoView(prefs)

        // 민원 작성 버튼
        findViewById<Button>(R.id.move_ai_response).setOnClickListener {
            startActivity(Intent(this, AiResponseActivity::class.java))
        }

        // 작성된 민원 목록 버튼
        findViewById<Button>(R.id.move_response_detail).setOnClickListener {
            startActivity(Intent(this, ComplaintListActivity::class.java))
        }

        findViewById<Button>(R.id.move_site1).setOnClickListener {
            val url1 = "https://www.siheung.go.kr/main/bbs/list.do?ptIdx=46&mId=0401010000"
            val intent1 = Intent(Intent.ACTION_VIEW, Uri.parse(url1))
            startActivity(intent1)
        }

        // 4) 시흥톡 바로가기
        findViewById<Button>(R.id.move_site2).setOnClickListener {
            val url2 = "https://talk.siheung.go.kr/"
            val intent2 = Intent(Intent.ACTION_VIEW, Uri.parse(url2))
            startActivity(intent2)
        }
    }

    // ② 툴바 메뉴 리소스 연결
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main_screen, menu)
        return true
    }
    // ③ 메뉴 아이템 클릭 처리
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.mainscreen_menu -> {
                //startActivity(Intent(this, OptionActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveSampleNotice(prefs: SharedPreferences) {
        val sample = "🎉 앱 버전 1.1 출시! 새로운 기능을 확인해 보세요.\n(발행일: ${todayDate()})"
        prefs.edit()
            .putString(KEY_ANNOUNCE, sample)
            .apply()
    }

    private fun loadNoticeIntoView(prefs: SharedPreferences) {
        val noticeStr = prefs.getString(KEY_ANNOUNCE, "현재 공지사항이 없습니다.")
        announcementTv.text = noticeStr
    }

    private fun todayDate(): String {
        val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
