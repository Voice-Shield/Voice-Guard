package com.example.fishingcatch0403.rest_api

import com.example.fishingcatch0403.BuildConfig
import com.example.fishingcatch0403.system_manager.ProgressBarManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

// STT 결과 값 출력을 위한 인터페이스
interface SttResultCallback {
    fun onSuccess(result: String)
    fun onError(errorMessage: String)
}
var isSTTResultReceived = false
class ApiController() {
    private lateinit var progressBarManager: ProgressBarManager

    // ProgressBarManager 초기화 메서드
    fun initProgressBarManager(manager: ProgressBarManager) {
        progressBarManager = manager
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
    fun getSTTResult(audioFile: File, callback: SttResultCallback) {
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
            // STT 요청
            val res = sttService.recognizeSpeech(
                audioFile = convertedAudioFile,
                params = params,
                resContentType = "application/json".toTextReqBody()
            )
            // STT 응답 처리
            withContext(Dispatchers.Main) {
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
                        isSTTResultReceived = true
                        progressBarManager.updateProgressBar(100)
                        callback.onSuccess(res) // STT 내용 콜백
                    }
                } else {
                    val errorBody = res.errorBody()?.string() ?: "Unknown error"
                    progressBarManager.updateProgressBar(0)
                    callback.onError(errorBody)
                }
            }
        }
    }
}
