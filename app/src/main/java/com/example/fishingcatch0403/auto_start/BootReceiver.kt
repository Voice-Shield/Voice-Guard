package com.example.fishingcatch0403.auto_start

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, BootService::class.java)
            // 부팅 후 실행 플래그 추가
            serviceIntent.putExtra("fromBoot", true)
            context?.startForegroundService(serviceIntent)
        }
    }
}


