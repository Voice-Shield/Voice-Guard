package com.example.fishingcatch0403.stt

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fishingcatch0403.BuildConfig
import com.example.fishingcatch0403.system_manager.FileUtil
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Clova Speech API 비밀 키 및 호출 URL
private const val SECRET = BuildConfig.SECRET_KEY
private const val INVOKE_URL = BuildConfig.INVOKE_URL

// HTTP 클라이언트 및 Gson 인스턴스 초기화
private val httpClient: CloseableHttpClient = HttpClients.createDefault()
private val gson: Gson = Gson()

class STTViewModel : ViewModel() {

    // STT 결과를 관리하기 위한 LiveData 변수
    private val _speechResult = MutableLiveData<String>()
    val speechResult: LiveData<String> get() = _speechResult

    // STT 진행 상황을 관리하기 위한 LiveData 변수
    private val _progressLiveData = MutableLiveData<Int>()
    val progressLiveData: LiveData<Int> get() = _progressLiveData

    // FileUtil 인스턴스 초기화
    private lateinit var fileUtil: FileUtil

    // FileUtil 인스턴스를 설정하는 메서드
    fun setFileUtil(contentResolver: ContentResolver) {
        this.fileUtil = FileUtil(contentResolver) // ContentResolver를 사용하여 FileUtil 초기화
    }

    // 요청 바디 클래스
    data class NestRequestEntity(
        var language: String = "ko-KR", // 언어 설정
        var completion: String = "sync", // 완료 방식
        var callback: String? = null, // 콜백 URL
        var wordAlignment: Boolean = true, // 단어 정렬 여부
        var fullText: Boolean = true, // 전체 텍스트 여부
        var forbiddens: String? = null, // 금지 단어
        var boostings: List<Boosting>? = null, // 강조 단어 리스트
        var diarization: Diarization? = null, // 발화자 구분 설정
        var sed: Sed? = null // Sed 설정
    ) {
        data class Boosting(var words: String) // 강조 단어 클래스
        data class Diarization(
            var enable: Boolean = false, // 발화자 구분 활성화 여부
            var speakerCountMin: Int? = null, // 최소 발화자 수
            var speakerCountMax: Int? = null // 최대 발화자 수
        )

        data class Sed(var enable: Boolean = false) // Sed 클래스
    }

    // 음성 인식 요청 메서드
    fun recognizeSpeech(context: Context, speakerCount: Int, phoneNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                _progressLiveData.postValue(0) // 초기화

                // 1. 녹음 파일 경로 가져오기 (10%)
                val audioFilePath = fileUtil.getLatestRecordingFile()?.absolutePath ?: return@launch
                _progressLiveData.postValue(10) // 10% 진행 상태

                // 2. STT 파일 이름 및 경로 설정 (20%)
                val fileName = "STT_${speakerCount}_${phoneNumber}_${
                    SimpleDateFormat("yyyy.MM.dd_HH:mm:ss", Locale.getDefault()).format(Date())
                }.txt"
                val fileUri = fileUtil.createFileUri(fileName) // STT 파일 URI 생성
                val txtFilePath = fileUtil.getRealPathFromUri(context, fileUri) // STT 파일 경로 저장
                _progressLiveData.postValue(20) // 20% 진행 상태

                // 3. STT 요청 실행 (40%)
                val nestRequestEntity = NestRequestEntity(
                    diarization = NestRequestEntity.Diarization(
                        enable = true, // 화자 구분 활성화
                        speakerCountMin = speakerCount.coerceAtLeast(2), // 최소 화자 수
                        speakerCountMax = speakerCount.coerceAtMost(5) // 최대 화자 수
                    )
                )
                _progressLiveData.postValue(30) // STT 요청 전 30%

                // 파일 업로드 및 STT 처리 (파일 크기에 비례하여 진행률을 계산할 수 있음)
                val result = upload(File(audioFilePath), nestRequestEntity)
                _progressLiveData.postValue(70) // STT 처리 완료 후 70%

                // 4. STT 결과를 텍스트 파일로 저장 (90%)
                txtFilePath?.let { path ->
                    saveTextToFile(result, path, fileUri)
                } ?: throw Exception("텍스트 파일 경로를 가져올 수 없습니다.")
                _progressLiveData.postValue(90) // 파일 저장 완료 후 90%

                // 5. 결과 완료 (100%)
                withContext(Dispatchers.Main) {
                    _speechResult.value = result
                    _progressLiveData.postValue(100) // 최종 완료
                }
            }.onFailure { e ->
                // 오류 처리
                withContext(Dispatchers.Main) {
                    _speechResult.value = "오류 발생: ${e.message}"
                }
            }
        }
    }


    // 미디어 파일을 로컬에서 업로드하여 음성 인식 요청 메서드
    private fun upload(file: File, nestRequestEntity: NestRequestEntity): String {
        Log.d("[APP] STTViewModel", "음성 인식 요청 업로드 시작") // 업로드 시작 로그
        val httpPost = HttpPost("$INVOKE_URL/recognizer/upload")
        val httpEntity = MultipartEntityBuilder.create()
            .addTextBody("params", gson.toJson(nestRequestEntity), ContentType.APPLICATION_JSON)
            .addBinaryBody("media", file, ContentType.MULTIPART_FORM_DATA, file.name)
            .build()
        httpPost.entity = httpEntity
        Log.d("[APP] STTViewModel", "음성 인식 요청 업로드 완료") // 업로드 완료 로그
        return execute(httpPost) // HTTP 요청 실행
    }

    // HTTP 요청 실행 메서드
    private fun execute(httpPost: HttpPost): String {
        httpPost.setHeader("Accept", "application/json")
        httpPost.setHeader("X-CLOVASPEECH-API-KEY", SECRET) // API 키 설정
        httpPost.setHeader("Content-Type", "application/json")
        httpPost.setHeader("charset", "UTF-8")

        Log.d("[APP] STTViewModel", "HTTP 요청 실행") // HTTP 요청 실행 로그
        httpClient.use { client ->
            val response: CloseableHttpResponse = client.execute(httpPost)
            val entity: HttpEntity? = response.entity
            val result = EntityUtils.toString(entity, StandardCharsets.UTF_8)

            if (response.statusLine.statusCode != 200) {
                // HTTP 요청 실패 시 처리
                val errorMessage =
                    "HTTP 요청 실패: ${response.statusLine.statusCode} - ${result.trim()}"
                Log.e("[APP] STTViewModel", errorMessage) // 실패 로그
                throw IOException(errorMessage) // IOException을 발생시켜 상위 catch로 전달
            }

            Log.d("[APP] STTViewModel", "HTTP 요청 실행 완료") // HTTP 요청 실행 완료 로그
            return result // 결과 반환
        }
    }

    // STT 결과를 텍스트 파일로 저장하는 메서드
    private fun saveTextToFile(text: String, txtFilePath: String, fileUri: Uri) {
        // 파일 디스크립터 설정
        val fileDescriptor = fileUtil.getFileDescriptor(fileUri).fileDescriptor // STT 파일 디스크립터 가져오기

        // ParcelFileDescriptor를 사용하여 파일에 텍스트 저장
        FileOutputStream(fileDescriptor).use { outputStream ->
            outputStream.write(text.toByteArray(Charsets.UTF_8)) // 텍스트를 바이트 배열로 변환하여 저장
            Log.d("[APP] STTViewModel", "STT 결과를 파일에 저장 완료: $txtFilePath") // 파일 저장 완료 로그
        }
    }
}
