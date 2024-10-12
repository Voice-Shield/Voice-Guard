package com.example.fishingcatch0403.fishingcatch0403.fragments.phishing_log

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// 알림 데이터베이스
@Database(entities = [NotificationItem::class], version = 1, exportSchema = false)
abstract class NotificationDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao

    // 싱글톤 패턴을 사용하여 데이터베이스 인스턴스를 유일하게 유지
    companion object {
        @Volatile
        private var INSTANCE: NotificationDatabase? = null

        // 데이터베이스 인스턴스를 반환하는 메서드
        fun getDatabase(context: Context): NotificationDatabase {
            return INSTANCE ?: synchronized(this) {
                // 데이터베이스 인스턴스가 없으면 생성
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotificationDatabase::class.java,
                    "notification_database"
                ).build()
                INSTANCE = instance // 인스턴스를 싱글톤으로 유지
                instance    // 생성된 인스턴스 반환
            }
        }
    }
}

