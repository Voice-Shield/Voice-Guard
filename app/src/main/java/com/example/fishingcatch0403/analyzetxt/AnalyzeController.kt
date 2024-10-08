package com.example.fishingcatch0403.analyzetxt

import android.annotation.SuppressLint
import android.util.Log
import com.example.fishingcatch0403.analyzetxt.retrofit.Message
import com.example.fishingcatch0403.analyzetxt.retrofit.OpenAIRequest
import com.example.fishingcatch0403.analyzetxt.retrofit.OpenAIResponse
import com.example.fishingcatch0403.analyzetxt.retrofit.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AnalyzeController {

    var analyzeResult: String? = null

    // 분석 결과를 전달하는 콜백 인터페이스
    interface AnalysisCallback {
        fun onAnalysisComplete(result: String)
    }

    fun analyzeText(text: String, callback: AnalysisCallback) {
        // OpenAI API 요청
        val request = OpenAIRequest(
            "gpt-3.5-turbo",
            listOf(
                Message(
                    "user",
                    "$text \n 위 내용은 통화내용인데 보이스피싱으로 의심이 된다면 '보이스 피싱이 의심됩니다' 를 의심이 안 된다면 '보이스 피싱이 아닙니다' 로 대답해주고 추가적으로 만약 보이스 피싱으로 의심된다면 어떤 단어가 몇건씩 적발되었는지 best3만 추려서 같이 깔끔하게 이 내용들만 보여줘"
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
                                    analyzeResult = getResponse
                                } else {
                                    Log.d("[APP] AI check", "보이스 피싱이 아닙니다.")
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
