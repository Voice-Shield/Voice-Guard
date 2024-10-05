package com.example.fishingcatch0403.analyzetxt.retrofit

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import com.example.fishingcatch0403.BuildConfig

interface OpenAIService {
    @Headers("Authorization: Bearer ${BuildConfig.GPT_KEY}")
    @POST("v1/chat/completions")
    fun sendMessage(@Body request: OpenAIRequest): Call<OpenAIResponse>
}
