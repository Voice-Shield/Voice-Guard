package com.example.fishingcatch0403.rest_api

import android.util.Log
import com.example.fishingcatch0403.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

class ApiController() {

    private val gson by lazy {
        GsonConverterFactory.create()
    }

    private val retrofitClient by lazy {
        val interceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder().apply {
            addInterceptor(interceptor)
            connectTimeout(30, TimeUnit.SECONDS)
            readTimeout(30, TimeUnit.SECONDS)
            writeTimeout(30, TimeUnit.SECONDS)
        }.build()

        Retrofit.Builder().baseUrl(BuildConfig.INVOKE_URL).client(client)
            .addConverterFactory(gson).build()
    }

    fun getSTTResult(audioFile: File) {
        val sttService = retrofitClient.create(STTApiService::class.java)
        val convertedAudioFile = MultipartBody.Part.createFormData(
            "media",
            audioFile.name,
            audioFile.asRequestBody("audio/m4a".toMediaTypeOrNull())
        )

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

        CoroutineScope(Dispatchers.IO).launch {
            val res = sttService.recognizeSpeech(
                audioFile = convertedAudioFile,
                params = params,
                resContentType = "application/json".toTextReqBody()
            )

            Log.d("[APP] stt", "$params")
            if (res.isSuccessful) {
                val data = res.body()
                data?.run {
                    val res = buildString {
                        var curSpeaker = 1
                        append("${curSpeaker}번 화자: ")
                        segments?.forEach {
                            val speaker = it.speaker.label.toInt()
                            val content = it.text.trim()

                            if(speaker == curSpeaker) append("$content ")
                            else{
                                append('\n')
                                append("${speaker}번 화자: ")
                                append("$content ")
                                curSpeaker = speaker
                            }
                        }
                    }

                    Log.d("[APP] stt", "$res")
                }
            } else {
                val errorBody = res.errorBody()?.string() ?: "Unknown error"
                Log.e("[APP] stt", "${errorBody}")
            }
        }
    }
}
