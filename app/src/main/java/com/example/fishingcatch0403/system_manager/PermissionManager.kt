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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(val context: Context) {

    // 필요한 권한들을 문자열로 선언합니다.
    private val recordPermission = android.Manifest.permission.RECORD_AUDIO
    private val statePermission = android.Manifest.permission.READ_PHONE_STATE
    private val audioPermission = android.Manifest.permission.READ_MEDIA_AUDIO
    private val contactPermission = android.Manifest.permission.READ_CONTACTS
    private val storagePermission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    private val readPermission = android.Manifest.permission.READ_EXTERNAL_STORAGE
    private val readCallLogPermission = android.Manifest.permission.READ_CALL_LOG
    private val notificationPermission = android.Manifest.permission.POST_NOTIFICATIONS

    // 권한을 확인하고 요청하는 메소드
    fun checkPermissions() {
        // context를 Activity로 캐스팅
        val activity = context as? Activity ?: return

        val isRecordOK = ContextCompat.checkSelfPermission(activity, recordPermission) ==
                PackageManager.PERMISSION_GRANTED
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

        val permissionsToRequest = mutableListOf<String>()

        when {
            Build.VERSION.SDK_INT >= 33 -> {
                if (!isRecordOK || !isAudioOK || !isStorageOK || !isStateOK || !isContactOK || !isCallLogOK || !isNotificationOK) {
                    if (!isRecordOK) permissionsToRequest.add(recordPermission)
                    if (!isAudioOK) permissionsToRequest.add(audioPermission)
                    if (!isStateOK) permissionsToRequest.add(statePermission)
                    if (!isContactOK) permissionsToRequest.add(contactPermission)
                    if (!isCallLogOK) permissionsToRequest.add(readCallLogPermission)
                    if (!isNotificationOK) permissionsToRequest.add(notificationPermission)
                    if (permissionsToRequest.isNotEmpty()) {
                        ActivityCompat.requestPermissions(
                            activity,
                            permissionsToRequest.toTypedArray(),
                            1000
                        )
                    }
                }
            }

            Build.VERSION.SDK_INT >= 30 -> {
                if (!isRecordOK || !isStorageOK || !isStateOK || !isContactOK || !isReadOK) {
                    if (!Environment.isExternalStorageManager()) {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:${activity.packageName}")
                        activity.startActivity(intent)
                    } else {
                        if (!isRecordOK) permissionsToRequest.add(recordPermission)
                        if (!isStateOK) permissionsToRequest.add(statePermission)
                        if (!isContactOK) permissionsToRequest.add(contactPermission)
                        if (!isReadOK) permissionsToRequest.add(readPermission)
                        if (permissionsToRequest.isNotEmpty()) {
                            ActivityCompat.requestPermissions(
                                activity,
                                permissionsToRequest.toTypedArray(),
                                1000
                            )
                        }
                    }
                }
            }

            else -> {
                if (!isRecordOK || !isStorageOK || !isReadOK || !isStateOK || !isContactOK || !isCallLogOK) {
                    if (!isRecordOK) permissionsToRequest.add(recordPermission)
                    if (!isStorageOK) permissionsToRequest.add(storagePermission)
                    if (!isReadOK) permissionsToRequest.add(readPermission)
                    if (!isStateOK) permissionsToRequest.add(statePermission)
                    if (!isContactOK) permissionsToRequest.add(contactPermission)
                    if (!isCallLogOK) permissionsToRequest.add(readCallLogPermission)
                    if (permissionsToRequest.isNotEmpty()) {
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
    }
}
