package com.example.fishingcatch0403.system_manager

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import com.example.fishingcatch0403.R

private val contactUtil = ContactUtil() // ContactUtil 인스턴스 생성

class AlertService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 수신 전화번호가 전달되었을 경우
        intent?.let {
            val phoneNumber = it.getStringExtra("phoneNumber")
            Log.d("[APP] AlertService", "AlertService 시작: $phoneNumber")

            phoneNumber?.let { number ->
                // 주소록에 없는 번호인지 확인
                if (!contactUtil.isNumInContacts(this, number)) {
                    Log.d("[APP] AlertService", "연락처에 없는 번호")
                    showPhishingAlert(this)
                } else {
                    Log.d("[APP] AlertService", "연락처에 등록된 번호")
                    stopSelf() // 서비스 종료
                }
            }
        }
        return START_NOT_STICKY // 서비스는 강제로 종료된 후 자동으로 재시작되지 않음
    }

    override fun onBind(intent: Intent): IBinder? {
        return null // 바인딩 하지 않기 때문에 null 반환
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("[APP] AlertService", "AlertService 생성")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("[APP] AlertService", "AlertService 종료")
    }

    // 사용자가 보이스 피싱 방지 서비스를 선택했는지 저장하는 함수
    private fun saveUserChoice(context: Context, choice: Boolean) {
        val sharedPref: SharedPreferences =
            context.getSharedPreferences("UserChoices", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putBoolean("phishing_prevention", choice) // 사용자가 "예"를 선택하면 true 저장
        editor.apply() // 비동기식으로 저장
    }

    // 보이스 피싱 방지 서비스 선택 알림을 플로팅으로 표시
    private fun showPhishingAlert(context: Context) {
        if (Settings.canDrawOverlays(context)) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val layoutInflater = LayoutInflater.from(context)
            val phishingAlertView = layoutInflater.inflate(R.layout.phishing_alert_layout, null)
            phishingAlertView.setBackgroundResource(R.drawable.floating_background)

            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            )

            layoutParams.gravity = Gravity.CENTER // 화면 중앙에 표시

            // '예' 버튼 클릭 리스너
            phishingAlertView.findViewById<Button>(R.id.yesButton).setOnClickListener {
                windowManager.removeView(phishingAlertView) // 뷰 제거
                saveUserChoice(context, true) // "예" 선택 시 처리
                showRecordingAlert(context) // 통화 녹음 알림 표시
            }

            // '아니오' 버튼 클릭 리스너
            phishingAlertView.findViewById<Button>(R.id.noButton).setOnClickListener {
                windowManager.removeView(phishingAlertView) // 뷰 제거
                saveUserChoice(context, false) // "아니오" 선택 시 처리
            }

            // WindowManager를 통해 뷰 추가
            windowManager.addView(phishingAlertView, layoutParams)

        } else {
            // 권한이 없으면 권한 요청
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
        }
    }

    // 통화 녹음 활성화 알림을 플로팅으로 표시
    private fun showRecordingAlert(context: Context) {
        if (Settings.canDrawOverlays(context)) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val layoutInflater = LayoutInflater.from(context)
            val recordingAlertView = layoutInflater.inflate(R.layout.recording_alert_layout, null)
            recordingAlertView.setBackgroundResource(R.drawable.floating_background)

            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            )

            layoutParams.gravity = Gravity.CENTER // 화면 중앙에 표시

            // '확인' 버튼 클릭 리스너
            recordingAlertView.findViewById<Button>(R.id.confirmButton).setOnClickListener {
                windowManager.removeView(recordingAlertView) // 뷰 제거
            }

            // WindowManager를 통해 뷰 추가
            windowManager.addView(recordingAlertView, layoutParams)
        }
    }
}
