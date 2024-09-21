package com.example.fishingcatch0403.dialer

import android.content.Context
import android.content.Intent
import android.telecom.TelecomManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog

// 디바이스의 다이얼러 앱을 관리하는 클래스
class DialerManager(private val context: Context) {

    // 다이얼러 앱을 시작하는 함수
    fun startDialer(launcher: ActivityResultLauncher<Intent>) {

        // 기본 인텐트 설정
        val settingsIntent = Intent(TelecomManager.ACTION_SHOW_CALL_SETTINGS)

        // 다이얼러 앱이 설치되어 있는지 확인
        if (context.packageManager.resolveActivity(settingsIntent, 0) != null) {
            // 통화 설정에서 자동 녹음 활성화 요청
            Toast.makeText(context, "\"통화 녹음\"에서 \"통화 자동 녹음\"을 활성화 해주세요.", Toast.LENGTH_LONG).show()
            // 통화 설정 화면으로 이동
            launcher.launch(settingsIntent)
        } else {
            // 통화 설정 화면을 열 수 없는 경우 다이얼로그로 안내
            Log.d("[APP] DialerManager", "전화 앱의 설정 화면을 열 수 없습니다.")
            showAlertDialog()
        }
    }

    // 다이얼로그를 보여주는 함수
    private fun showAlertDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("안내")
            .setMessage("\"전화 앱\"의 \"통화 녹음\"에서 \"통화 자동 녹음\"을 활성화 해주세요.")
            .setPositiveButton("확인") { dialog, _ ->
                dialog.dismiss()
                // 전화 앱을 실행하는 인텐트
                val dialerIntent = Intent(Intent.ACTION_DIAL)
                dialerIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(dialerIntent)
            }
            .setCancelable(false)

        // 다이얼로그 표시
        builder.create().show()
    }
}
