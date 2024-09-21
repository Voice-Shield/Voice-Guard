package com.example.fishingcatch0403.fishingcatch0403.fragments.manager

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

class BatteryOptimizationHelper(private val context: Context) {

    private val prefsName = "MyAppPrefs"    // SharedPreferences 이름
    private val firstRun = "first_run"      // 처음 실행 여부를 저장하는 키
    private val sharedPreferences: SharedPreferences =  // SharedPreferences 인스턴스
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    // 앱이 처음 실행되는지 확인
    fun isFirstRun(): Boolean {
        val isFirstRun = sharedPreferences.getBoolean(firstRun, true)   // 처음 실행 여부를 가져옴
        if (isFirstRun) {   // 처음 실행인 경우
            sharedPreferences.edit().putBoolean(firstRun, false).apply()    // 처음 실행 여부를 false로 설정
        }
        return isFirstRun
    }

    // 다이얼로그 표시
    fun showBatteryOptimizationDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("배터리 절전 모드 방지")
        builder.setMessage("앱이 백그라운드에서 계속 실행되도록 배터리 절전 모드를 해제하시겠습니까?")
        builder.setPositiveButton("설정으로 이동") { dialog, _ ->
            requestIgnoreBatteryOptimization()
            dialog.dismiss()
        }
        builder.setNegativeButton("취소") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    // 배터리 최적화 예외 설정 화면으로 이동
    private fun requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager  // PowerManager 인스턴스
            val packageName = context.packageName   // 현재 앱의 패키지 이름

            // 배터리 최적화에서 이미 예외로 설정된 경우
            if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Toast.makeText(context, "이미 배터리 최적화 예외로 설정되어 있습니다.", Toast.LENGTH_SHORT).show()
            } else {
                // 배터리 최적화 예외를 요청하는 인텐트 실행
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                context.startActivity(intent)
            }
        } else {
            Toast.makeText(context, "Android 6.0 이상에서만 지원됩니다.", Toast.LENGTH_SHORT).show()
        }
    }
}

