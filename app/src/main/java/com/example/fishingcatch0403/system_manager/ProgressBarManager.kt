package com.example.fishingcatch0403.system_manager

import android.app.NotificationManager
import android.content.Context
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.fishingcatch0403.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

class ProgressBarManager(
    private val context: Context,
    private val notificationManager: NotificationManager,
    private val notificationId: Int,
    private val channelId: String
) {
    private var progressJob: Job? = null

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
            .setAutoCancel(true) // 완료 시 알림 자동 삭제
            .build()

        // 알림을 표시
        notificationManager.notify(notificationId, notification)
    }

    // 실시간 시간 기반 ProgressBar 업데이트
    fun startProgressUpdate(expectedTime: Long) {
        progressJob = CoroutineScope(Dispatchers.Main).launch {
            val startTime = System.currentTimeMillis()
            var progress: Int

            while (isActive) {
                val elapsedTime = System.currentTimeMillis() - startTime
                progress = (elapsedTime * 100 / expectedTime).toInt()

                // 진행률이 100을 넘지 않도록 제한
                updateProgressBar(min(progress, 100))

                // Progress가 100%에 도달하면 종료
                if (progress >= 100) break

                delay(100)  // 100ms 간격으로 진행률 업데이트
            }
        }
    }

    // ProgressBar 업데이트 중지
    fun stopProgressUpdate() {
        progressJob?.cancel()
    }
}
