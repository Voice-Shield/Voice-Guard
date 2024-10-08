package com.example.fishingcatch0403.fishingcatch0403.fragments.phishing_log

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationItem)

    @Query("SELECT * FROM notifications")
    suspend fun getAllNotifications(): List<NotificationItem>

    @Delete
    suspend fun deleteNotification(notification: NotificationItem)
}

