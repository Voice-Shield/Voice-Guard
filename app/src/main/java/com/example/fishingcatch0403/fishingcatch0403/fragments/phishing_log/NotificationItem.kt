package com.example.fishingcatch0403.fishingcatch0403.fragments.phishing_log

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateTime: String,
    val phoneNumber: String,
    val result: String,
    val resultType: Int // 1: 의심, 2: 안전
)
