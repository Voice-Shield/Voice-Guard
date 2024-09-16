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
                                        startCallService(context, phoneNumber)
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

    private fun startCallService(context: Context, phoneNumber: String) {
        val intent = Intent(context, CallService::class.java)
        intent.putExtra("phoneNumber", phoneNumber)
        context.startService(intent)
    }

    private fun stopCallService(context: Context) {
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
