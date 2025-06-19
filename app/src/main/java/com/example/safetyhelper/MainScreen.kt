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
import android.widget.FrameLayout
import androidx.appcompat.widget.SwitchCompat
import utils.ThemeHelper

class MainScreen : AppCompatActivity() {

    private lateinit var announcementTv: TextView
    private lateinit var switchBigText: SwitchCompat
    private var isFirstLaunch = true
    private var isLockChecked = false

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_ANNOUNCE = "latest_notice"
        private const val KEY_BIG_TEXT_MODE = "big_text_mode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyDarkMode(this)
        super.onCreate(savedInstanceState)

        isFirstLaunch = savedInstanceState?.getBoolean("IS_FIRST_LAUNCH", true) ?: true
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
        switchBigText = findViewById(R.id.switchBigText)

        switchBigText.isChecked = isBig

        // 큰 글씨 모드 토글
        switchBigText.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit()
                .putBoolean(KEY_BIG_TEXT_MODE, isChecked)
                .apply()
            recreate()
        }
        // 공지 저장 및 로드
        saveSampleNotice(prefs)
        loadNoticeIntoView(prefs)

        // 민원 작성 버튼
        findViewById<FrameLayout>(R.id.move_ai_response).setOnClickListener {
            startActivity(Intent(this, PreAiResponseOneActivity::class.java))
        }

        // 작성된 민원 목록 버튼
        findViewById<FrameLayout>(R.id.move_response_detail).setOnClickListener {
            startActivity(Intent(this, ComplaintListActivity::class.java))
        }

        findViewById<FrameLayout>(R.id.move_site1).setOnClickListener {
            val url1 = "https://www.siheung.go.kr/main/bbs/list.do?ptIdx=46&mId=0401010000"
            val intent1 = Intent(Intent.ACTION_VIEW, Uri.parse(url1))
            startActivity(intent1)
        }

        // 4) 시흥톡 바로가기
        findViewById<FrameLayout>(R.id.move_site2).setOnClickListener {
            val url2 = "https://talk.siheung.go.kr/"
            val intent2 = Intent(Intent.ACTION_VIEW, Uri.parse(url2))
            startActivity(intent2)
        }

    }

    //잠금화면 버튼 on일시 메인 들어오기 전에 잠금 화면으로 진입 off일시 그냥 진입
    override fun onResume() {
        super.onResume()

        val lockPrefs = getSharedPreferences("lock_pref", MODE_PRIVATE)
        val isLockEnabled = lockPrefs.getBoolean("lock_enabled", false)
        val isPasswordSet = lockPrefs.getString("app_password", null) != null
        val skipLock = intent.getBooleanExtra("SKIP_LOCK", false)

        // 최초 진입 + 잠금 설정이 되어 있을 때만 락화면 실행
        if (isFirstLaunch && isLockEnabled && isPasswordSet && !isLockChecked && !skipLock) {
            isLockChecked = true
            isFirstLaunch = false
            val intent = Intent(this, LockScreen::class.java)
            intent.putExtra("FROM_MAIN", true)

            startActivity(intent)
        } else {
            isFirstLaunch = false
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
            R.id.action_settings -> {
                // SettingAct 로 이동하는 인텐트 생성
                val intent = Intent(this, SettingAct::class.java)
                intent.putExtra("SKIP_LOCK", true)
                startActivity(intent)
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
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("IS_FIRST_LAUNCH", isFirstLaunch)
    }
}
