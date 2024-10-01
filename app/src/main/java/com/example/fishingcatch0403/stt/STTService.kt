package com.example.fishingcatch0403.stt

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.fishingcatch0403.R
import com.example.fishingcatch0403.rest_api.ApiController
import com.example.fishingcatch0403.rest_api.SttResultCallback
import com.example.fishingcatch0403.system_manager.FileUtil
import com.example.fishingcatch0403.system_manager.ProgressBarManager


lateinit var notificationManager: NotificationManager
const val notificationId = 1
const val CHANNEL_ID = "STTServiceChannel"
lateinit var apiController: ApiController

class STTService : Service() {

    private lateinit var progressBarManager: ProgressBarManager

    override fun onCreate() {
        super.onCreate()
        Log.d("[APP] STTService", "STTService 생성")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // ProgressBarManager 초기화
        apiController.initProgressBarManager(this)
        createNotificationChannel() // 알림 채널 생성
        startForegroundService() // Foreground Service 시작
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        analyzeRecording(this)
        return START_NOT_STICKY // 서비스는 강제로 종료된 후 자동으로 재시작되지 않음
    }

    override fun onBind(intent: Intent): IBinder? {
        return null // 바인딩 하지 않기 때문에 null 반환
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("[APP] STTSService", "STTService 종료")
    }

    // 녹음 파일 분석
    private fun analyzeRecording(context: Context) {
        Log.d("[APP] STTService", "녹음 파일 분석 시작")
        FileUtil(contentResolver).getLatestRecordingFile()?.run {
            ApiController().getSTTResult(context, this, object : SttResultCallback {
                override fun onSuccess(result: String) {
                    Log.d("[APP] STTService", "STT 결과: $result")
                    progressBarManager.stopProgressUpdate()
                    progressBarManager.updateProgressBar(100)  // 완료 시 100%
                    updateNotificationR(result)
                }

                override fun onError(errorMessage: String) {
                    Log.e("[APP] STTService", "STT 오류: $errorMessage")
                    progressBarManager.stopProgressUpdate()
                    progressBarManager.updateProgressBar(0)  // 실패 시 0%
                    updateNotificationR("STT 오류: $errorMessage")
                }
            })
        }
    }

    // Foreground Service 시작 및 기본 알림 생성
    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("음성 인식 중")
            .setContentText("녹음 파일을 분석하고 있습니다.")
            .setSmallIcon(R.drawable.idea) // 아이콘 설정
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(notificationId, notification)
    }

    // 음성 인식 결과에 따른 알림 업데이트
    private fun updateNotificationR(result: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("음성 인식 결과")
            .setContentText(result) // 음성 인식 결과를 알림으로 설정
            .setStyle(NotificationCompat.BigTextStyle().bigText(result)) // 긴 텍스트 알림
            .setSmallIcon(R.drawable.connection)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        )
            notificationManager.notify(notificationId, notification)
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
