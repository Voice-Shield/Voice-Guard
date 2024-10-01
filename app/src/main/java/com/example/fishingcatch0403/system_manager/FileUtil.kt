package com.example.fishingcatch0403.system_manager

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import java.io.File
import java.io.IOException

class FileUtil(private val contentResolver: ContentResolver) {
    companion object {
        const val STT_PATH = "/STT_Call"
    }

    // 파일 URI 생성 함수
    fun createFileUri(fileName: String): Uri? {
        val values = ContentValues()    // ContentValues 객체 생성
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName) // 파일 이름 설정
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain") // MIME 타입 설정

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31 이상 (Scoped Storage 도입)
            values.put(
                MediaStore.MediaColumns.RELATIVE_PATH,  // 저장 경로 설정
                Environment.DIRECTORY_DOCUMENTS + STT_PATH // 저장 경로 설정
            )
        } else { // API 30 이하 (Scoped Storage 미도입)
            val directory =
                File("${Environment.getExternalStorageDirectory()}/${Environment.DIRECTORY_DOCUMENTS}$STT_PATH")
            if (!directory.exists()) {
                directory.mkdirs() // 디렉토리가 없으면 생성
            }
            values.put(
                MediaStore.MediaColumns.DATA,
                directory.absolutePath + "/" + fileName
            )   // 파일 경로 설정
        }

        val uri =
            contentResolver.insert(MediaStore.Files.getContentUri("external"), values)   // URI 생성
        return uri
    }

    @Throws(IOException::class)
    fun getFileDescriptor(uri: Uri): ParcelFileDescriptor { // ParcelFileDescriptor 객체 반환
        val pfd = contentResolver.openFileDescriptor(uri, "w")  // ParcelFileDescriptor 객체 생성
            ?: throw IOException("Cannot open file descriptor for URI: $uri")   // ParcelFileDescriptor 생성 실패 시 예외 발생
        return pfd
    }

    // 실제 파일 경로를 저장하는 함수
    fun getRealPathFromUri(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Files.FileColumns.DATA)  // 실제 경로를 쿼리할 컬럼
        val cursor = context.contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                return it.getString(columnIndex)  // 실제 파일 경로 반환
            }
        }
        return null
    }

    // 최근에 저장된 녹음 파일을 가져오는 함수
    fun getLatestRecordingFile(): File? {
        // 파일 경로 설정
        val recordingsPath =
            Environment.getExternalStorageDirectory().absolutePath + "/Recordings/Call"
        val recordingsDir = File(recordingsPath)

        // 폴더가 존재하고, 폴더가 비어있지 않을 때
        if (recordingsDir.exists() && recordingsDir.isDirectory) {
            // 폴더 내의 모든 m4a 파일 필터링
            val m4aFiles = recordingsDir.listFiles { file ->
                file.extension.equals("m4a", ignoreCase = true)
            }

            // m4aFiles가 null이 아닌 경우
            if (m4aFiles != null && m4aFiles.isNotEmpty()) {
                // 가장 최근에 수정된 m4a 파일 찾기
                val latestFile = m4aFiles.maxByOrNull { it.lastModified() }
                return latestFile // 가장 최근의 파일 반환
            }
        }
        return null // 폴더가 없거나 m4a 파일이 없을 경우
    }
}
