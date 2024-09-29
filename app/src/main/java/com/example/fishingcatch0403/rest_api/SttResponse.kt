package com.example.fishingcatch0403.rest_api

data class SttResponse(
    val result: String?,
    val message: String?,
    val token: String?,
    val version: String?,
    val params: ResponseParams?,
    val progress: Int,
    val keywords: Map<String, Any>?,
    val segments: List<Segment>?
    )

data class ResponseParams(
    val service: String,
    val domain: String,
    val lang: String,
    val completion: String,
    val callback: String,
    val diarization: Diarization,
    val sed: Sed,
    val boostings: Array<String>,
    val forbiddens: String,
    val wordAligment: Boolean,
    val fullText: Boolean,
    val noiseFiltering: Boolean,
    val resultToObs: Boolean,
    val priority: Int,
    val userData: UserData
)

data class Sed(
    val enable: Boolean
)

data class UserData(
    val _ncp_DomainCode: String,
    val _ncp_DomainId: Int,
    val _ncp_TaskId: Int,
    val _ncp_TraceId: String,
    val id: Int
)

data class Segment(
    val start: Int,
    val end: Int,
    val text: String,
    val confidence: Double,
    val diarization: DiarizationRes,
    val speaker: Speaker,
    val words: List<List<Any>>,
    val textEdited: String
)

data class DiarizationRes(
    val label: String
)

data class Speaker(
    val label: String,
    val name: String,
    val edited: Boolean
)

