package com.example.fishingcatch0403.stt

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.example.fishingcatch0403.R

private var phoneNumber: String? = null // phoneNumber 저장 변수
private const val CHANNEL_ID = "STTServiceChannel"

class STTService : Service(), LifecycleOwner {

//    private lateinit var sttProcessor: STTProcessor // STTProcessor 인스턴스
    private val lifecycleRegistry = LifecycleRegistry(this) // LifecycleRegistry 인스턴스

    override fun onCreate() {
        super.onCreate()
        Log.d("[APP] STTService", "ClovaSTTService 생성")

        createNotificationChannel() // 알림 채널 생성
        startForegroundService() // Foreground Service 시작

//        sttProcessor = STTProcessor(contentResolver) // STTProcessor 초기화

        // 진행 상황과 음성 인식 결과를 관찰
//        sttProcessor.progressLiveData.observe(this) { progress ->
//            updateNotification(progress) // 진행 상황을 알림으로 업데이트
//        }
//
//        sttProcessor.speechResult.observe(this) { result ->
//            updateNotificationR(result) // 음성 인식 결과를 알림으로 업데이트
//        }

        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            phoneNumber = it.getStringExtra("phoneNumber") // phoneNumber 가져오기
            Log.d("[APP] STTService", "전달된 전화번호: $phoneNumber")
            showSpeakerSelection(this) // 플로팅 창 표시
        }
        return START_NOT_STICKY // 서비스는 강제로 종료된 후 자동으로 재시작되지 않음
    }

    override fun onBind(intent: Intent): IBinder? {
        return null // 바인딩 하지 않기 때문에 null 반환
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED // 생명주기 상태 업데이트
        super.onDestroy()
        Log.d("[APP] STTSService", "ClovaSTTService 종료")
    }

    // 화자 수 선택 플로팅 창 표시 함수
    @SuppressLint("InflateParams")
    private fun showSpeakerSelection(context: Context) {
        if (Settings.canDrawOverlays(context)) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val layoutInflater = LayoutInflater.from(context)
            val speakerSelectionView =
                layoutInflater.inflate(R.layout.speaker_selection_layout, null)

            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER // 화면 중앙에 표시
            }

            // 선택 버튼 설정
            listOf(2, 3, 4, 5).forEach { speakerCount ->
                speakerSelectionView.findViewById<Button>(getButtonId(speakerCount))
                    .setOnClickListener {
                        analyzeRecording(context, speakerCount, phoneNumber) // 전화번호 전달
                        windowManager.removeView(speakerSelectionView) // 플로팅 뷰 제거
                    }
            }

            // WindowManager를 통해 뷰 추가
            windowManager.addView(speakerSelectionView, layoutParams)
        }
    }

    private fun getButtonId(speakerCount: Int): Int {
        return when (speakerCount) {
            2 -> R.id.two_speakers
            3 -> R.id.three_speakers
            4 -> R.id.four_speakers
            5 -> R.id.five_speakers
            else -> throw IllegalArgumentException("Invalid speaker count")
        }
    }

    // 선택된 화자 수에 따라 녹음 분석 처리 함수
    private fun analyzeRecording(context: Context, speakerCount: Int, phoneNumber: String?) {
        Log.d("[APP] STTService", "화자 수: $speakerCount - 녹음 파일 분석 시작, 전화번호: $phoneNumber")
        phoneNumber?.let {
//            sttProcessor.recognizeSpeech(context, speakerCount, it) // STTProcessor에서 음성 인식 호출
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

        startForeground(1, notification)
    }

    // 진행 상황에 따른 알림 업데이트
    private fun updateNotification(progress: Int) {
        val customView = RemoteViews(packageName, R.layout.notification_progress_layout).apply {
            setTextViewText(R.id.progress_text, "진행 상황: $progress%")
            setProgressBar(R.id.progress_bar, 100, progress, false)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContent(customView) // 커스텀 뷰를 알림으로 설정
            .setSmallIcon(R.drawable.diagram) // 아이콘 설정
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true) // 알림 갱신 시 소리 알림 방지
            .build()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }

    // 음성 인식 결과에 따른 알림 업데이트
    private fun updateNotificationR(result: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("음성 인식 결과")
            .setContentText(result) // 음성 인식 결과를 알림으로 설정
            .setSmallIcon(R.drawable.connection)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
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

    // LifecycleOwner 인터페이스 구현
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}
