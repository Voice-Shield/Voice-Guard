package com.example.fishingcatch0403.fishingcatch0403.fragments.phishing_log

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

// 알림 데이터베이스 접근 객체
@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationItem)  // 알림 삽입

    @Query("SELECT * FROM notifications")
    suspend fun getAllNotifications(): List<NotificationItem> // 모든 알림 가져오기

    @Delete
    suspend fun deleteNotification(notification: NotificationItem) // 알림 삭제
}

