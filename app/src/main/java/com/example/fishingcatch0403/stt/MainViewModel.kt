package com.example.fishingcatch0403.stt

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fishingcatch0403.rest_api.ApiController
import com.example.fishingcatch0403.rest_api.SttResultCallback
import com.example.fishingcatch0403.system_manager.FileUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val apiController = ApiController() // ApiController 초기화

// M4A 파일 모델을 나타내는 데이터 클래스
data class M4AFileModel(val fileName: String, val filePath: String)

// STT 분석 결과를 나타내는 데이터 클래스
data class AnalyzedResult(
    val answer: String
)

// UI 상태를 나타내는 sealed class
sealed class State<out T> {
    data object Loading : State<Nothing>()
    data class Error(val throwable: Throwable?) : State<Nothing>()
    data class Success<T>(val result: T) : State<T>()
}

class MainViewModel(context: Context) : ViewModel() {

    @SuppressLint("StaticFieldLeak")
    private val context = context.applicationContext    // 애플리케이션 컨텍스트에 대한 참조를 유지
    private val fileUtil = FileUtil(context.contentResolver)    // 파일 유틸리티 객체 생성

    // UI 상태를 나타내는 StateFlow
    private val _m4aFileState = MutableStateFlow<State<List<M4AFileModel>>>(State.Loading)
    val m4aFileState get() = _m4aFileState.asStateFlow()

    // STT 분석 결과를 나타내는 StateFlow
    private val _transcriptState = MutableStateFlow<State<AnalyzedResult>>(State.Loading)
    val transcriptState = _transcriptState.asStateFlow()

    // M4A 파일 목록을 불러오는 함수
    fun loadM4AFiles() {
        _m4aFileState.value = State.Loading
        viewModelScope.launch {
            runCatching {
                val m4aFolderPath =
                    Environment.getExternalStorageDirectory().absolutePath + "/Recordings/Call"
                val m4aFiles =
                    File(m4aFolderPath).listFiles()?.filter { it.extension == "m4a" } ?: emptyList()

                if (m4aFiles.isEmpty()) throw Exception("M4A 파일이 없습니다.")   // 파일이 없을 경우 예외 발생

                // M4A 파일 모델 리스트 생성
                m4aFiles.map { file ->
                    M4AFileModel(fileName = file.name, filePath = file.absolutePath)
                }
            }.onSuccess { files ->
                _m4aFileState.value = State.Success(files)  // 성공 시 파일 리스트 전달
            }.onFailure { exception ->
                _m4aFileState.value = State.Error(exception) // 실패 시 예외 전달
            }
        }
    }

    // STT 요청을 위한 메서드
    fun startSTT(filePath: String, callback: SttResultCallback) {
        val audioFile = File(filePath) // M4A 파일 객체 생성
        // 현재 시간 및 날짜 형식 설정
        val currentDateTime =
            SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date())
        apiController.getSTTResult(audioFile, object : SttResultCallback {
            override fun onSuccess(result: String) {
                // STT 성공 시 파일로 저장
                val fileName = "통화 내용_$currentDateTime.txt" // 파일 이름 설정
                val uri = saveSTTResultToFile(fileName, result) // STT 결과 저장
                if (uri != null) {
                    callback.onSuccess(
                        "STT 결과가 ${
                            fileUtil.getRealPathFromUri(
                                context,
                                uri
                            )
                        }에 저장되었습니다."
                    ) // 성공 메시지 전달
                } else {
                    callback.onError("STT 결과 저장 실패.") // 저장 실패 시 에러 메시지 전달
                }
            }

            override fun onError(errorMessage: String) {
                // STT 실패 시 처리
                callback.onError(errorMessage) // 에러 메시지 전달
            }
        })
    }

    // STT 결과를 텍스트 파일로 저장하는 메서드
    fun saveSTTResultToFile(fileName: String, result: String): Uri? {
        return fileUtil.saveSTTResultToFile(result, fileName) // STT 결과를 파일로 저장
    }
}
