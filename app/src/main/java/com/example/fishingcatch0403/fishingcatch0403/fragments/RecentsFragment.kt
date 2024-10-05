package com.example.fishingcatch0403.fishingcatch0403.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.fishingcatch0403.R
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class RecentsFragment : Fragment() {

    private var listViewAdapter: ArrayAdapter<String>? = null   // ListView에 사용할 Adapter
    private val fileList = ArrayList<String>()  // 파일 목록을 저장할 ArrayList

    override fun onCreateView(
        inflater: LayoutInflater,   // LayoutInflater: XML 레이아웃을 코드로 변환하는 역할
        container: ViewGroup?,  // ViewGroup: 뷰들이 포함된 컨테이너
        savedInstanceState: Bundle? // Bundle: 액티비티의 이전 상태를 전달하는 객체
    ): View? {
        // fragment_recents.xml 파일을 인플레이션하여 뷰로 반환
        val view = inflater.inflate(R.layout.fragment_recents, container, false)

        // 파일 목록을 표시할 ListView 참조
        val listView = view.findViewById<ListView>(R.id.textfiles)

        // ListView에 파일 목록을 표시할 Adapter 생성
        listViewAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, fileList)
        listView.adapter = listViewAdapter

        // ListView의 아이템을 클릭했을 때 이벤트 처리
        listView.setOnItemClickListener { parent, view, position, id ->
            // 클릭한 아이템의 파일명을 가져옴
            val fileName = fileList[position]
            // 여기서 파일을 읽거나 다른 작업을 수행할 수 있습니다.
            val fileContents = readFileContents(fileName)
            Toast.makeText(requireContext(), "$fileName 선택됨", Toast.LENGTH_SHORT).show()
            Toast.makeText(requireContext(), fileContents, Toast.LENGTH_LONG).show()
        }

        // 저장소 읽기 권한이 있는지 확인
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 100)
        } else {
            loadTextFiles()
        }
        return view
    }

    // 저장소 읽기 권한 요청 결과 처리
    private fun loadTextFiles() {
        // 다운로드 폴더 경로
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        // 다운로드 폴더에 있는 .txt 파일 목록을 가져옴
        val files = path.listFiles { _, name -> name.endsWith(".txt") }
        // 파일 목록 초기화
        fileList.clear()
        // 파일 목록에 파일명 추가
        files?.forEach { file ->
            fileList.add(file.name)
        }
        // ListView 갱신
        listViewAdapter?.notifyDataSetChanged()
    }

    // 파일 내용을 읽어오는 함수
    private fun readFileContents(fileName: String): String {
        // 파일 내용을 저장할 StringBuilder 객체 생성
        val stringBuilder = StringBuilder()
        // 파일을 읽어오는 코드
        try {
            // 파일 경로 생성
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            // 파일을 읽어오는 코드
            val inputStream = file.inputStream()
            // 파일 내용을 읽어오는 BufferedReader 객체 생성
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            // 파일 내용을 한 줄씩 읽어서 StringBuilder에 추가
            var line: String?
            // 파일 내용을 한 줄씩 읽어서 StringBuilder에 추가
            while (bufferedReader.readLine().also { line = it } != null) {
                stringBuilder.append(line).append('\n')
            }
            // 파일을 닫음
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            return "파일을 읽는 중 오류가 발생했습니다."
        }
        return stringBuilder.toString()
    }

    // 권한 요청 결과 처리
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(    // 권한 요청 결과 처리
        requestCode: Int,   // 요청 코드
        permissions: Array<out String>, // 요청한 권한 목록
        grantResults: IntArray  // 권한 요청 결과
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )    // 상위 클래스의 메소드 호출
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {  // 권한 요청 코드가 100이고 권한이 허용된 경우
            loadTextFiles() // 저장소 읽기 권한이 허용된 경우 파일 목록을 불러옵니다.
        } else {    // 권한이 거부된 경우
            Toast.makeText(requireContext(), "저장소 읽기 권한이 거부되었습니다.", Toast.LENGTH_SHORT)
                .show()  // 토스트 메시지를 표시합니다.
        }
    }
}
