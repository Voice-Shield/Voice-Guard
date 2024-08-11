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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
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
class HomeFragment : Fragment(), CoroutineScope by MainScope() {
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

    var showLoadingBar = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentHomeBinding.inflate(inflater, container, false)
        mBinding!!.run {
            recordingListview.adapter = adapter
            recordingListview.setOnItemClickListener { _, _, position, _ ->
                wavItemClick(position)
            }
        }
        mainViewModel.loadRecordings()
        mainViewModel.setCredentials(credentials)

        collectLatestStateFlow(mainViewModel.recordState) { state ->
            when (val value = state) {
                is State.Success -> {
                    with(value) {
                        if (::convertedWavDataList.isInitialized) {
                            val new = result.filter { it !in convertedWavDataList }
                            convertedWavDataList.addAll(new)
                            adapter.addAll(new.map { wav->wav.fileName })
                        } else {
                            convertedWavDataList = result.toMutableList()
                            adapter.addAll(result.map { wav -> wav.fileName })
                        }
                        adapter.notifyDataSetChanged()
                    }
                }

                is State.Error -> {
                    Toast.makeText(
                        requireContext(),
                        "파일 변환 중에 에러가 발생하였습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is State.Loading -> {}
            }
        }
        collectLatestStateFlow(mainViewModel.transcriptState) {
            when (val value = it) {
                is State.Success -> {
                    showLoadingBar = false
                    Toast.makeText(
                        requireContext(),
                        "파일 경로 : ${value.result.answer}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is State.Error -> {
                    showLoadingBar = false
                    Toast.makeText(
                        requireContext(),
                        "파일 변환 중에 에러가 발생하였습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                //로딩바를 진행하면 됨
                is State.Loading -> {
                    showLoadingBar = true
                }
            }
        }


        return mBinding!!.root
    }


    //wav 파일 리스트에서 파일을 선택할 때 호출되는 함수
    private fun wavItemClick(position: Int) {
        Toast.makeText(requireContext(), "분석 시작", Toast.LENGTH_SHORT).show()
        convertedWavDataList[position].let {
            val outputFilePath =
                File(requireContext().cacheDir, "mono_${it.fileName}").absolutePath
            mainViewModel.analyzeRecordedFile(it.filePath, outputFilePath)
        }
    }

    fun <T> Fragment.collectLatestStateFlow(flow: Flow<T>, collector: suspend (T) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                flow.collectLatest(collector)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel() // CoroutineScope를 정리합니다.
    }
}
