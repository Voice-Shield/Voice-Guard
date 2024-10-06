package com.example.fishingcatch0403.stt

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.fishingcatch0403.R
import com.example.fishingcatch0403.analyzetxt.AnalyzeTxT
import com.example.fishingcatch0403.rest_api.ApiController
import com.example.fishingcatch0403.rest_api.SttResultCallback
import com.example.fishingcatch0403.system_manager.FileUtil
import com.example.fishingcatch0403.system_manager.ProgressBarManager

lateinit var notificationManager: NotificationManager
const val notificationId = 1
const val CHANNEL_ID = "STTServiceChannel"
private var phoneNumber: String? = null

class STTService : Service() {

    private lateinit var progressBarManager: ProgressBarManager
    private lateinit var apiController: ApiController

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        apiController = ApiController()

        // ProgressBarManager 초기화
        initProgressBarManager()

        createNotificationChannel() // 알림 채널 생성
        startForegroundService() // Foreground Service 시작
    }

    private fun initProgressBarManager() {
        progressBarManager =
            ProgressBarManager(this, notificationManager, notificationId, CHANNEL_ID)
        apiController.initProgressBarManager(progressBarManager) // ApiController에 ProgressBarManager 전달
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            phoneNumber = it.getStringExtra("phoneNumber")
            Log.d("[APP] STTService", "STTService 시작: $phoneNumber")
            analyzeRecording(phoneNumber)
        }
        return START_NOT_STICKY // 서비스는 강제로 종료된 후 자동으로 재시작되지 않음
    }

    // 녹음 파일 분석
    private fun analyzeRecording(phoneNumber: String?) {
        Log.d("[APP] STTService", "녹음 파일 분석 시작")
        // FileUtil로 최신 녹음 파일을 가져옴
        FileUtil(contentResolver).getLatestRecordingFile()?.run {
//            progressBarManager.updateProgressBar(0,5000) --> 현재 progressBar 실행중 정지 오류 발생
            // STT API를 호출하여 음성을 텍스트로 변환
            apiController.getSTTResult(this, object : SttResultCallback {
                override fun onSuccess(result: String) {
                    Log.d("[APP] STTService", "STT 결과: $result")
                    showResultNotification(result)
                    analyzeText(result, phoneNumber)
                }

                override fun onError(errorMessage: String) {
                    Log.e("[APP] STTService", "STT 오류: $errorMessage")
                    showResultNotification("STT 오류: $errorMessage")
                }
            })
        } ?: run {
            // 파일이 없을 경우 로그 출력
            Log.e("[APP] STTService", "녹음 파일을 찾을 수 없습니다.")
            notificationManager.cancelAll()
            showResultNotification("녹음 파일을 찾을 수 없습니다.")
        }
    }

    // STT로 변환한 텍스트를 AnalyzeTxT 서비스에 전달
    private fun analyzeText(text: String, phoneNumber: String?) {
        val intent = Intent(this, AnalyzeTxT::class.java)
        intent.putExtra("text", text)
        intent.putExtra("phoneNumber", phoneNumber)
        startService(intent)
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
    private fun showResultNotification(result: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("통화 내용")
            .setContentText(result) // 음성 인식 결과를 알림으로 설정
            .setStyle(NotificationCompat.BigTextStyle().bigText(result)) // 긴 텍스트 알림
            .setSmallIcon(R.drawable.connection)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(
            notificationId,
            notification
        )
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

    override fun onBind(intent: Intent): IBinder? {
        return null // 바인딩 하지 않기 때문에 null 반환
    }

}
