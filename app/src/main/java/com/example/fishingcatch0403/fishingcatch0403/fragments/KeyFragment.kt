package com.example.fishingcatch0403.fishingcatch0403.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.fishingcatch0403.databinding.FragmentKeyBinding

class KeyFragment : Fragment() {

    private var mBinding: FragmentKeyBinding? = null
    private lateinit var inputField: EditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentKeyBinding.inflate(inflater, container, false)
        mBinding = binding

        inputField = binding.etNumbers

        // 전화 걸기 버튼 클릭 리스너
        binding.iconCall.setOnClickListener {
            makeCall()
        }

        // 삭제 버튼 클릭 리스너
        binding.iconBack.setOnClickListener {
            deleteLastNumber()
        }

        // 숫자 버튼 클릭 리스너
        setNumberButtonClickListener(binding)

        return mBinding?.root
    }

    private fun setNumberButtonClickListener(binding: FragmentKeyBinding) {
        binding.one.setOnClickListener { appendToInput("1") }
        binding.two.setOnClickListener { appendToInput("2") }
        binding.three.setOnClickListener { appendToInput("3") }
        binding.four.setOnClickListener { appendToInput("4") }
        binding.five.setOnClickListener { appendToInput("5") }
        binding.six.setOnClickListener { appendToInput("6") }
        binding.seven.setOnClickListener { appendToInput("7") }
        binding.eight.setOnClickListener { appendToInput("8") }
        binding.nine.setOnClickListener { appendToInput("9") }
        binding.zero.setOnClickListener { appendToInput("0") }
        binding.star.setOnClickListener { appendToInput("*") }
        binding.sharp.setOnClickListener { appendToInput("#") }
    }

    private fun appendToInput(value: String) {
        inputField.append(value)
    }

    private fun deleteLastNumber() {
        val currentText = inputField.text.toString()
        if (currentText.isNotEmpty()) {
            inputField.setText(currentText.substring(0, currentText.length - 1))
            inputField.setSelection(inputField.text.length) // 커서를 마지막 위치로 이동
        }
    }

    private fun makeCall() {
        val phoneNumber = inputField.text.toString()
        if (phoneNumber.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            // 전화 걸기 권한 요청
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.CALL_PHONE), REQUEST_CODE_CALL
                )
            } else {
                startActivity(intent)
            }
        }
    }

    override fun onDestroyView() {
        mBinding = null
        super.onDestroyView()
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_CALL -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    makeCall()
                }
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_CALL = 2
    }
}
