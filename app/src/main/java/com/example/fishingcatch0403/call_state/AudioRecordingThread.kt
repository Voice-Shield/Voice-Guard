package com.example.fishingcatch0403.call_state

import android.Manifest
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
import java.io.IOException
import java.nio.ByteBuffer

class AudioRecordingThread(
    private val context: Context,        // 애플리케이션 컨텍스트
    private val fileUri: Uri,            // 녹음 파일의 저장 위치 URI
    private val bufferSize: Int,         // 버퍼 크기 (녹음 데이터 저장을 위한 버퍼)
    private val sampleRate: Int,         // 샘플링 레이트 (녹음 품질)
    private val audioFormat: Int,        // 오디오 포맷 (예: PCM 16비트)
) : Thread() {
    private var isRecordingThread = false   // 녹음 상태를 추적하는 변수
    private var audioRecord: AudioRecord? = null   // 오디오 녹음을 위한 AudioRecord 객체
    private var mediaCodec: MediaCodec? = null     // 오디오 인코딩을 위한 MediaCodec 객체
    private var mediaMuxer: MediaMuxer? = null     // 인코딩된 데이터를 파일에 저장하기 위한 MediaMuxer 객체

    override fun run() {
        try {
            // AudioRecord 객체 생성 (음성 통신 소스를 사용하여 설정)
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                audioFormat,
                bufferSize
            )

            // MediaCodec 설정 (AAC 인코더 사용)
            mediaCodec = MediaCodec.createEncoderByType("audio/mp4a-latm").apply {
                val format = MediaFormat.createAudioFormat("audio/mp4a-latm", sampleRate, 1)
                format.setInteger(MediaFormat.KEY_BIT_RATE, 128000)  // 비트레이트 설정
                format.setInteger(
                    MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectLC
                )  // AAC 프로파일 설정
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            }

            // MediaMuxer 설정 (MPEG-4 형식으로 파일 저장)
            mediaMuxer = MediaMuxer(
                context.contentResolver.openFileDescriptor(fileUri, "w")?.fileDescriptor
                    ?: throw IOException("Cannot open file descriptor"),
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )

            // 녹음 시작
            audioRecord?.startRecording()
            mediaCodec?.start()

            val buffer = ByteBuffer.allocate(bufferSize)  // 데이터를 저장할 버퍼
            val bufferInfo = MediaCodec.BufferInfo()  // 인코딩된 데이터 정보를 저장할 객체

            isRecordingThread = true
            while (isRecordingThread) {
                val bytesRead = audioRecord?.read(buffer, bufferSize) ?: 0  // 오디오 데이터를 읽음
                if (bytesRead > 0) {
                    // 인코딩 및 Muxing 처리
                    encodeAndMux(buffer, bytesRead, bufferInfo)
                }
            }

        } catch (e: Exception) {
            Log.e("[APP] AudioRecordingThread", "녹음 중 오류 발생: ${e.message}")
        }
    }

    // 인코딩 및 Muxing 처리 함수
    private fun encodeAndMux(
        buffer: ByteBuffer,
        bytesRead: Int,
        bufferInfo: MediaCodec.BufferInfo
    ) {
        // 입력 버퍼에 데이터를 넣고 인코딩
        val inputBufferIndex = mediaCodec?.dequeueInputBuffer(10000) ?: -1
        if (inputBufferIndex >= 0) {
            val inputBuffer = mediaCodec?.getInputBuffer(inputBufferIndex)
            inputBuffer?.clear()  // 버퍼 초기화
            inputBuffer?.put(buffer)  // 읽어온 오디오 데이터를 버퍼에 넣음
            mediaCodec?.queueInputBuffer(
                inputBufferIndex,
                0,
                bytesRead,
                System.nanoTime() / 1000,
                0
            )  // 인코딩 큐에 추가
        }

        // 인코딩된 데이터를 출력하고 Muxer에 쓰기
        var outputBufferIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 10000) ?: -1
        while (outputBufferIndex >= 0) {
            val encodedData = mediaCodec?.getOutputBuffer(outputBufferIndex)
            if (encodedData != null && bufferInfo.size > 0) {
                mediaMuxer?.writeSampleData(0, encodedData, bufferInfo)  // 인코딩된 데이터를 파일에 저장
            }
            mediaCodec?.releaseOutputBuffer(outputBufferIndex, false)  // 출력 버퍼 해제
            outputBufferIndex =
                mediaCodec?.dequeueOutputBuffer(bufferInfo, 0) ?: -1  // 다음 출력 버퍼 가져오기
        }
    }

    // 녹음 중지 함수
    fun stopRecording() {
        isRecordingThread = false
        try {
            audioRecord?.stop()  // AudioRecord 중지
            audioRecord?.release()  // AudioRecord 리소스 해제
            mediaCodec?.stop()  // MediaCodec 중지
            mediaCodec?.release()  // MediaCodec 리소스 해제
            mediaMuxer?.stop()  // MediaMuxer 중지
            mediaMuxer?.release()  // MediaMuxer 리소스 해제
        } catch (e: Exception) {
            Log.e("[APP] AudioRecordingThread", "녹음 종료 오류: ${e.message}")
        }
        audioRecord = null
        mediaCodec = null
        mediaMuxer = null
    }
}

