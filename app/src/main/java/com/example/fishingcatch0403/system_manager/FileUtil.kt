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
import java.io.FileOutputStream
import java.io.IOException

class FileUtil(private val contentResolver: ContentResolver) {
    companion object {
        const val STT_PATH = "/STT_Call"  // STT 결과 파일이 저장될 경로
    }

    // 파일 URI 생성 함수
    private fun createFileUri(fileName: String): Uri? {
        val values = ContentValues()  // ContentValues 객체 생성
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName) // 파일 이름 설정
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain") // MIME 타입 설정

        // 다운로드 폴더에 저장하기 위한 경로 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31 이상 (Scoped Storage 도입)
            values.put(
                MediaStore.MediaColumns.RELATIVE_PATH,  // 저장 경로 설정
                Environment.DIRECTORY_DOWNLOADS + STT_PATH // 다운로드 폴더 경로 설정
            )
        } else { // API 30 이하 (Scoped Storage 미도입)
            val directory =
                File("${Environment.getExternalStorageDirectory()}/${Environment.DIRECTORY_DOWNLOADS}$STT_PATH")
            if (!directory.exists()) {
                directory.mkdirs() // 디렉토리가 없으면 생성
            }
            values.put(
                MediaStore.MediaColumns.DATA,
                directory.absolutePath + "/" + fileName // 파일 경로 설정
            )
        }

        return contentResolver.insert(MediaStore.Files.getContentUri("external"), values) // URI 생성
    }

    // STT 결과를 파일로 저장하는 함수
    fun saveSTTResultToFile(result: String, fileName: String): Uri? {
        val uri = createFileUri(fileName) // 파일 URI 생성
        uri?.let {
            try {
                // 파일 디스크립터를 열어 STT 결과를 파일에 작성
                contentResolver.openFileDescriptor(it, "w")?.use { pfd ->
                    // OutputStream을 사용하여 데이터 쓰기
                    FileOutputStream(pfd.fileDescriptor).use { outputStream ->
                        outputStream.write(result.toByteArray()) // STT 결과를 바이트 배열로 변환하여 파일에 기록
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace() // 예외 발생 시 스택 트레이스 출력
                return null // 실패 시 null 반환
            }
        }
        return uri // 생성된 URI 반환
    }

    @Throws(IOException::class)
    // 파일의 ParcelFileDescriptor를 반환하는 함수
    fun getFileDescriptor(uri: Uri): ParcelFileDescriptor {
        val pfd = contentResolver.openFileDescriptor(uri, "w")
            ?: throw IOException("Cannot open file descriptor for URI: $uri") // 파일 디스크립터 생성 실패 시 예외 발생
        return pfd // 성공 시 ParcelFileDescriptor 반환
    }

    // URI로부터 실제 파일 경로를 가져오는 함수
    fun getRealPathFromUri(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Files.FileColumns.DATA) // 실제 경로를 쿼리할 컬럼
        val cursor = context.contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                return it.getString(columnIndex) // 실제 파일 경로 반환
            }
        }
        return null // 파일 경로를 찾지 못한 경우 null 반환
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
                file.extension.equals("m4a", ignoreCase = true) // m4a 확장자인 파일만 필터링
            }

            // m4aFiles가 null이 아닌 경우
            if (m4aFiles != null && m4aFiles.isNotEmpty()) {
                // 가장 최근에 수정된 m4a 파일 찾기
                val latestFile = m4aFiles.maxByOrNull { it.lastModified() }
                return latestFile // 가장 최근의 파일 반환
            }
        }
        return null // 폴더가 없거나 m4a 파일이 없을 경우 null 반환
    }
}
