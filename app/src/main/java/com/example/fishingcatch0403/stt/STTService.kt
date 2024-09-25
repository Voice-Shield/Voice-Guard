package com.example.fishingcatch0403.stt

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import com.example.fishingcatch0403.R

private val invoke_url = ""
private val secret = ""

class STTService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showSpeakerSelection(this)
        return START_NOT_STICKY // 서비스는 강제로 종료된 후 자동으로 재시작되지 않음
    }

    override fun onBind(intent: Intent): IBinder? {
        return null // 바인딩 하지 않기 때문에 null 반환
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("[APP] STTService", "ClovaSTTService 생성")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("[APP] STTSService", "ClovaSTTService 종료")
    }

    // 화자 수 선택 플로팅 창 표시 함수
    private fun showSpeakerSelection(context: Context) {
        if (Settings.canDrawOverlays(context)) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val layoutInflater = LayoutInflater.from(context)
            val speakerSelectionView =
                layoutInflater.inflate(R.layout.speaker_selection_layout, null)

            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            )

            layoutParams.gravity = Gravity.CENTER // 화면 중앙에 표시

            // 2명 선택 버튼
            speakerSelectionView.findViewById<Button>(R.id.two_speakers).setOnClickListener {
                analyzeRecording(context, 2) // 녹음 분석 시작 (2명)
                windowManager.removeView(speakerSelectionView) // 플로팅 뷰 제거
            }

            // 3명 선택 버튼
            speakerSelectionView.findViewById<Button>(R.id.three_speakers).setOnClickListener {
                analyzeRecording(context, 3) // 녹음 분석 시작 (3명)
                windowManager.removeView(speakerSelectionView) // 플로팅 뷰 제거
            }

            // 4명 선택 버튼
            speakerSelectionView.findViewById<Button>(R.id.four_speakers).setOnClickListener {
                analyzeRecording(context, 4) // 녹음 분석 시작 (4명)
                windowManager.removeView(speakerSelectionView) // 플로팅 뷰 제거
            }

            // 5명 선택 버튼
            speakerSelectionView.findViewById<Button>(R.id.five_speakers).setOnClickListener {
                analyzeRecording(context, 5) // 녹음 분석 시작 (5명)
                windowManager.removeView(speakerSelectionView) // 플로팅 뷰 제거
            }

            // WindowManager를 통해 뷰 추가
            windowManager.addView(speakerSelectionView, layoutParams)
        }
    }

    // 선택된 화자 수에 따라 녹음 분석 처리 함수
    private fun analyzeRecording(context: Context, speakerCount: Int) {
        Log.d("[APP] STTService", "화자 수: $speakerCount - 녹음 파일 분석 시작")
        // 녹음 파일 분석 로직 추가
    }


}
