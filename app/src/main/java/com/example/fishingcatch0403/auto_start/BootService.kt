package com.example.fishingcatch0403.auto_start

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.fishingcatch0403.MainActivity
import com.example.fishingcatch0403.R

class BootService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        // 알림을 클릭하면 앱이 실행되도록 설정
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "channel_id")
            .setContentTitle("보이스 피싱 방지")
            .setContentText("서비스 제공 대기 중 입니다")
            .setSmallIcon(R.drawable.logo)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW) // 낮은 우선순위로 설정
            .setOngoing(true) // 알림을 지속적으로 유지
            .build()

        startForeground(1, notification) // 포그라운드 서비스로 실행

        return START_STICKY
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            "channel_id",
            "Foreground Service Channel",
            NotificationManager.IMPORTANCE_LOW // 중요도를 LOW로 설정하여 소리와 진동을 끔
        ).apply {
            description = "서비스 알림 채널"
            // 소리 및 진동 끄기
            setSound(null, null) // 소리 비활성화
            enableVibration(false) // 진동 비활성화
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}
