package com.example.fishingcatch0403.fishingcatch0403.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.fishingcatch0403.MainViewModel
import com.example.fishingcatch0403.State
import com.example.fishingcatch0403.WavModel
import com.example.fishingcatch0403.databinding.FragmentHomeBinding
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

/** 메인 화면
 *
 *
 *- 보여줄 내용
 * 1. wav 녹음 파일들을 리스트로 보여줌(List<String>)
 * 2.
 * */
class HomeFragment : Fragment(){
    private var mBinding: FragmentHomeBinding? = null   // 뷰 바인딩
    private val adapter by lazy {
        ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, ArrayList<String>())
    }
    private val credentials by lazy {
        requireContext().assets.open("fishing0408-1c5f19ff4af6.json").use { inputStream ->
            return@use GoogleCredentials.fromStream(inputStream)
        }
    }

    private lateinit var convertedWavDataList: MutableList<WavModel>
    private val mainViewModel by viewModels<MainViewModel>() // MainViewModel 객체 생성

    private var showLoadingBar = false
    private var showToast = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentHomeBinding.inflate(inflater, container, false)
        mBinding!!.run {
            recordingListview.adapter = adapter
            recordingListview.setOnItemClickListener { _, _, position, _ ->
                showToast = true
                wavItemClick(position)
            }
        }
        showToast = true
        mainViewModel.loadRecordings()
        mainViewModel.setCredentials(credentials)

        collectLatestStateFlow(mainViewModel.recordState) { state ->
            when (state) {
                is State.Success -> {
                    showToast = false
                    with(state) {
                        if (::convertedWavDataList.isInitialized) {
                            val new = result.filter { it !in convertedWavDataList }
                            convertedWavDataList.addAll(new)
                            adapter.addAll(new.map { wav -> wav.fileName })
                        } else {
                            convertedWavDataList = result.toMutableList()
                            adapter.addAll(result.map { wav -> wav.fileName })
                        }
                        adapter.notifyDataSetChanged()
                    }
                }

                is State.Error -> {
                    setToast("파일 변환 중에 에러가 발생하였습니다.")
                }

                is State.Loading -> {}
            }
        }
        collectLatestStateFlow(mainViewModel.transcriptState) {
            when (val value = it) {
                is State.Success -> {
                    setToast("파일 경로 : ${value.result.answer}") {
                        showLoadingBar = false
                    }
                }

                is State.Error -> {
                    setToast("파일 변환 중에 에러가 발생하였습니다.") {
                        showLoadingBar = false
                    }
                }

                //로딩바를 진행하면 됨
                is State.Loading -> {
                    showLoadingBar = true
                }
            }
        }

        return mBinding!!.root
    }

    // 토스트 메시지 중복 제거를 위한 함수
    private fun setToast(msg: String, extension: () -> Unit = {}) {
        if (showToast) {
            extension()
            Toast.makeText(
                requireContext(),
                msg,
                Toast.LENGTH_SHORT
            ).show()
            showToast = false
        }
    }

    //wav 파일 리스트에서 파일을 선택할 때 호출되는 함수
    private fun wavItemClick(position: Int) {
        Toast.makeText(requireContext(), "분석 시작", Toast.LENGTH_SHORT).show()
        convertedWavDataList[position].let {
            val outputFilePath = File(requireContext().cacheDir, "mono_${it.fileName}").absolutePath
            mainViewModel.analyzeRecordedFile(it.filePath, outputFilePath)
        }
    }

    private fun <T> Fragment.collectLatestStateFlow(flow: Flow<T>, collector: suspend (T) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                flow.collectLatest(collector)
            }
        }
    }
}
