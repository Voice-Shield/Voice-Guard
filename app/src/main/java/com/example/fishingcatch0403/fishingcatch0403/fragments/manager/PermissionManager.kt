package com.example.fishingcatch0403.manager

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager (val context: Context, val activity: Activity) {


    fun readFile() {

        // m4a 파일 경로
        val m4aPath = Environment.getExternalStorageDirectory().absolutePath + "/audio.m4a"

        // MediaMetadataRetriever 사용
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(m4aPath)

        // 메타데이터 읽기
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)

        // 미리보기 이미지 추출
        val bitmap = retriever.getFrameAtTime()

        // 파일 입력 스트림 가져오기
        val `is` = retriever.embeddedPicture

        // 파일 처리
        AudioProcessManager()

        // 리소스 해제
        retriever.release()
    }

    private fun AudioProcessManager() {
        TODO("Not yet implemented")
    }


    fun asdf() {
        // 권한 확인 및 요청
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // 권한이 없는 경우 권한 요청
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1000)

        } else {
            // 권한 있는 경우 파일 읽기 처리
            readFile()
        }

        fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
            onRequestPermissionsResult(requestCode, permissions, grantResults)

            if(requestCode == 1000 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                // 권한 획득 성공시 파일 읽기
                readFile()
            }
        }
    }
}