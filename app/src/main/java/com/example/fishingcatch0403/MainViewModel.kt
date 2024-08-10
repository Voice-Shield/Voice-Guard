package com.example.fishingcatch0403

import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1.RecognitionAudio
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechSettings
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.measureTime

//data class MonoAudio(
//    val filePath:String,
//    va
//)


data class WavModel(val fileName: String, val filePath: String)
data class AnalyzedResult(
//    val warnWords: HashMap<String, Int> //(단어, 발견 빈도)
    val answer: String
)


sealed class State<out T> {
    object Loading : State<Nothing>()
    data class Error(val throwable: Throwable?) : State<Nothing>()
    data class Success<T>(val result: T) : State<T>()
}


class MainViewModel : ViewModel() {
    private lateinit var credentials: GoogleCredentials
    private val _recordState = MutableStateFlow<State<List<WavModel>>>(State.Loading)
    val recordState get() = _recordState.asStateFlow()

    //모노 오디오 파일로 변환 후, STT 과정을 거쳐, 분석까지 이뤄지는 상황에서의 진행 상태
    private val _transcriptState = MutableStateFlow<State<AnalyzedResult>>(State.Loading)
    val transcriptState get() = _transcriptState.asStateFlow()


    fun setCredentials(credentials: GoogleCredentials) {
        this.credentials = credentials
    }

    fun loadRecordings() {
        viewModelScope.launch {
            runCatching {
                (Environment.getExternalStorageDirectory().absolutePath + "/Recordings/Call").let { ringtonesFolderPath ->
                    val m4aFiles =
                        File(ringtonesFolderPath).listFiles().filter { it.extension == "m4a" }
                    if (m4aFiles.isNullOrEmpty()) throw Exception("녹음 파일이 없습니다.")
                    val convertedFiles = mutableListOf<WavModel>()
                    withContext(Dispatchers.IO) {
                        m4aFiles.forEach { m4aFile ->   // m4a 파일을 하나씩 처리
                            val wavModel =
                                ("${m4aFile.nameWithoutExtension}.wav").let { wavFileName ->
                                    WavModel(
                                        fileName = wavFileName,
                                        filePath = "${m4aFile.parent}/${wavFileName}"
                                    )
                                }
                            if (File(wavModel.filePath).exists().not()) {
                                if (wavModel.fromM4a(m4aFile.path)) convertedFiles.add(wavModel)
                                else throw Exception("파일 변환에 실패했습니다.")
                            } else convertedFiles.add(wavModel)
                        }
                        _recordState.value = State.Success(convertedFiles)
                    }
                }
            }.onFailure {
                _recordState.value = State.Error(it)
            }
        }
    }

    private suspend fun convertM4aToWav(inputPath: String, outputPath: String): Boolean {
        // FFmpeg를 사용하여 m4a 파일을 wav 파일로 변환
        return withContext(Dispatchers.IO) {
            // FFmpeg 명령어를 사용하여 m4a 파일을 wav 파일로 변환
            val command = arrayOf(
                "-i", inputPath, "-acodec", "pcm_s16le", "-ar", "44100", "-ac", "2", outputPath
            )
            val executionResult = FFmpeg.execute(command)   // FFmpeg 명령어 실행
            if (executionResult != 0) { // 명령어 실행 실패 시 로그 출력
                val log = Config.getLastCommandOutput()
                Log.e("FFmpeg", "커맨드 입력 실패: $log")
            }
            executionResult == 0 // 0이면 성공, 그 외는 실패
        }
    }

    private fun WavModel.fromM4a(m4aFilePath: String): Boolean {
        val command = arrayOf(
            "-i", m4aFilePath, "-acodec", "pcm_s16le", "-ar", "44100", "-ac", "2", this.filePath
        )
        // FFmpeg 명령어  실행
        return (FFmpeg.execute(command) == 0).also {
            if (it.not()) {
                val log = Config.getLastCommandOutput()
                Log.e("FFmpeg", "커맨드 입력 실패: $log")
            }
        }
    }

    //UI 단에서 녹음 파일을 선택한 경우, 녹음 파일을 분석
    fun analyzeRecordedFile(wavFilePath: String, outputFilePath: String) {
        _transcriptState.value = State.Loading
        viewModelScope.launch {
           val elapsedTime =  measureTime {
                val command = arrayOf("-i", wavFilePath, "-ac", "1", outputFilePath)  // FFmpeg 명령어
                // 출력 파일이 이미 존재할 경우 삭제
                val outputFile = File(outputFilePath)
                if (outputFile.exists()) {
                    outputFile.delete()
                }
                // FFmpeg 명령어 실행
                val result = suspendCoroutine { cont ->
                    FFmpeg.executeAsync(command) { _: Long, returnCode: Int ->
                        cont.resume(
                            returnCode
                        )
                    }
                }
                // 모노 파일 변환 결과에 따른 진행 분기
                if (result == 0) { //정상적으로 변환된 경우
                    transcriptMonoFile(monoFilePath = outputFilePath).onFailure {
                        _transcriptState.value = State.Error(it)
                    }.onSuccess { transcript ->
                        val saveResult = saveTranscriptionToFile(transcript)
                        saveResult.onFailure {
                            _transcriptState.value = State.Error(Exception("파일 저장 중에 에러가 발생했습니다. "))
                        }.onSuccess {
                            _transcriptState.value = State.Success(AnalyzedResult(it))
                        }
                    }
                } else _transcriptState.value = State.Error(Exception("오디오 변환 실패"))
            }
            Log.d("Elapsed Time to analyze file: ","${elapsedTime.inWholeSeconds}s")
        }
    }

    private suspend fun transcriptMonoFile(monoFilePath: String): Result<String> =
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                val segmentedMonoAudios = segmentAudio(monoFilePath)
                if (segmentedMonoAudios == null) throw Exception("모노 오디오로 변환 중에 에러가 발생했습니다.")
                else {
                    val sortedAudios = segmentedMonoAudios.listFiles().sortedWith { o1, o2 ->
                        o1.nameWithoutExtension.split("_")[1].toInt()
                            .compareTo(o2.nameWithoutExtension.split("_")[1].toInt())
                    }
                    val res = sortedAudios.map { audio ->
                        val settings = SpeechSettings.newBuilder()
                            .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                            .build()
                        SpeechClient.create(settings).use { speechClient ->
                            try {
                                val audioBytes = ByteString.copyFrom(audio.readBytes())
                                val config = RecognitionConfig.newBuilder()
                                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                                    .setSampleRateHertz(44100).setLanguageCode("ko-KR").build()
                                val audio =
                                    RecognitionAudio.newBuilder().setContent(audioBytes).build()
                                val response = speechClient.recognize(config, audio)
                                val results =
                                    response.resultsList.joinToString("") { it.alternativesList[0].transcript }
                                "$results"
                            } catch (e: Exception) {
                                throw e
                                Log.e("Error On Transcription: ", e.message.toString())
                                ""
                            }
                        }
                    }

                    segmentedMonoAudios.delete() // 변환된 모노 오디오 파일 들을 삭제
                    res.joinToString("") { it }
                }
            }
        }


    private suspend fun segmentAudio(audioFilePath: String): File? = withContext(Dispatchers.IO) {
        val audio = File(audioFilePath)
        val tmp = File(audio.parent + "/tmp").also { it.mkdirs() }
        val commandForSegment = arrayOf(
            "-i",
            audioFilePath, //원본 오디오 파일 위치
            "-f",
            "segment",
            "-segment_time",
            "59", //분할 기준 시간(단위 : 초)
            "${tmp.path}/segmented_%03d.wav" // 분할된 오디오 파일의 네이밍
        )
        val std = FFmpeg.execute(commandForSegment).also {
            it //추가 로깅 필요시 이용하기
        } == 0



        return@withContext if (std) tmp else null
    }


    private suspend fun saveTranscriptionToFile(transcription: String) =
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                // 현재 시간과 날짜를 포함하는 파일 이름을 생성합니다.
                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val currentDateTime = dateFormat.format(Date())
                // "transcription_YYYYMMDD_HHMMSS.txt" 형태의 파일 이름을 생성합니다.
                val fileName = "transcription_$currentDateTime.txt"

                // 파일 객체를 생성합니다. 파일은 외부 저장소의 Downloads 디렉토리에 저장됩니다.
                val file = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    fileName
                )

                FileOutputStream(file).use { fos ->
                    OutputStreamWriter(fos).use { writer ->
                        writer.write(transcription)
                    }
                }
                val fileSpace = file.absolutePath
                Log.d("transcribe", "번역된 파일 : $fileSpace")  // 번역된 파일 경로 Log 출력

                fileSpace
            }
        }
}
