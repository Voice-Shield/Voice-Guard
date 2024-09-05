package com.example.fishingcatch0403.call_state

import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.IBinder
import android.provider.ContactsContract
import android.util.Log
import java.util.Date

private var mediaRecorder: MediaRecorder? = null
private var isRecording = false
private var recordingFilePath = ""

class CallService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val phoneNumber = it.getStringExtra("phoneNumber")
            Log.d("[APP] CallService", "CallService 시작: $phoneNumber")

            phoneNumber?.let { number ->
                // 주소록에 없는 번호인지 확인
                if (!isNumInContacts(this, number)) {
                    startRecording(this, number)
                } else {
                    Log.d("[APP] CallService", "연락처에 등록된 번호")
                    stopSelf() // 녹음할 필요가 없으므로 서비스 중지
                }
            }
        }
        return START_NOT_STICKY // 서비스는 강제로 종료된 후 자동으로 재시작되지 않음
    }

    override fun onBind(intent: Intent): IBinder? {
        return null // 바인딩 하지 않기 때문에 null 반환
    }

    override fun onCreate() {
        Log.d("[APP] CallService", "CallService 생성")
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording() // 서비스 종료 시 녹음 중지
        Log.d("[APP] CallService", "CallService 종료")
    }

    private fun isNumInContacts(context: Context, phoneNumber: String): Boolean {
        val contentResolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        // 주소록의 모든 번호를 조회
        val cursor: Cursor? = contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )

        cursor?.use {
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val contactNumber = cursor.getString(numberIndex)

                // 전화번호를 동일한 형식으로 비교하기 위한 공백 및 하이픈 제거
                val normalizedContactNumber = contactNumber.replace(Regex("[^\\d]"), "")
                val normalizedIncomingPhoneNumber = phoneNumber.replace(Regex("[^\\d]"), "")

                // 연락처에 저장된 번호와 수신된 번호가 동일한지 비교
                if (normalizedContactNumber == normalizedIncomingPhoneNumber) {
                    return true // 연락처에 저장된 번호와 수신 번호가 일치하면 true 반환
                }
            }
        }
        return false    // 연락처에 저장된 번호와 일치하는 번호가 없다면 false 반환
    }

    private fun startRecording(context: Context, phoneNumber: String) {
        if (!isRecording) {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )

                mediaRecorder = MediaRecorder()
                mediaRecorder?.apply {
                    setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4) // M4A 파일 형식
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC) // AAC 인코더 사용
                    setAudioSamplingRate(44100) // 샘플링 레이트 설정
                    setAudioEncodingBitRate(128000) // 비트레이트 설정 (128kbps 권장)
                    recordingFilePath =
                        context.getExternalFilesDir(null)?.absolutePath + "/Recordings/Call/Recording_Call" + phoneNumber + "${Date().time}.m4a"
                    setOutputFile(recordingFilePath)

                    prepare()
                    start()
                    isRecording = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("[APP] CallRecording", "녹음 시작 실패: ${e.message}")
            }
        }
    }

    private fun stopRecording() {
        if (isRecording) {
            try {
                mediaRecorder?.apply {
                    stop()
                    reset()
                    release()
                    isRecording = false
                    Log.d("[APP] CallRecording", "녹음 종료: $recordingFilePath")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("[APP] CallRecording", "녹음 종료 실패: ${e.message}")
            }
        }
    }
}
