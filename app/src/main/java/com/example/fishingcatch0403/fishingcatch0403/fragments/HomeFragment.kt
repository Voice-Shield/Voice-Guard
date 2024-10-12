package com.example.fishingcatch0403.fishingcatch0403.fragments

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.util.Log
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
import com.example.fishingcatch0403.databinding.FragmentHomeBinding
import com.example.fishingcatch0403.rest_api.ApiController
import com.example.fishingcatch0403.rest_api.SttResultCallback
import com.example.fishingcatch0403.stt.CHANNEL_ID
import com.example.fishingcatch0403.stt.M4AFileModel
import com.example.fishingcatch0403.stt.MainViewModel
import com.example.fishingcatch0403.stt.MainViewModelFactory
import com.example.fishingcatch0403.stt.State
import com.example.fishingcatch0403.stt.notificationId
import com.example.fishingcatch0403.system_manager.ProgressBarManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var mBinding: FragmentHomeBinding? = null   // 뷰 바인딩
    private val adapter by lazy {
        ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, ArrayList<String>())
    }

    private lateinit var m4aDataList: MutableList<M4AFileModel>
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory(requireContext())
    }

    private var showLoadingBar = false
    private var showToast = false

    private lateinit var progressBarManager: ProgressBarManager
    private lateinit var apiController: ApiController
    private lateinit var notificationManager: NotificationManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentHomeBinding.inflate(inflater, container, false)
        m4aDataList = mutableListOf()

        mBinding!!.run {
            recordingListview.adapter = adapter
            recordingListview.setOnItemClickListener { _, _, position, _ ->
                showToast = true
                m4aItemClick(position)
            }
        }
        showToast = true
        mainViewModel.loadM4AFiles()

        collectLatestStateFlow(mainViewModel.m4aFileState) { state ->
            when (state) {
                is State.Success -> {
                    showToast = false
                    with(state) {
                        if (::m4aDataList.isInitialized) {
                            val new = result.filter { it !in m4aDataList }
                            m4aDataList.addAll(new)
                            adapter.addAll(new.map { m4a -> m4a.fileName })
                        } else {
                            m4aDataList = result.toMutableList()
                            adapter.addAll(result.map { m4a -> m4a.fileName })
                        }
                        adapter.notifyDataSetChanged()
                    }
                }

                is State.Error -> {
                    setToast("파일을 찾을 수 없습니다.")
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

                is State.Loading -> {
                    showLoadingBar = true
                }
            }
        }
        return mBinding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Activity에서 NotificationManager 초기화
        notificationManager =
            requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // ApiController 초기화
        apiController = ApiController()

        // ProgressBarManager 초기화
        initProgressBarManager() // 이 위치가 apiController 초기화 후에 오도록 변경
    }


    private fun initProgressBarManager() {
        progressBarManager =
            context?.let {
                ProgressBarManager(
                    it,
                    notificationManager,
                    notificationId,
                    CHANNEL_ID
                )
            } ?: run {
                // 에러 처리: context가 null일 경우
                Log.e("[APP] HomeFragment", "Context가 null 입니다")
                return
            }
        apiController.initProgressBarManager(progressBarManager) // ApiController에 ProgressBarManager 전달
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

    // M4A 파일을 클릭할 때 STT 요청을 보내는 함수
    private fun m4aItemClick(position: Int) {
        Toast.makeText(requireContext(), "STT 분석 시작", Toast.LENGTH_SHORT).show()

        // 선택한 M4A 파일에 대해 STT 분석 시작
        val selectedFile = m4aDataList[position]
        mainViewModel.startSTT(selectedFile.filePath, object : SttResultCallback {
            override fun onSuccess(result: String) {
                // STT 분석 성공 시
                setToast("STT 분석 완료: $result")
            }

            override fun onError(errorMessage: String) {
                // STT 분석 실패 시
                setToast("STT 분석 중 오류 발생: $errorMessage")
            }
        })
    }

    // 뷰 바인딩 해제
    private fun <T> Fragment.collectLatestStateFlow(flow: Flow<T>, collector: suspend (T) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                flow.collectLatest(collector)
            }
        }
    }
}
