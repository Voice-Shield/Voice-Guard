package com.example.fishingcatch0403.call_state

import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.IBinder
import android.provider.ContactsContract
import android.util.Log
import com.example.fishingcatch0403.system_manager.FileUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

//private var mediaRecorder: MediaRecorder? = null  // 오디오 녹음을 위한 MediaRecorder 객체
private var recordingFilePath: String? = null   // 녹음 파일 경로를 저장하는 변수
private var isRecording = false // 녹음 중인지 여부를 제어하는 플래그
private lateinit var fileUtil: FileUtil    // 파일 저장을 위한 유틸리티 객체

class CallService : Service() {

    private var recordingThread: AudioRecordingThread? = null  // 녹음 스레드를 관리할 변수

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        fileUtil = FileUtil(contentResolver)  // FileUtil 인스턴스 생성
        // 수신 전화번호가 전달되었을 경우
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
                val normalizedContactNumber = contactNumber.replace(Regex("\\D"), "")
                val normalizedIncomingPhoneNumber = phoneNumber.replace(Regex("\\D"), "")
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

    // 녹음 시작 함수
    private fun startRecording(context: Context, phoneNumber: String) {
        // 현재 녹음 중이 아닐 경우에만 녹음 시작
        if (!isRecording) {
            // 오디오 설정 정보 정의
            val sampleRate = 44100 // 샘플링 레이트 설정
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT // 16비트 PCM 형식 설정
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                audioFormat
            )  // 버퍼 크기 계산

            // 저장할 파일의 이름 생성 (전화번호와 시간 정보 포함)
            val fileName = "Recording_Audio_${phoneNumber}_${
                SimpleDateFormat(
                    "yyyy.MM.dd_HH:mm:ss",
                    Locale.getDefault()
                ).format(Date())
            }.m4a"
            val fileUri = fileUtil.createFileUri(fileName) // 녹음 파일을 저장할 URI 생성
            recordingFilePath = fileUri.toString() // 녹음 파일 경로 저장

            // AudioRecordingThread 스레드 시작
            recordingThread = AudioRecordingThread(
                context,
                fileUri,
                bufferSize,
                sampleRate,
                audioFormat
            )
            recordingThread?.start()

            isRecording = true // 녹음 상태를 true로 변경
            Log.i("[APP] AudioRecording", "녹음 시작: $recordingFilePath")
        }
    }

    // 녹음 중지 함수
    private fun stopRecording() {
        if (isRecording) {
            recordingThread?.stopRecording()  // 스레드에서 녹음 중지 호출
            recordingThread = null  // 스레드 객체 해제
            isRecording = false  // 녹음 중 상태 해제
            Log.i("[APP] AudioRecording", "녹음 종료: $recordingFilePath")
        }
    }

//    // 녹음 시작 함수
//    private fun startRecording(context: Context, phoneNumber: String) {
//        // 현재 녹음 중이 아니라면 녹음을 시작
//        if (!isRecording) {
//            // 오디오 포커스 요청
//            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//            audioManager.requestAudioFocus(
//                null,
//                AudioManager.STREAM_VOICE_CALL,
//                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
//            )
//
//            // MediaRecorder 객체 생성 및 설정
//            val recorder =
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
//            mediaRecorder = recorder.apply {
//                // 마이크 입력을 소스로 설정
////                setAudioSource(MediaRecorder.AudioSource.MIC)
//                setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
////                    setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
//
//                // 출력 파일 형식을 mp3로 설정
//                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
//                // 오디오 인코더를 AMR_NB로 설정
//                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//
//                setAudioSamplingRate(44100)     // 샘플링 레이트 44100 설정
////                    setAudioEncodingBitRate(128000)   // 인코딩 비트레이트 128000 설정
//
//                // 녹음 파일 이름 생성 (시간과 전화번호를 포함하여 유니크하게 생성)
//                val fileName = "Recording_Voice_${phoneNumber}_${
//                    SimpleDateFormat(
//                        "yyyy.MM.dd_HH:mm:ss",
//                        Locale.getDefault()
//                    ).format(Date())
//                }"
//                val fileUri = createFileUri(fileName)
//                recordingFilePath =
//                    fileUri.toString() // 녹음 파일의 URI를 recordingFilePath에 저장
//
//                // MediaRecorder가 녹음할 파일 경로 설정
//                setOutputFile(getFileDescriptor(fileUri).fileDescriptor)
//                prepare()  // MediaRecorder 준비
//            }
//
//
//            mediaRecorder?.runCatching {
//                start()    // 녹음 시작
//                isRecording = true // 녹음 상태 업데이트
//                Log.i("[APP] VoiceRecording", "녹음 시작: $recordingFilePath")
//            }?.onFailure { e ->
//                // 녹음 준비 또는 시작 실패 시 예외 처리 및 로그 출력
//                Log.e("[APP] VoiceRecording", "녹음 준비 또는 시작 실패: ${e.message}")
//                return
//            }
//        }
//    }
//
//    private fun stopRecording() {
//        if (isRecording) {
//            try {
//                mediaRecorder?.run {
//                    stop()  // 녹음 중지
//                    reset() // MediaRecorder를 초기 상태로 리셋
//                    release()  // 리소스 해제
//                }
//                mediaRecorder = null // 객체 해제 후 참조 제거
//                isRecording = false
//                Log.d("[APP] VoiceRecording", "녹음 종료: $recordingFilePath")
//            } catch (e: Exception) {
//                e.printStackTrace()
//                Log.e("[APP] VoiceRecording", "녹음 종료 실패: ${e.message}")
//            }
//        }
//    }
}
