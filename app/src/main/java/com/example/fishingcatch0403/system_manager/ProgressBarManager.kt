package com.example.fishingcatch0403.system_manager

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.fishingcatch0403.R
import com.example.fishingcatch0403.rest_api.isSTTResultReceived
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProgressBarManager(
    private val context: Context,
    private val notificationManager: NotificationManager,
    private val notificationId: Int,
    private val channelId: String
) {

    private var progressMain: Int = 0    // 진행률 변수
    private var job: Job? = null    // 코루틴 작업을 저장할 변수

    // ProgressBar 업데이트 함수
    fun updateProgressBar(progress: Int, expectedTime: Long) {
        val customView =
            RemoteViews(context.packageName, R.layout.notification_progress_layout).apply {
                setTextViewText(R.id.progressText, "진행률: $progress%")
                setProgressBar(R.id.progressBar, 100, progress, false)
            }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContent(customView)
            .setSmallIcon(R.drawable.diagram)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()

        progressMain = progress
        startProgressUpdate(expectedTime)

        // 알림 표시
        notificationManager.notify(notificationId, notification)
    }

    // ProgressBar 시작 함수
    private fun startProgressUpdate(expectedTime: Long) {
        if (expectedTime <= 0) return

        job?.cancel()
        job = CoroutineScope(Dispatchers.Main).launch {
            val startTime = System.currentTimeMillis()

            runCatching {
                while (progressMain < 100) {
                    delay(100) // 100ms 대기

                    // STT 결과가 수신된 경우
                    if (isSTTResultReceived) {
                        Log.d("[APP] ProgressBarManager", "STT 결과 수신됨 - ProgressBar 100% 설정")
                        progressMain = 100 // 진행률을 100으로 설정
                        updateProgressBar(progressMain, 0) // 진행률 업데이트
                        break
                    }

                    val elapsedTime = System.currentTimeMillis() - startTime
                    progressMain =
                        (elapsedTime * 100 / expectedTime).coerceAtMost(100).toInt() // 최대 100으로 제한

                    Log.d("[APP] ProgressBarManager", "현재 진행률: $progressMain")

                    // 현재 진행률 알림 업데이트
                    updateProgressBar(progressMain, 0)
                }
            }.onFailure { e ->
                Log.e("[APP] ProgressBarManager", "오류 발생: ${e.message}")
            }.also {
                Log.d("[APP] ProgressBarManager", "코루틴 종료")
            }
        }
    }
}
