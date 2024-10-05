package com.example.fishingcatch0403.analyzetxt

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.fishingcatch0403.R
import com.example.fishingcatch0403.stt.CHANNEL_ID
import com.example.fishingcatch0403.stt.notificationId
import com.example.fishingcatch0403.stt.notificationManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

lateinit var analyzeController: AnalyzeController
private var sttText: String? = null
private var phoneNumber: String? = null

class AnalyzeTxT : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // 텍스트가 전달되었을 경우
        intent?.let {
            sttText = it.getStringExtra("text")
            phoneNumber = it.getStringExtra("phoneNumber")
            Log.d("[APP] AnalyzeTxT", "AnalyzeTxT 시작: $sttText")
            startAnalyze(sttText!!)
        }
        return START_NOT_STICKY // 서비스는 강제로 종료된 후 자동으로 재시작되지 않음
    }

    // 텍스트 분석 시작 함수
    private fun startAnalyze(text: String) {
        analyzeController.analyzeText(text, object : AnalyzeController.AnalysisCallback {
            override fun onAnalysisComplete(result: String) {
                // 알림 표시
                showResultNotification(result, phoneNumber!!)
            }
        })
    }

    override fun onBind(intent: Intent): IBinder? {
        return null // 바인딩 하지 않기 때문에 null 반환
    }

    override fun onCreate() {
        super.onCreate()
        analyzeController = AnalyzeController()
        createNotificationChannel() // 알림 채널 생성
    }

    @SuppressLint("RemoteViewLayout")
    private fun showResultNotification(result: String, phoneNumber: String) {
        // 현재 시간 및 날짜 형식 설정
        val currentDateTime =
            SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date())

        // 커스텀 레이아웃 설정
        val remoteViews = RemoteViews(packageName, R.layout.notification_layout).apply {
            setTextViewText(R.id.title, "보이스 피싱 분석 결과")
            setTextViewText(R.id.message, "$currentDateTime\n$phoneNumber\n$result")

            // 알림 내용에 따라 배경 색상 및 아이콘 설정
            when {
                result.contains("의심됩니다") -> {
                    setInt(R.id.notification_layout, "setBackgroundColor", Color.RED) // 빨간색 배경
                    setTextColor(R.id.message, Color.BLACK) // 검은색 글자
                    setImageViewResource(R.id.icon, R.drawable.alert) // 경고 아이콘
                }

                result.contains("아닙니다") -> {
                    setInt(R.id.notification_layout, "setBackgroundColor", Color.GREEN) // 녹색 배경
                    setTextColor(R.id.message, Color.WHITE) // 흰색 글자
                    setImageViewResource(R.id.icon, R.drawable.shield) // 안전 아이콘
                }

                else -> {
                    setInt(R.id.notification_layout, "setBackgroundColor", Color.YELLOW) // 노란색 배경
                    setTextColor(R.id.message, Color.WHITE) // 흰색 글자
                    setImageViewResource(R.id.icon, R.drawable.logo) // 기본 아이콘
                }
            }
        }

        // 알림 빌더 설정
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo) // 기본 아이콘
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContent(remoteViews)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notification = notificationBuilder.build()
        notificationManager.notify(notificationId + 1, notification)
    }

    // 알림 채널 생성
    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "STT Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }
}
