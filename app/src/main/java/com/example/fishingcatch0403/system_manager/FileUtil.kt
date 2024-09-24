package com.example.fishingcatch0403.system_manager

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.IOException

class FileUtil(private val contentResolver: ContentResolver) {

    // 파일 URI 생성 함수(녹음 파일 저장 경로)
    fun createFileUri(fileName: String): Uri {
        val values = ContentValues()    // ContentValues 객체 생성
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName) // 파일 이름 설정
        values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/m4a") // MIME 타입 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31 이상 (Scoped Storage 도입)
            values.put(
                MediaStore.MediaColumns.RELATIVE_PATH,  // 저장 경로 설정
                Environment.DIRECTORY_RECORDINGS + "/Call" // 저장 경로 설정
            )
        } else { // API 30 이하 (Scoped Storage 미도입)
            val directory =
                File(Environment.getExternalStorageDirectory().toString() + "/Recordings/Call")
            if (!directory.exists()) {
                directory.mkdirs() // 디렉토리가 없으면 생성
            }
            values.put(
                MediaStore.MediaColumns.DATA,
                directory.absolutePath + "/" + fileName
            )   // 파일 경로 설정
        }

        val uri =
            contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)   // URI 생성
                ?: throw IOException("Failed to create new MediaStore record.") // URI 생성 실패 시 예외 발생
        return uri
    }

    // 녹음 파일의 실제 파일 경로를 저장하는 함수
    fun getRealPathFromUri(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Audio.Media.DATA)  // 실제 경로를 쿼리할 컬럼
        val cursor = context.contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                return it.getString(columnIndex)  // 실제 파일 경로 반환
            }
        }
        return null
    }
}
