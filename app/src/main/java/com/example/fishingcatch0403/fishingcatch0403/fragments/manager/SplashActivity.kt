package com.example.fishingcatch0403.fishingcatch0403.fragments.manager

import android.content.Intent
import android.os.Bundle
import android.os.HandlerThread
import androidx.appcompat.app.AppCompatActivity
import com.example.fishingcatch0403.R

class SplashActivity : AppCompatActivity() {
    private val SPLASH_DELAY = 2000L // 로고 표시 시간 (2초)
    private lateinit var handlerThread: HandlerThread
    private lateinit var handler: android.os.Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash) // 로고 레이아웃 설정

        handlerThread = HandlerThread("SplashThread")
        handlerThread.start()
        handler = android.os.Handler(handlerThread.looper)

        handler.postDelayed({
            startActivity(Intent(this, LoginActivity::class.java))
            finish() // SplashActivity 종료
        }, SPLASH_DELAY)
    }

    override fun onDestroy() {
        super.onDestroy()
        handlerThread.quit()
    }
}