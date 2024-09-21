package com.example.fishingcatch0403.system_manager

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import java.io.File
import java.io.IOException

class FileUtil(private val contentResolver: ContentResolver) {

    // 파일 URI 생성 함수(녹음 파일 저장 경로)
    fun createFileUri(fileName: String): Uri {
        val values = ContentValues()    // ContentValues 객체 생성
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName) // 파일 이름 설정
        values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/m4a") // MIME 타입 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 29 이상
            values.put(
                MediaStore.MediaColumns.RELATIVE_PATH,  // 저장 경로 설정
                Environment.DIRECTORY_RECORDINGS + "/Call" // 저장 경로 설정
            )
        } else { // API 28 이하 (Scoped Storage 도입 전)
            val directory =
                File(Environment.getExternalStorageDirectory().toString() + "/Recordings/Call")
            if (!directory.exists()) {
                directory.mkdirs() // 디렉토리가 없으면 생성
            }
            values.put(MediaStore.MediaColumns.DATA, directory.absolutePath + "/" + fileName)
        }

        val uri =
            contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)   // URI 생성
                ?: throw IOException("Failed to create new MediaStore record.") // URI 생성 실패 시 예외 발생
        return uri
    }

    @Throws(IOException::class)
    fun getFileDescriptor(uri: Uri): ParcelFileDescriptor { // ParcelFileDescriptor 객체 반환
        val pfd = contentResolver.openFileDescriptor(uri, "w")  // ParcelFileDescriptor 객체 생성
            ?: throw IOException("Cannot open file descriptor for URI: $uri")   // ParcelFileDescriptor 생성 실패 시 예외 발생
        return pfd
    }
}
