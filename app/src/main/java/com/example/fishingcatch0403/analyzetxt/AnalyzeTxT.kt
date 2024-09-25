package com.example.fishingcatch0403.analyzetxt

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

class AnalyzeTxT(private val context: Context) {
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
        callback: (String?, Throwable?) -> Unit // 콜백 함수
    ) {  // 텍스트를 OpenAI ChatGPT API로 전송하는 함수
        val url =
            "https://api.openai.com/v1/completions" // ChatGPT API 엔드포인트 URL
        val json = JSONObject().apply { // JSON 객체를 생성합니다.
            put("model", "text-davinci-003") // 'model' 키로 'text-davinci-003'을 추가합니다.
            put(
                "prompt",
                "다음 텍스트에서 보이스피싱 시도를 분석하고 의심스러운 부분을 자세히 설명해주세요: \n\n$text"
            ) // 'prompt' 키로 텍스트를 추가합니다.
            put("max_tokens", 200)  // 'max_tokens' 키로 200을 추가합니다.(응답 길이)
            put("temperature", 0.5) // 'temperature' 키로 0.5를 추가합니다.(다양성)
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
                callback(null,e)  // 콜백 함수를 호출합니다.
            }

            override fun onResponse(call: Call, response: Response) {   // 요청 성공 시 호출되는 메소드
                response.use {  // Response 객체를 사용합니다.
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")  // 응답이 성공적이지 않으면 예외를 발생시킵니다.

                    val responseData = response.body?.string()  // 응답 데이터를 문자열로 변환합니다.
                    callback(responseData, null)  // 콜백 함수를 호출합니다.
                }
            }
        })
    }

    // ChatGPT API로부터 받은 응답을 분석하여 보이스피싱 여부를 판단하는 함수
    fun analyzeResponse(response: String?, callback: (Boolean, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = try {
                val isPhishing = response?.let {
                    extractPhishingAnalysis(it)
                } ?: false
                Pair(isPhishing, response)
            } catch (e: Exception) {
                Pair(false, "분석 중 오류 발생: ${e.message}")
            }

            withContext(Dispatchers.Main) {
                callback(result.first, result.second)
            }
        }
    }

    private fun extractPhishingAnalysis(response: String): Boolean {
        val jsonResponse = JSONObject(response) // JSON 문자열을 JSONObject로 변환합니다.
        val choices = jsonResponse.optJSONArray("choices") // 'choices' 키로 JSONArray를 가져옵니다.
        if (choices != null && choices.length() > 0) {
            val text = choices.getJSONObject(0).optString("text") // 'text' 키로 문자열을 가져옵니다.
            val phishingKeywords = loadPhishingKeywords() // 보이스피싱 키워드 목록을 불러옵니다.

            return phishingKeywords.any { keyword -> text.contains(keyword) } // 보이스피싱 키워드가 포함되어 있는지 확인합니다.
        }
        return false
    }

    private fun loadPhishingKeywords(): List<String> {
        val resourceId =
            context.resources.getIdentifier("phishing_keywords", "raw", context.packageName)
        return if (resourceId != 0) {
            context.resources.openRawResource(resourceId).bufferedReader().useLines { lines ->
                lines.toList()
            }
        } else {
            emptyList()
        }
    }
}
