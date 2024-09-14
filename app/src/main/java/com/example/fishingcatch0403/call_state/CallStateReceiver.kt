package com.example.fishingcatch0403.call_state

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
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
                            intent.getStringExtra(INCOMING_NUMBER)
                                ?.run {
                                    Log.d("[APP] CallState", "전화번호: $this")
                                    foundPhoneNumber = true
                                }
                        }

                        TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                            Log.d("[APP] CallState", "통화진행")
                            val phoneNumber = intent.getStringExtra(INCOMING_NUMBER)
                            if (phoneNumber == null) {
                                Log.e("[APP] CallState", "전화번호: null")
                            } else {
                                Log.d("[APP] CallState", "전화번호: $phoneNumber")
                                goAsync(Dispatchers.IO) {
                                    Log.d("[APP] CallState", "Async block 시작")
                                    stopCallService(context)
                                    if (isCallServiceStart.not()) {
                                        startCallService(context, phoneNumber)
                                        isCallServiceStart = true
                                    }
                                    Log.d("[APP] CallState", "CallService 시작됨")
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

    private fun startCallService(context: Context, phoneNumber: String) {
        Log.d("[APP] CallState", "Start CallService with phoneNumber: $phoneNumber")
        val intent = Intent(context, CallService::class.java)
        intent.putExtra("phoneNumber", phoneNumber)
        context.startService(intent)

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            context.startForegroundService(intent) // Oreo 이상에서는 startForegroundService 사용
//        } else {
//
//        }

        // 서비스가 정상적으로 시작되었는지 확인
        val componentName = context.startService(intent)
        if (componentName == null) {
            Log.e("[APP] CallState", "CallService 시작 실패")
        } else {
            Log.d("[APP] CallState", "CallService 시작 성공")
        }
    }


    private fun stopCallService(context: Context) {
        Log.d("[APP] CallState", "Stop Service")
        val intent = Intent(
            context, CallService::class.java
        )
        context.stopService(intent)
    }

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
