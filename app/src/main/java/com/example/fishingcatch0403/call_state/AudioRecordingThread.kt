package com.example.fishingcatch0403.call_state

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.net.Uri
import android.util.Log
import androidx.core.app.ActivityCompat

class AudioRecordingThread(
    private val context: Context, // 애플리케이션 컨텍스트
    private val fileUri: Uri, // 녹음 파일의 저장 위치 URI
    private val bufferSize: Int, // 버퍼 크기 (녹음 데이터 저장을 위한 버퍼)
    private val sampleRate: Int, // 샘플링 레이트 (녹음 품질)
    private val audioFormat: Int, // 오디오 포맷 (예: PCM 16비트)
) : Thread() {
    private var isRecordingThread = false // 녹음 상태를 추적하는 변수
    private var audioRecord: AudioRecord? = null // 오디오 녹음을 위한 AudioRecord 객체
    private var mediaCodec: MediaCodec? = null // 오디오 인코딩을 위한 MediaCodec 객체
    private var mediaMuxer: MediaMuxer? = null // 인코딩된 데이터를 파일에 저장하기 위한 MediaMuxer 객체
    private var muxerStarted = false // MediaMuxer가 시작되었는지 여부를 추적하는 플래그
    private var trackIndex = -1 // 트랙 인덱스 초기화

    @SuppressLint("Recycle")
    override fun run() {
        Log.d("[APP] AudioRecordingThread", "녹음 스레드 시작")
        runCatching {
            // 오디오 녹음 권한 체크
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e("[APP] AudioRecordingThread", "오디오 녹음을 위한 권한 획득 실패")
                return // 권한이 없으면 실행 중단
            }

            // AudioRecord 객체 초기화
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_CALL,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO, // 모노 채널
                audioFormat,
                bufferSize
            )

            // AudioRecord 초기화 확인
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("[APP] AudioRecordingThread", "AudioRecord 초기화 실패: $audioRecord")
                return // 초기화 실패 시 실행 중단
            }

            // MediaMuxer 초기화
            val outputDescriptor =
                context.contentResolver.openFileDescriptor(fileUri, "w")?.fileDescriptor
            if (outputDescriptor != null) {
                mediaMuxer =
                    MediaMuxer(outputDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            } else {
                Log.e("[APP] AudioRecordingThread", "파일 디스크립터 열기 실패")
                return // 파일 디스크립터 열기 실패 시 실행 중단
            }

            // MediaCodec 초기화
            mediaCodec = MediaCodec.createEncoderByType("audio/mp4a-latm").apply {
                // 인코딩 포맷 설정
                val format = MediaFormat.createAudioFormat("audio/mp4a-latm", sampleRate, 1)
                format.setInteger(MediaFormat.KEY_BIT_RATE, 128000) // 비트 전송률 설정
                format.setInteger(
                    MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectLC
                )
                // MediaCodec 구성
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            }

            // MediaCodec 및 AudioRecord 시작
            mediaCodec?.start() // 인코더 시작
            audioRecord?.startRecording() // 녹음 시작

            Log.i("[APP] AudioRecordingThread", "녹음 시작")

            val audioBuffer = ByteArray(bufferSize) // 오디오 데이터를 저장할 버퍼
            val bufferInfo = MediaCodec.BufferInfo() // MediaCodec 출력 버퍼 정보를 저장할 객체

            isRecordingThread = true // 녹음 스레드 루프 시작
            // 녹음 스레드 루프
            while (isRecordingThread) {
                // 오디오 데이터 읽기
                val bytesRead = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
                if (bytesRead > 0) {
                    // 입력 버퍼 처리
                    val inputBufferIndex = mediaCodec?.dequeueInputBuffer(10000) ?: -1
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = mediaCodec?.getInputBuffer(inputBufferIndex)
                        inputBuffer?.clear() // 버퍼 초기화
                        // bytesRead가 inputBuffer의 크기를 초과하지 않도록 처리
                        val dataToCopy = bytesRead.coerceAtMost(inputBuffer?.remaining() ?: 0)
                        inputBuffer?.put(audioBuffer, 0, dataToCopy) // 오디오 데이터를 입력 버퍼에 추가

                        // 인코딩 큐에 추가
                        mediaCodec?.queueInputBuffer(
                            inputBufferIndex,
                            0,
                            dataToCopy,
                            System.nanoTime() / 1000, // 타임스탬프 추가
                            0
                        )
                    }

                    // OutputBuffer 처리
                    var outputBufferIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 10000) ?: -1
                    while (outputBufferIndex >= 0) {
                        // Muxer에 트랙 추가 및 시작
                        if (!muxerStarted) {
                            val newFormat = mediaCodec?.outputFormat
                            trackIndex = newFormat?.let { mediaMuxer?.addTrack(it) } ?: -1
                            if (newFormat != null) {
                                trackIndex = mediaMuxer?.addTrack(newFormat) ?: -1
                                if (trackIndex >= 0) {
                                    mediaMuxer?.start() // Muxer 시작
                                    muxerStarted = true // Muxer가 시작되었음을 기록
                                    Log.i("[APP] AudioRecordingThread", "Muxer 시작됨")
                                } else {
                                    Log.e("[APP] AudioRecordingThread", "MediaMuxer에 트랙 추가 실패")
                                    return // 오류 발생 시 실행 중단
                                }
                            }
                        }

                        // 인코딩된 데이터 Muxer에 쓰기
                        if (bufferInfo.size > 0 && muxerStarted) {
                            val encodedData = mediaCodec?.getOutputBuffer(outputBufferIndex)
                            if (encodedData != null) {
                                mediaMuxer?.writeSampleData(trackIndex, encodedData, bufferInfo) // 인코딩된 샘플 데이터 작성
                            }
                            mediaCodec?.releaseOutputBuffer(outputBufferIndex, false) // 출력 버퍼 해제
                        }
                        outputBufferIndex =
                            mediaCodec?.dequeueOutputBuffer(bufferInfo, 0) ?: -1 // 다음 출력 버퍼 가져오기
                    }
                }
            }
        }.onFailure { e -> // 오류 처리
            Log.e("[APP] AudioRecordingThread", "녹음 중 오류 발생: ${e.message}", e) // 예외 메시지 및 스택 추적 로깅
        }
    }

    // 녹음 중지 메서드
    fun stopRecording() {
        isRecordingThread = false // 녹음 스레드 루프 중지 플래그 설정

        runCatching {
            // AudioRecord가 녹음 중인 경우 안전하게 중지
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop() // AudioRecord 중지
                }
                it.release() // AudioRecord 리소스 해제
            }
        }.onFailure { e ->
            Log.e("[APP] AudioRecordingThread", "AudioRecord 중지 중 오류 발생: ${e.message}", e) // 오류 로깅
        }

        runCatching {
            // MediaCodec이 실행 중인 경우 안전하게 중지
            mediaCodec?.let {
                it.stop() // MediaCodec 중지
                it.release() // MediaCodec 리소스 해제
            }
        }.onFailure { e ->
            Log.e("[APP] AudioRecordingThread", "MediaCodec 중지 중 오류 발생: ${e.message}", e) // 오류 로깅
        }

        runCatching {
            // 최소한 하나의 샘플이 작성되었는지 확인
            if (muxerStarted) {
                mediaMuxer?.stop() // MediaMuxer 중지
                Log.i("[APP] AudioRecordingThread", "MediaMuxer가 성공적으로 중지됨")
            } else {
                Log.w("[APP] AudioRecordingThread", "MediaMuxer가 시작되지 않았거나 이미 중지됨")
            }
        }.onFailure { e ->
            Log.e("[APP] AudioRecordingThread", "MediaMuxer 중지 중 오류 발생: ${e.message}", e) // 오류 로깅
        }

        // 메모리 누수를 방지하기 위해 객체 정리
        audioRecord = null
        mediaCodec = null
        mediaMuxer = null
        trackIndex = -1
    }
}
