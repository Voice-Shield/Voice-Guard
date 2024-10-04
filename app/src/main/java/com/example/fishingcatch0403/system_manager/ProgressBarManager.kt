package com.example.fishingcatch0403.system_manager

import android.app.NotificationManager
import android.content.Context
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.fishingcatch0403.R
import com.example.fishingcatch0403.rest_api.isSTTResultReceived
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

class ProgressBarManager(
    private val context: Context,
    private val notificationManager: NotificationManager,
    private val notificationId: Int,
    private val channelId: String
) {

    // ProgressBar 업데이트 함수
    fun updateProgressBar(progress: Int) {
        // 알림에서 ProgressBar와 텍스트 업데이트
        val customView =
            RemoteViews(context.packageName, R.layout.notification_progress_layout).apply {
                setTextViewText(R.id.progressText, "진행률: $progress%")
                setProgressBar(R.id.progressBar, 100, progress, false)
            }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContent(customView) // 커스텀 뷰를 알림으로 설정
            .setSmallIcon(R.drawable.diagram) // 아이콘 설정
            .setPriority(NotificationCompat.PRIORITY_LOW)   // 알림 우선순위 설정
            .setOnlyAlertOnce(true) // 알림 갱신 시 소리 알림 방지
            .setOngoing(true) // 진행 중 알림 설정
            .build()

        // 알림을 표시
        notificationManager.notify(notificationId, notification)
    }

    // ProgressBar 시작 함수
    fun startProgressUpdate(expectedTime: Long) {
        if (expectedTime <= 0) return // 예상 시간이 0 이하일 경우 함수 종료
        val startTime = System.currentTimeMillis()
        CoroutineScope(Dispatchers.Main).launch {
            for (i in 0 until expectedTime step 100) { // 예상 시간에 따라 진행률 업데이트
                delay(100) // 100ms 대기

                if (isSTTResultReceived) {
                    updateProgressBar(100) // STT 결과 수신 시 100%로 설정
                    break // 루프 종료
                }

                val elapsedTime = System.currentTimeMillis() - startTime
                val progress = (elapsedTime * 100 / expectedTime).toInt()

                if (progress >= 100) {
                    updateProgressBar(100) // 100%에 도달하면 업데이트
                    break // 루프 종료
                } else {
                    updateProgressBar(min(progress, 100)) // 진행률 업데이트
                }
            }
        }
    }
}
