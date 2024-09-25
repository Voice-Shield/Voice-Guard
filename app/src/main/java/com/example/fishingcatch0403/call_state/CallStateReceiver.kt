package com.example.fishingcatch0403.call_state

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.telephony.TelephonyManager
import android.util.Log
import com.example.fishingcatch0403.system_manager.AlertService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private var phoneState = ""
private var foundPhoneNumber = false
private var onCalled = false
private var isCallServiceStart = false
const val INCOMING_NUMBER = TelephonyManager.EXTRA_INCOMING_NUMBER


class CallStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val isPhishingPreventionEnabled = getUserChoice(context)

        if (intent.action == "android.intent.action.PHONE_STATE") {
            intent.getStringExtra(TelephonyManager.EXTRA_STATE)?.let { currentState ->
                if (phoneState != currentState) {
                    foundPhoneNumber = false
                    phoneState = currentState
                }
                Log.d("[APP] CallState", "전화상태: $phoneState")

                if (foundPhoneNumber.not()) {
                    when (phoneState) {
                        TelephonyManager.EXTRA_STATE_RINGING -> {
                            Log.d("[APP] CallState", "통화수신")
                            val phoneNumber = intent.getStringExtra(INCOMING_NUMBER)
                            if (phoneNumber == null) {
                                Log.e("[APP] CallState", "수신 전화번호: null")
                            } else {
                                Log.d("[APP] CallState", "수신 전화번호: $phoneNumber")
                                goAsync(Dispatchers.IO) {
                                    alertService(context, phoneNumber)
                                }
                                foundPhoneNumber = true
                            }
                        }

                        TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                            Log.d("[APP] CallState", "통화진행")
                            val phoneNumber = intent.getStringExtra(INCOMING_NUMBER)
                            if (phoneNumber == null) {
                                Log.e("[APP] CallState", "수신 전화번호: null")
                            } else {
                                Log.d("[APP] CallState", "수신 전화번호: $phoneNumber")
                                goAsync(Dispatchers.IO) {
                                    stopCallService(context)
                                    if (isCallServiceStart.not()) {
                                        if (isPhishingPreventionEnabled) {
                                            startCallService(context, phoneNumber)
                                            // 보이스 피싱 방지 기능 활성화
                                            Log.d("[APP] CallStateReceiver", "서비스 제공")
                                        } else {
                                            // 기능 비활성화
                                            Log.d("[APP] CallStateReceiver", "서비스 미제공")
                                        }
                                        isCallServiceStart = true
                                    }
                                    onCalled = true
                                    foundPhoneNumber = true
                                }
                            }
                        }

                        TelephonyManager.EXTRA_STATE_IDLE -> {
                            Log.d("[APP] CallState", "통화종료")
                            if (isCallServiceStart) {
                                stopCallService(context)
                                isCallServiceStart = false
                            }
                            onCalled = false
                            foundPhoneNumber = true
                        }
                    }
                }
            }
        }
    }

    // 사용자가 보이스 피싱 방지 서비스를 선택했는지 확인하는 함수
    private fun getUserChoice(context: Context): Boolean {
        val sharedPref: SharedPreferences =
            context.getSharedPreferences("UserChoices", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("phishing_prevention", false) // 기본값은 false
    }

    // 보이스 피싱 방지 서비스 제공 받을지 선택할 수 있는 알림 함수
    private fun alertService(context: Context, phoneNumber: String) {
        val intent = Intent(context, AlertService::class.java)
        intent.putExtra("phoneNumber", phoneNumber)
        context.startService(intent)
    }

    // 통화 분석 서비스를 시작 함수
    private fun startCallService(context: Context, phoneNumber: String) {
        val intent = Intent(context, CallService::class.java)
        intent.putExtra("phoneNumber", phoneNumber)
        context.startService(intent)
    }

    // 통화 분석 서비스 종료 함수
    private fun stopCallService(context: Context) {
        val intent = Intent(
            context, CallService::class.java
        )
        context.stopService(intent)
    }

    // 코루틴 함수
    private fun BroadcastReceiver.goAsync(
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend CoroutineScope.() -> Unit
    ) {
        val pendingResult = goAsync()
        @OptIn(DelicateCoroutinesApi::class) // Must run globally; there's no teardown callback.
        GlobalScope.launch(context) {
            try {
                block()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
