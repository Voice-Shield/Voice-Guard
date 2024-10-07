package com.example.fishingcatch0403.system_manager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(val context: Context) {

    // 필요한 권한들을 문자열로 선언합니다.
    private val statePermission = android.Manifest.permission.READ_PHONE_STATE

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val audioPermission = android.Manifest.permission.READ_MEDIA_AUDIO
    private val contactPermission = android.Manifest.permission.READ_CONTACTS
    private val storagePermission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    private val readPermission = android.Manifest.permission.READ_EXTERNAL_STORAGE
    private val readCallLogPermission = android.Manifest.permission.READ_CALL_LOG

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val notificationPermission = android.Manifest.permission.POST_NOTIFICATIONS
    private val overlayPermission = Settings.ACTION_MANAGE_OVERLAY_PERMISSION // 오버레이 권한을 위한 액션

    // 권한을 확인하고 요청하는 메소드
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun checkPermissions() {
        // context를 Activity로 캐스팅
        val activity = context as? Activity ?: return

        // 개별 권한 상태를 확인합니다.
        val isStateOK = ContextCompat.checkSelfPermission(activity, statePermission) ==
                PackageManager.PERMISSION_GRANTED
        val isAudioOK = ContextCompat.checkSelfPermission(activity, audioPermission) ==
                PackageManager.PERMISSION_GRANTED
        val isContactOK = ContextCompat.checkSelfPermission(activity, contactPermission) ==
                PackageManager.PERMISSION_GRANTED
        val isStorageOK = ContextCompat.checkSelfPermission(activity, storagePermission) ==
                PackageManager.PERMISSION_GRANTED
        val isReadOK = ContextCompat.checkSelfPermission(activity, readPermission) ==
                PackageManager.PERMISSION_GRANTED
        val isCallLogOK = ContextCompat.checkSelfPermission(activity, readCallLogPermission) ==
                PackageManager.PERMISSION_GRANTED
        val isNotificationOK = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(activity, notificationPermission) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        // 오버레이 권한 확인
        val isOverlayOK = Settings.canDrawOverlays(activity)

        // 요청할 권한 목록을 저장하는 리스트
        val permissionsToRequest = mutableListOf<String>()

        // Android 13 이상일 때의 권한 처리
        when {
            Build.VERSION.SDK_INT >= 33 -> {
                if (!isAudioOK || !isStorageOK || !isStateOK || !isContactOK || !isCallLogOK || !isNotificationOK || !isOverlayOK) {
                    // 필요한 권한들을 리스트에 추가
                    if (!isAudioOK) permissionsToRequest.add(audioPermission)
                    if (!isStateOK) permissionsToRequest.add(statePermission)
                    if (!isContactOK) permissionsToRequest.add(contactPermission)
                    if (!isCallLogOK) permissionsToRequest.add(readCallLogPermission)
                    if (!isNotificationOK) permissionsToRequest.add(notificationPermission)
                    if (!isOverlayOK) {
                        // 오버레이 권한 요청을 위한 인텐트 시작
                        val intent = Intent(
                            overlayPermission,
                            Uri.parse("package:${activity.packageName}")
                        )
                        activity.startActivityForResult(intent, 1001)
                    }
                    if (permissionsToRequest.isNotEmpty()) {
                        // 필요한 권한 요청
                        ActivityCompat.requestPermissions(
                            activity,
                            permissionsToRequest.toTypedArray(),
                            1000
                        )
                    }
                }
            }

            // Android 11 이상일 때의 권한 처리
            Build.VERSION.SDK_INT >= 30 -> {
                if (!isStorageOK || !isStateOK || !isContactOK || !isReadOK || !isOverlayOK) {
                    if (!Environment.isExternalStorageManager()) {
                        // 모든 파일 접근 권한 요청
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:${activity.packageName}")
                        activity.startActivity(intent)
                    } else {
                        if (!isStateOK) permissionsToRequest.add(statePermission)
                        if (!isContactOK) permissionsToRequest.add(contactPermission)
                        if (!isReadOK) permissionsToRequest.add(readPermission)
                        if (!isOverlayOK) {
                            // 오버레이 권한 요청을 위한 인텐트 시작
                            val intent = Intent(
                                overlayPermission,
                                Uri.parse("package:${activity.packageName}")
                            )
                            activity.startActivityForResult(intent, 1001)
                        }
                        if (permissionsToRequest.isNotEmpty()) {
                            // 필요한 권한 요청
                            ActivityCompat.requestPermissions(
                                activity,
                                permissionsToRequest.toTypedArray(),
                                1000
                            )
                        }
                    }
                }
            }

            // 그 외 버전에서의 권한 처리
            else -> {
                if (!isStorageOK || !isReadOK || !isStateOK || !isContactOK || !isCallLogOK || !isOverlayOK) {
                    if (!isStorageOK) permissionsToRequest.add(storagePermission)
                    if (!isReadOK) permissionsToRequest.add(readPermission)
                    if (!isStateOK) permissionsToRequest.add(statePermission)
                    if (!isContactOK) permissionsToRequest.add(contactPermission)
                    if (!isCallLogOK) permissionsToRequest.add(readCallLogPermission)
                    if (!isOverlayOK) {
                        // 오버레이 권한 요청을 위한 인텐트 시작
                        val intent = Intent(
                            overlayPermission,
                            Uri.parse("package:${activity.packageName}")
                        )
                        activity.startActivityForResult(intent, 1001)
                    }
                    if (permissionsToRequest.isNotEmpty()) {
                        // 필요한 권한 요청
                        ActivityCompat.requestPermissions(
                            activity,
                            permissionsToRequest.toTypedArray(),
                            1000
                        )
                    }
                }
            }
        }
    }

    // 사용자의 권한 요청 응답을 처리하는 메소드
    fun handlePermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        // context를 Activity로 캐스팅
        val activity = context as? Activity ?: return

        if (requestCode == 1000) {
            var allPermissionsGranted = true

            // 요청한 권한에 대해서만 처리
            for (i in permissions.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            activity,
                            permissions[i]
                        )
                    ) {
                        Toast.makeText(activity, "권한이 필요합니다: ${permissions[i]}", Toast.LENGTH_LONG)
                            .show()
                    } else {
                        Toast.makeText(
                            activity,
                            "설정에서 권한을 활성화하세요: ${permissions[i]}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            if (!allPermissionsGranted) {
                Toast.makeText(
                    activity,
                    "필수 권한이 거부되었습니다. 앱의 기능을 사용하기 위해서는 권한이 필요합니다.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        if (requestCode == 1001) { // 오버레이 권한 처리
            if (!Settings.canDrawOverlays(activity)) {
                Toast.makeText(activity, "오버레이 권한이 거부되었습니다.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(activity, "오버레이 권한이 허용되었습니다.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
