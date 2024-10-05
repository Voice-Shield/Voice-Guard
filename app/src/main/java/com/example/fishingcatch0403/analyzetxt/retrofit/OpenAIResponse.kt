package com.example.fishingcatch0403.analyzetxt.retrofit

data class OpenAIResponse (
    val id: String,
    val choices: List<Choice>
)

data class Choice(
    val message: Message,
    val index: Int
)
