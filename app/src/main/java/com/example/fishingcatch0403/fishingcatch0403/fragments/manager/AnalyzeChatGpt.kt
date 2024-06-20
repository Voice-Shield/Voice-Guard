package com.example.fishingcatch0403.fishingcatch0403.fragments.manager

import android.os.Environment
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException

class AnalyzeChatGpt {
    private val client = OkHttpClient() // OkHttpClient 객체를 생성합니다.

    fun readTextFile(filePath: String): String { // 텍스트 파일을 읽어오는 함수
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            filePath
        ) // 파일 경로를 설정합니다.
        return try {
            file.readText() // 파일을 읽어 텍스트로 반환합니다.
        } catch (e: IOException) {
            e.printStackTrace() // 오류 메시지를 출력합니다.
            "파일을 찾지 못 하거나 읽을 수 없습니다."
        }
    }

    fun sendTextToChatGPT(
        apiKey: String, // OpenAI ChatGPT API 키
        text: String,   // 전송할 텍스트
        callback: (String?) -> Unit // 콜백 함수
    ) {  // 텍스트를 OpenAI ChatGPT API로 전송하는 함수
        val url =
            "https://api.openai.com/v1/engines/davinci-codex/completions" // ChatGPT API 엔드포인트 URL
        val json = JSONObject().apply { // JSON 객체를 생성합니다.
            put("prompt", text) // 텍스트를 'prompt' 키로 추가합니다.
            put("max_tokens", 100)  // 'max_tokens' 키로 100을 추가합니다.
        }.toString()    // JSON 객체를 문자열로 변환합니다.

        val body = json // JSON 문자열을 RequestBody로 변환합니다.
            .toRequestBody("application/json; charset=utf-8".toMediaType()) // JSON 형식으로 요청을 보냅니다.
        val request = Request.Builder().url(url)    // Request 객체를 생성합니다.
            .header("Authorization", "Bearer $apiKey")  // API 키를 헤더에 추가합니다.
            .post(body) // POST 요청을 설정합니다.
            .build()    // Request 객체를 생성합니다.

        client.newCall(request).enqueue(object : Callback { // 비동기적으로 요청을 보냅니다.
            override fun onFailure(call: Call, e: IOException) {    // 요청 실패 시 호출되는 메소드
                e.printStackTrace() // 오류 메시지를 출력합니다.
                callback(null)  // 콜백 함수를 호출합니다.
            }

            override fun onResponse(call: Call, response: Response) {   // 요청 성공 시 호출되는 메소드
                response.use {  // Response 객체를 사용합니다.
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")  // 응답이 성공적이지 않으면 예외를 발생시킵니다.

                    val responseData = response.body?.string()  // 응답 데이터를 문자열로 변환합니다.
                    callback(responseData)  // 콜백 함수를 호출합니다.
                }
            }
        })
    }

    // ChatGPT API로부터 받은 응답을 분석하여 보이스피싱 여부를 판단하는 함수
    fun analyzeResponse(response: String?): Boolean {
        // 응답을 분석하여 보이스피싱 여부 판단
        response?.let {
            if (it.contains("보이스피싱")) { // 응답에 '보이스피싱'이 포함되어 있는 경우
                return true // 보이스피싱인 경우
            }
        }
        return false    // 보이스피싱이 아닌 경우
    }
}
