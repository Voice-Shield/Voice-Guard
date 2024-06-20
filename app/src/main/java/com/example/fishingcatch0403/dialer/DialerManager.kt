package com.example.fishingcatch0403.dialer

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher

// 디바이스의 다이얼러 앱을 관리하는 클래스
class DialerManager(private val context: Context) {

    // 다이얼러 앱을 시작하는 함수
    fun startDialer(launcher: ActivityResultLauncher<Intent>) {
        // 사용자에게 자동 통화 녹음을 켜달라는 안내 메시지를 보여주는 토스트 메시지
        Toast.makeText(context, "설정의 통화 녹음에서 자동 통화 녹음을 켜 주세요", Toast.LENGTH_LONG).show()

        // 기본 인텐트 설정
        val intent = Intent(Intent.ACTION_MAIN)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        // 삼성 다이얼러 패키지 이름
        val packageName = "com.samsung.android.dialer"

        // 삼성 다이얼러 앱을 직접 시작하기 위한 인텐트 설정
        val launchIntent = Intent(Intent.ACTION_MAIN)
        launchIntent.setPackage(packageName)
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        // 삼성 다이얼러 앱이 설치되어 있으면 해당 앱을 시작
        if (context.packageManager.resolveActivity(launchIntent, 0) != null) {
            launcher.launch(launchIntent)
            return
        }

        // 삼성 다이얼러 앱이 설치되어 있지 않은 경우, 기본 다이얼러 액티비티를 시작
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.setClassName(
            "com.samsung.android.dialer",
            "com.samsung.android.dialer.DialtactsActivity"
        )

        context.startActivity(intent)
    }
}

