package com.example.fishingcatch0403.analyzetxt.retrofit

data class OpenAIRequest(
    val model: String,
    val messages: List<Message>
)

data class Message(
    val role: String,
    val content: String
)
