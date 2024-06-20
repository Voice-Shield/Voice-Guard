package com.example.fishingcatch0403.fishingcatch0403.fragments

import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.example.fishingcatch0403.R
import com.example.fishingcatch0403.databinding.FragmentHomeBinding
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1.*
import com.google.protobuf.ByteString
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.file.Files
import java.nio.file.Paths
import kotlinx.coroutines.*
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.RecognitionAudio
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding
import com.google.cloud.speech.v1.RecognizeResponse
import java.net.URLConnection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class HomeFragment : Fragment(), CoroutineScope by MainScope() {
    private var mBinding: FragmentHomeBinding? = null
    private lateinit var recordingListView: ListView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        val binding = FragmentHomeBinding.inflate(inflater, container, false)
        mBinding = binding
        recordingListView = binding.recordingListview

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadRecordings()
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

    private fun convertToMono(
        inputFilePath: String,  // 입력 파일 경로
        outputFilePath: String, // 출력 파일 경로
        onComplete: (Boolean) -> Unit   // 변환 완료 시 호출할 콜백 함수
    ) {
        val command = arrayOf("-i", inputFilePath, "-ac", "1", outputFilePath)  // FFmpeg 명령어

        // 명령어와 경로를 로그로 출력
        Log.d(
            "FFmpeg", "Command: ${command.joinToString(" ")}, " +
                    "Output: $outputFilePath, " +
                    "Input: $inputFilePath"
        )

        // 출력 파일이 이미 존재할 경우 삭제
        val outputFile = File(outputFilePath)
        if (outputFile.exists()) {
            outputFile.delete()
        }

        // FFmpeg 명령어 실행
        FFmpeg.executeAsync(command) { _, returnCode ->
            onComplete(returnCode == 0) // 성공 시 onComplete 함수 호출
        }
    }

    private fun loadRecordings() {
        val ringtonesFolderPath =
            Environment.getExternalStorageDirectory().absolutePath + "/Recordings/Call"
        val ringtonesFolder = File(ringtonesFolderPath)
        val files = ringtonesFolder.listFiles()

        // 파일 리스트 로딩 실패 시 처리
        if (files.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "녹음 파일이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // m4a 파일만 필터링
        val m4aFiles = files.filter { it.extension == "m4a" }

        // m4a 파일이 없을 경우 처리
        if (m4aFiles.isEmpty()) {
            Toast.makeText(requireContext(), "m4a 녹음 파일이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 리스트뷰에 파일 이름을 표시하기 위한 어댑터
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, ArrayList<String>())
        recordingListView.adapter = adapter

        // 변환된 파일 경로를 저장할 맵
        val convertedFilesMap = mutableMapOf<String, String>()

        // m4a 파일을 wav 파일로 변환(코루틴 사용)
        GlobalScope.launch(Dispatchers.IO) {
            m4aFiles.forEach { m4aFile ->   // m4a 파일을 하나씩 처리
                val wavFileName = m4aFile.nameWithoutExtension + ".wav" // 변환된 파일 이름
                val wavFilePath = m4aFile.parent + "/" + wavFileName    // 변환된 파일 경로
                val wavFile = File(wavFilePath) // 변환된 파일 객체

                if (!wavFile.exists()) {    // wav 파일이 없을 경우만 변환
                    if (convertM4aToWav(m4aFile.path, wavFilePath)) {   // m4a 파일을 wav 파일로 변환
                        withContext(Dispatchers.Main) { // UI 스레드에서 처리
                            adapter.add(wavFileName)    // 리스트뷰에 wav 파일 이름 추가
                            convertedFilesMap[wavFileName] = wavFilePath    // 변환된 파일 경로 저장
                        }
                    } else {    // 변환 실패 시 토스트 메시지 출력
                        withContext(Dispatchers.Main) { // UI 스레드에서 처리
                            Toast.makeText(
                                requireContext(),
                                "${m4aFile.name} 파일 변환에 실패했습니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {    // wav 파일이 이미 존재할 경우
                    withContext(Dispatchers.Main) { // UI 스레드에서 처리
                        adapter.add(wavFileName)    // 리스트뷰에 wav 파일 이름 추가
                        convertedFilesMap[wavFileName] = wavFilePath    // 변환된 파일 경로 저장
                    }
                }
            }

            withContext(Dispatchers.Main) { // UI 스레드에서 처리
                adapter.notifyDataSetChanged()    // 리스트뷰 갱신
            }
        }

        recordingListView.setOnItemClickListener { _, _, position, _ -> // 리스트뷰 아이템 클릭 시
            val selectedFileName =
                adapter.getItem(position) ?: return@setOnItemClickListener   // 선택 파일 이름
            val selectedFilePath =
                convertedFilesMap[selectedFileName] ?: return@setOnItemClickListener    // 선택 파일 경로
            val outputFilePath =
                File(requireContext().cacheDir, "mono_$selectedFileName").absolutePath  // 변환된 파일 경로

            // 선택 파일 속성 Log 출력
            Log.d(
                "FileSelection",
                "fileName: $selectedFileName, filePath: $selectedFilePath, fileExtension: wav, mimeType: ${
                    URLConnection.guessContentTypeFromName(selectedFileName) ?: "Unknown"
                }"
            )

            // 파일 선택 토스트 메시지 출력
            Toast.makeText(requireContext(), "$selectedFileName 선택됨", Toast.LENGTH_SHORT).show()

            // 선택된 파일을 처리하는 로직 (오디오 파일을 텍스트로 변환)
            convertToMono(selectedFilePath, outputFilePath) { success ->
                if (success) {
                    // 변환된 모노 파일로 STT 처리
                    transcribeAudio(outputFilePath)
                } else {
                    Toast.makeText(requireContext(), "오디오 변환 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getGoogleCredentialsFromAsset(
        context: Context, assetName: String
    ): GoogleCredentials {
        context.assets.open(assetName).use { inputStream ->
            return GoogleCredentials.fromStream(inputStream)
        }
    }

    private fun transcribeAudio(filePath: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val credentials =
                    getGoogleCredentialsFromAsset(requireContext(), "fishing0408-1c5f19ff4af6.json")
                val settings = SpeechSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build()

                SpeechClient.create(settings).use { speechClient ->
                    val data = Files.readAllBytes(Paths.get(filePath))
                    val audioBytes = ByteString.copyFrom(data)

                    val config = RecognitionConfig.newBuilder().setEncoding(AudioEncoding.LINEAR16)
                        .setSampleRateHertz(44100).setLanguageCode("ko-KR").build()

                    val audio = RecognitionAudio.newBuilder().setContent(audioBytes).build()

                    val response = speechClient.recognize(config, audio)
                    val results = response.resultsList

                    withContext(Dispatchers.Main) {
                        // 결과가 비어있는 경우 처리
                        if (results.isEmpty()) {
                            Toast.makeText(
                                requireContext(), "변환할 수 있는 오디오가 없습니다.", Toast.LENGTH_LONG
                            ).show()
                            return@withContext
                        }

                        for (result in results) {
                            val alternative = result.alternativesList[0]
                            Toast.makeText(
                                requireContext(),
                                "Transcription: ${alternative.transcript}",
                                Toast.LENGTH_LONG
                            ).show()
                            saveTranscriptionToFile(
                                alternative.transcript
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "오류 발생: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
                e.printStackTrace()
            }
        }
    }

    private fun saveTranscriptionToFile(transcription: String) {
        try {
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
            Toast.makeText(requireContext(), "$fileSpace 에 저장됨", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "파일 저장 오류: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel() // CoroutineScope를 정리합니다.
    }
}
