package com.example.fishingcatch0403.rest_api

import android.content.Context
import android.util.Log
import com.example.fishingcatch0403.BuildConfig
import com.example.fishingcatch0403.stt.CHANNEL_ID
import com.example.fishingcatch0403.stt.notificationId
import com.example.fishingcatch0403.stt.notificationManager
import com.example.fishingcatch0403.system_manager.ProgressBarManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.min

// STT 결과 값 출력을 위한 인터페이스
interface SttResultCallback {
    fun onSuccess(result: String)
    fun onError(errorMessage: String)
}

class ApiController() {
    private lateinit var progressBarManager: ProgressBarManager

    // 초기화 메서드
    fun initProgressBarManager(context: Context) {
        progressBarManager =
            ProgressBarManager(context, notificationManager, notificationId, CHANNEL_ID)
    }

    // Gson 컨버터
    private val gson by lazy {
        GsonConverterFactory.create()
    }

    // Retrofit 클라이언트
    private val retrofitClient by lazy {
        // 로깅 인터셉터 설정
        val interceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        // OkHttpClient 설정
        val client = OkHttpClient.Builder().apply {
            addInterceptor(interceptor)
            connectTimeout(30, TimeUnit.SECONDS)
            readTimeout(30, TimeUnit.SECONDS)
            writeTimeout(30, TimeUnit.SECONDS)
        }.build()
        // Retrofit 빌더를 사용하여 Retrofit 인스턴스 생성
        Retrofit.Builder().baseUrl(BuildConfig.INVOKE_URL).client(client)
            .addConverterFactory(gson).build()
    }

    // STT 요청
    fun getSTTResult(context: Context, audioFile: File, callback: SttResultCallback) {
        if (!::progressBarManager.isInitialized) {
            initProgressBarManager(context)
            Log.e("[APP] ApiController", "progressBarManager가 초기화되지 않았습니다.")
        }
        // STT API 서비스
        val sttService = retrofitClient.create(STTApiService::class.java)
        val convertedAudioFile = MultipartBody.Part.createFormData(
            "media",
            audioFile.name,
            audioFile.asRequestBody("audio/m4a".toMediaTypeOrNull())
        )

        // STT 요청 바디
        val sttRequest = SttRequest(
            language = "ko-KR",
            completion = "sync",
            diarization = Diarization(
                enable = true,
                speakerCountMin = 2,
                speakerCountMax = 5
            )
        )
        val params = Gson().toJson(sttRequest).toTextReqBody()

        // STT 요청
        CoroutineScope(Dispatchers.IO).launch {
            val startTime = System.currentTimeMillis() // STT 요청 시작 시간 기록

            // STT 요청
            val res = sttService.recognizeSpeech(
                audioFile = convertedAudioFile,
                params = params,
                resContentType = "application/json".toTextReqBody()
            )

            // 요청이 완료될 때까지 대기
            while (true) {
                // 경과 시간 계산
                val elapsedTime = System.currentTimeMillis() - startTime

                // 실제 소요 시간에 맞춰 ProgressBar 업데이트
                val progress =
                    (elapsedTime * 100 / (elapsedTime + 5000)).toInt() // 5000ms를 더해 소요 시간 기준으로 비율 계산
                progressBarManager.updateProgressBar(min(progress, 100))

                // STT 응답 처리
                if (res.isSuccessful) {
                    val data = res.body()
                    data?.run {
                        val res = buildString {
                            var curSpeaker = 1
                            append("${curSpeaker}번 화자: ")
                            segments?.forEach {
                                val speaker = it.speaker.label.toInt()
                                val content = it.text.trim()

                                if (speaker == curSpeaker) append("$content ")
                                else {
                                    append('\n')
                                    append("${speaker}번 화자: ")
                                    append("$content ")
                                    curSpeaker = speaker
                                }
                            }
                        }
                        // STT 결과 출력(텍스트 출력)
                        Log.d("[APP] stt", res)

                        withContext(Dispatchers.Main) {
                            // ProgressBar 업데이트 중지
                            progressBarManager.stopProgressUpdate()
                            // 실제 소요 시간으로 ProgressBar를 완료 상태로 업데이트
                            progressBarManager.updateProgressBar(100)

                            callback.onSuccess(res)
                        }
                    }
                    break
                } else {
                    val errorBody = res.errorBody()?.string() ?: "Unknown error"
                    Log.e("[APP] stt", errorBody)
                    withContext(Dispatchers.Main) {
                        progressBarManager.stopProgressUpdate()
                        progressBarManager.updateProgressBar(0)
                        callback.onError(errorBody)
                    }
                    break
                }
                // 100ms 간격으로 업데이트
                delay(100)
            }
        }
    }
}
