package com.example.fishingcatch0403.dialer

import android.content.Context
import android.content.SharedPreferences

class CallTrackingManager(private val context: Context) {

    // SharedPreferences 인스턴스를 지연 초기화로 선언합니다. 앱의 고유 설정 파일에 접근합니다.
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(
            "com.android.recordcall.PREFERENCE_FILE_KEY",
            Context.MODE_PRIVATE
        )
    }

    // 'is_start_dialer_called' 키로 저장된 값(다이얼러 시작 여부)을 조회합니다. 기본값은 false입니다.
    fun isStartDialerCalled(): Boolean {
        return sharedPreferences.getBoolean("is_start_dialer_called", false)
    }

    // 'is_start_dialer_called' 키에 대해 true 값을 저장하여, 다이얼러가 시작되었음을 표시합니다.
    fun markStartDialerCalled() {
        sharedPreferences.edit().putBoolean("is_start_dialer_called", true).apply()
    }
}
