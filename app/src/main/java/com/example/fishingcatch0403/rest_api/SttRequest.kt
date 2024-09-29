package com.example.fishingcatch0403.rest_api

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

data class SttRequest(
    val language: String,
    val completion: String,
    val diarization: Diarization,
)

data class Diarization(
    val enable: Boolean,
    val speakerCountMin: Int,
    val speakerCountMax: Int
)


fun String.toTextReqBody() = toRequestBody("text/plain".toMediaTypeOrNull())
