package com.example.fishingcatch0403.call_state

import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.MediaRecorder
import android.os.IBinder
import android.provider.ContactsContract
import android.util.Log
import java.io.File
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
                    Log.d("[APP] CallService", "연락처에 없는 번호")
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

    // 수신 전화번호가 주소록에 없는 번호인지 확인하는 함수
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
            if (numberIndex == -1) {
                Log.e("[APP] CallService", "주소록에서 번호 찾기 실패")
                return false
            }
            while (cursor.moveToNext()) {
                val contactNumber = cursor.getString(numberIndex)   // 주소록의 번호
                // 전화번호를 동일한 형식으로 비교하기 위한 공백 및 하이픈 제거
                val normalizedContactNumber = contactNumber.replace(Regex("[^\\d]"), "")
                val normalizedIncomingPhoneNumber = phoneNumber.replace(Regex("[^\\d]"), "")
                // 로그로 비교한 번호 출력
                Log.d(
                    "[APP] CallService",
                    "Comparing: $normalizedContactNumber with $normalizedIncomingPhoneNumber"
                )
                // 연락처에 저장된 번호와 수신된 번호가 동일한지 비교
                if (normalizedContactNumber == normalizedIncomingPhoneNumber) {
                    return true // 연락처에 저장된 번호와 수신 번호가 일치하면 true 반환
                }
            }
        }
        return false    // 연락처에 저장된 번호와 일치하는 번호가 없다면 false 반환
    }

    private fun startRecording(context: Context, phoneNumber: String) {
        // 현재 녹음 중이 아니라면 녹음을 시작
        if (!isRecording) {
            try {
                // MediaRecorder 객체 생성 및 설정
                mediaRecorder = MediaRecorder()
                mediaRecorder?.apply {
                    // 마이크 입력을 소스로 설정
                    setAudioSource(MediaRecorder.AudioSource.MIC)

                    // 출력 파일 형식을 MPEG_4로 설정
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

                    // 오디오 인코더를 AAC로 설정
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

                    // 샘플링 레이트와 인코딩 비트레이트 설정
                    setAudioSamplingRate(44100)
                    setAudioEncodingBitRate(128000)

                    // 파일 경로를 설정하고 폴더 생성 로직 수행
                    val baseDir = context.getExternalFilesDir(null)?.absolutePath

                    // 외부 저장소 권한 문제가 있을 수 있는 경우를 처리
                    if (baseDir == null) {
                        Log.e("[APP] VoiceRecording", "파일 경로를 가져올 수 없습니다. 외부 저장소 권한 문제일 수 있습니다.")
                        return
                    }

                    // 녹음 파일이 저장될 경로 설정
                    val fullPath = "$baseDir/Recordings/Call/"
                    val dir = File(fullPath)

                    // 경로가 존재하지 않으면 폴더를 생성
                    if (!dir.exists()) {
                        val isDirCreated = dir.mkdirs()

                        // 폴더 생성에 실패한 경우 에러 로그 출력
                        if (!isDirCreated) {
                            Log.e("[APP] VoiceRecording", "폴더 생성 실패: $fullPath")
                            return
                        } else {
                            Log.i("[APP] VoiceRecording", "폴더 생성 성공: $fullPath")
                        }
                    } else {
                        // 폴더가 이미 존재할 경우 로그 출력
                        Log.i("[APP] VoiceRecording", "폴더가 이미 존재합니다: $fullPath")
                    }

                    // 녹음 파일 이름 생성 (시간과 전화번호를 포함하여 유니크하게 생성)
                    val recordingFilePath =
                        "$fullPath/Recording_Voice_${phoneNumber}_${Date().time}.m4a"

                    // MediaRecorder가 녹음할 파일 경로 설정
                    setOutputFile(recordingFilePath)

                    // 녹음 준비 및 시작
                    try {
                        prepare()  // MediaRecorder 준비
                        start()    // 녹음 시작
                        isRecording = true // 녹음 상태 업데이트

                        // 성공적으로 녹음이 시작되었음을 알리는 로그
                        Log.i("[APP] VoiceRecording", "녹음 시작: $recordingFilePath")
                    } catch (e: Exception) {
                        // 녹음 준비 또는 시작 실패 시 예외 처리 및 로그 출력
                        Log.e("[APP] VoiceRecording", "녹음 준비 또는 시작 실패: ${e.message}")
                        return
                    }
                }
            } catch (e: Exception) {
                // 전체적인 예외 처리 (오류 발생 시 스택 트레이스를 출력)
                e.printStackTrace()
                Log.e("[APP] VoiceRecording", "녹음 시작 실패: ${e.message}")
            }
        }
    }

    private fun stopRecording() {
        if (isRecording) {
            try {
                mediaRecorder?.apply {
                    stop()  // 녹음 중지
                    reset() // MediaRecorder를 초기 상태로 리셋
                    release()  // 리소스 해제
                    mediaRecorder = null // 객체 해제 후 참조 제거
                    isRecording = false
                    Log.d("[APP] VoiceRecording", "녹음 종료: $recordingFilePath")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("[APP] VoiceRecording", "녹음 종료 실패: ${e.message}")
            }
        }
    }
}
