package com.example.fishingcatch0403.analyzetxt.retrofit

import com.example.fishingcatch0403.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val openAIService: OpenAIService by lazy {
        retrofit.create(OpenAIService::class.java)
    }
}
