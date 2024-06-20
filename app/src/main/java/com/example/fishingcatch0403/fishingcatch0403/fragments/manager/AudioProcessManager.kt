package com.example.fishingcatch0403.manager

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.annotation.RequiresApi
import java.io.File
import com.google.cloud.speech.v1.*
import java.nio.file.Files
import java.nio.file.Paths

class AudioProcessManager (val context: Context) {

    @RequiresApi(Build.VERSION_CODES.S)
    fun main() {
        // 오디오 파일 경로 및 텍스트 결과 저장 경로 설정
        val audioFilePath = context.getExternalFilesDir(Environment.DIRECTORY_RECORDINGS)!!.absolutePath + "/call.wav"
        val outputFilePath = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!.absolutePath + "/speech_results.txt"

        // 음성 인식 수행 및 결과 저장
        transcribeSpeechToText(audioFilePath, outputFilePath)
    }

    fun transcribeSpeechToText(audioFilePath: String, outputFilePath: String) {
        // SpeechClient 객체 생성
        val client = SpeechClient.create()

        // 오디오 파일 읽기
        val audioBytes = Files.readAllBytes(Paths.get(audioFilePath))

        // RecognitionConfig 설정
        val config = RecognitionConfig.newBuilder()
            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
            .setSampleRateHertz(16000)
            .setLanguageCode("ko-KR")
            .build()

        // RecognitionAudio 설정
        val audio = RecognitionAudio.newBuilder()

            .build()

        // Recognize 요청 보내기
        val response = client.recognize(config, audio)

        // 결과 처리
        val results = response.resultsList

        // 텍스트 파일에 결과 저장
        val outputFile = File(outputFilePath)
        outputFile.writeText("")

        for (result in results) {
            for (alternative in result.alternativesList) {
                outputFile.appendText(alternative.transcript + "\n")
            }
        }
    }

}