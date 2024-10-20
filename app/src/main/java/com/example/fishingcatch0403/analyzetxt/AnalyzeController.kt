package com.example.fishingcatch0403.analyzetxt

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.example.fishingcatch0403.R
import com.example.fishingcatch0403.analyzetxt.retrofit.Message
import com.example.fishingcatch0403.analyzetxt.retrofit.OpenAIRequest
import com.example.fishingcatch0403.analyzetxt.retrofit.OpenAIResponse
import com.example.fishingcatch0403.analyzetxt.retrofit.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AnalyzeController {

    var analyzeResult: String? = null   // 분석 결과를 저장하는 변수
    var resultCheck: Int? = null    // 분석 결과의 유형을 저장하는 변수

    // 질문을 가져오는 함수
    private fun getQuestion(context: Context): String {
        return context.getString(R.string.question)
    }

    // 분석 결과를 전달하는 콜백 인터페이스
    interface AnalysisCallback {
        fun onAnalysisComplete(result: String)
    }

    // 텍스트 분석 함수
    fun analyzeText(context: Context, text: String, callback: AnalysisCallback) {
        val question: String = getQuestion(context)
        // OpenAI API 요청
        val request = OpenAIRequest(
            "gpt-3.5-turbo",
            listOf(
                Message(
                    "user",
                    "$text \n $question"
                )
            )
        )
        Log.d("[APP] ChatGPT", "요청: $request")

        // API 호출
        RetrofitClient.openAIService.sendMessage(request)
            .enqueue(object : Callback<OpenAIResponse> {
                @SuppressLint("SetTextI18n")
                override fun onResponse(
                    call: Call<OpenAIResponse>,
                    response: Response<OpenAIResponse>
                ) {
                    if (response.isSuccessful) {
                        val resBody = response.body()

                        if (resBody != null) {
                            val getResponse =
                                resBody.choices.firstOrNull()?.message?.content
                            if (getResponse != null) {
                                Log.d("[APP] ChatGPT", "응답: $getResponse ")

                                if (getResponse.contains("의심됩니다")) {
                                    Log.d("[APP] AI check", "보이스 피싱이 의심됩니다.")
                                    resultCheck = 1
                                    analyzeResult = getResponse
                                } else {
                                    Log.d("[APP] AI check", "보이스 피싱이 아닙니다.")
                                    resultCheck = 0
                                    analyzeResult = getResponse
                                }
                                // 분석 결과가 완료되면 콜백 호출
                                callback.onAnalysisComplete(analyzeResult!!)
                            } else {
                                Log.e("[APP] ChatGPT", "응답 본문이 없습니다.")
                                analyzeResult = "No response content"
                            }
                            // 분석 결과가 완료되면 콜백 호출
                            callback.onAnalysisComplete(analyzeResult!!)
                        } else {
                            Log.e("[APP] ChatGPT", "응답이 null 입니다.")
                            analyzeResult = "Null response"
                            // 분석 결과가 완료되면 콜백 호출
                            callback.onAnalysisComplete(analyzeResult!!)
                        }
                    } else {
                        Log.e(
                            "[APP] ChatGPT",
                            "오류: ${response.code()}, \n메시지: ${response.message()}, \n오류 내용: ${
                                response.errorBody()?.string()
                            }"
                        )
                        analyzeResult = "Error: ${response.code()}"
                        // 분석 결과가 완료되면 콜백 호출
                        callback.onAnalysisComplete(analyzeResult!!)
                    }
                }

                @SuppressLint("SetTextI18n")
                override fun onFailure(call: Call<OpenAIResponse>, t: Throwable) {
                    Log.e("[APP] ChatGPT", "실패: ${t.message}")
                    analyzeResult = "Failure: ${t.message}"
                    // 분석 결과가 완료되면 콜백 호출
                    callback.onAnalysisComplete(analyzeResult!!)
                }
            })
    }
}
