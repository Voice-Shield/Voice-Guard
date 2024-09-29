package com.example.fishingcatch0403.rest_api

import com.example.fishingcatch0403.BuildConfig
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface STTApiService {
    @Multipart
    @Headers("X-CLOVASPEECH-API-KEY: ${BuildConfig.SECRET_KEY}")
    @POST("recognizer/upload")
    suspend fun recognizeSpeech(
        @Part audioFile: MultipartBody.Part,
        @Part("params") params: RequestBody,
        @Part("type") resContentType: RequestBody
    ): Response<SttResponse>
}
