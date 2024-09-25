package com.example.fishingcatch0403.call_state

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.fishingcatch0403.stt.STTService

//private var recordingFilePath: String? = null   // 녹음 파일 경로를 저장하는 변수
//private lateinit var fileUtil: FileUtil    // 파일 저장을 위한 유틸리티 객체
private var phoneNumber: String? = null

class CallService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

//        fileUtil = FileUtil(contentResolver)  // FileUtil 인스턴스 생성

        // 수신 전화번호가 전달되었을 경우
        intent?.let {
            phoneNumber = it.getStringExtra("phoneNumber")
            Log.d("[APP] CallService", "CallService 시작: $phoneNumber")
        }

        return START_NOT_STICKY // 서비스는 강제로 종료된 후 자동으로 재시작되지 않음
    }

    override fun onBind(intent: Intent): IBinder? {
        return null // 바인딩 하지 않기 때문에 null 반환
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("[APP] CallService", "CallService 생성")
        createNotificationChannel() // 알림 채널 생성
        startForegroundService() // 포그라운드 서비스 시작
    }

    override fun onDestroy() {
        super.onDestroy()
        phoneNumber?.let { startClovaStt(this, it) }
        Log.d("[APP] CallService", "CallService 종료")
    }

    // Clova STT 서비스 시작 함수
    private fun startClovaStt(context: Context, phoneNumber: String) {
        val intent = Intent(context, STTService::class.java)
        intent.putExtra("phoneNumber", phoneNumber)
        context.startService(intent)
    }

    // 포그라운드 서비스 알림 생성
    @SuppressLint("ForegroundServiceType")
    private fun startForegroundService() {
        val notificationIntent = Intent(this, CallService::class.java)
        val pendingIntent = PendingIntent.getService(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, "CallServiceChannel")
            .setContentTitle("서비스 제공중")
            .setContentText("보이스 피싱 방지 서비스 제공 중 입니다")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification) // 포그라운드 서비스 시작
    }

    // 알림 채널 생성
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "CallServiceChannel",
            "보이스 피싱 방지 서비스 ",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
