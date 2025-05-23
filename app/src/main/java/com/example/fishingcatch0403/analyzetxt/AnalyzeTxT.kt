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
import com.example.fishingcatch0403.fishingcatch0403.fragments.phishing_log.NotificationDao
import com.example.fishingcatch0403.fishingcatch0403.fragments.phishing_log.NotificationDatabase
import com.example.fishingcatch0403.fishingcatch0403.fragments.phishing_log.NotificationItem
import com.example.fishingcatch0403.stt.CHANNEL_ID
import com.example.fishingcatch0403.stt.notificationId
import com.example.fishingcatch0403.stt.notificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

lateinit var analyzeController: AnalyzeController
private var sttText: String? = null
private var phoneNumber: String? = null
private var isNotificationShown = false // 알림의 중복 표시를 제한하기 위한 플래그

class AnalyzeTxT : Service() {

    private lateinit var notificationDao: NotificationDao

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // 텍스트가 전달되었을 경우
        intent?.let {
            sttText = it.getStringExtra("text")
            phoneNumber = it.getStringExtra("phoneNumber")
            Log.d("[APP] AnalyzeTxT", "AnalyzeTxT 시작: $sttText")
            sttText?.let { text ->
                startAnalyze(text, phoneNumber)
            } ?: Log.e("[APP] AnalyzeTxT", "STT 텍스트가 null입니다.")
        }
        return START_NOT_STICKY // 서비스는 강제로 종료된 후 자동으로 재시작되지 않음
    }

    // 텍스트 분석 시작 함수
    private fun startAnalyze(text: String, phoneNumber: String?) {
        analyzeController.analyzeText(this, text, object : AnalyzeController.AnalysisCallback {
            override fun onAnalysisComplete(result: String) {
                // 알림 표시
                phoneNumber?.let {
                    showResultNotification(result, it)
                } ?: Log.e("[APP] AnalyzeTxT", "전화번호가 null입니다.")
            }
        })
    }

    override fun onBind(intent: Intent): IBinder? {
        return null // 바인딩 하지 않기 때문에 null 반환
    }

    override fun onCreate() {
        super.onCreate()
        notificationDao = NotificationDatabase.getDatabase(applicationContext).notificationDao()
        analyzeController = AnalyzeController()
        createNotificationChannel() // 알림 채널 생성
    }

    @SuppressLint("RemoteViewLayout")
    private fun showResultNotification(result: String, phoneNumber: String) {
        // 이미 알림이 표시된 경우
        if (isNotificationShown) {
            return
        }
        isNotificationShown = true

        // 현재 시간 및 날짜 형식 설정
        val currentDateTime =
            SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date())

        // 커스텀 레이아웃 설정
        val remoteViews = RemoteViews(packageName, R.layout.notification_layout).apply {
            // 결과에 따라 알림 색상 및 아이콘 설정
            setTextViewText(R.id.title, "보이스 피싱 분석 결과") // 제목은 항상 동일
            setTextViewText(R.id.message, "$currentDateTime\n$phoneNumber\n$result") // 메시지 설정

            when (analyzeController.resultCheck) {
                1 -> {
                    setInt(R.id.notification_layout, "setBackgroundColor", Color.RED)
                    setTextColor(R.id.message, Color.BLACK)
                    setImageViewResource(R.id.icon, R.drawable.alert)
                }

                0 -> {
                    setInt(R.id.notification_layout, "setBackgroundColor", Color.GREEN)
                    setTextColor(R.id.message, Color.WHITE)
                    setImageViewResource(R.id.icon, R.drawable.shield)
                }

                else -> {
                    setInt(R.id.notification_layout, "setBackgroundColor", Color.YELLOW)
                    setTextColor(R.id.message, Color.WHITE)
                    setImageViewResource(R.id.icon, R.drawable.logo)
                }
            }
        }

        // 알림 빌더 설정
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo) // 기본 아이콘
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContent(remoteViews)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true) // 알림 갱신 시 소리 알림 방지

        val notification = notificationBuilder.build()
        notificationManager.notify(notificationId + 1, notification)

        // 알림 저장 (의심되거나 안전할 경우만)
        if (analyzeController.resultCheck == 1 || analyzeController.resultCheck == 0) {
            val resultType = if (analyzeController.resultCheck == 1) 1 else 2
            val notificationItem = NotificationItem(
                dateTime = currentDateTime,
                phoneNumber = phoneNumber,
                result = result,
                resultType = resultType
            )
            CoroutineScope(Dispatchers.IO).launch {
                notificationDao.insertNotification(notificationItem)
            }
        }
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
