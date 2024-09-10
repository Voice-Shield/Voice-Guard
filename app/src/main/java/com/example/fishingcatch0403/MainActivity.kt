package com.example.fishingcatch0403

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.fishingcatch0403.databinding.ActivityMainBinding
import com.example.fishingcatch0403.dialer.CallTrackingManager
import com.example.fishingcatch0403.dialer.DialerManager

// 메인 액티비티 클래스. AppCompatActivity를 상속받습니다.
class MainActivity : AppCompatActivity() {

    // 필요한 권한들을 문자열로 선언합니다.
    private val recordPermission = android.Manifest.permission.RECORD_AUDIO
    private val statePermission = android.Manifest.permission.READ_PHONE_STATE
    private val audioPermission = android.Manifest.permission.READ_MEDIA_AUDIO
    private val contactPermission = android.Manifest.permission.READ_CONTACTS
    private val storagePermission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    private val readPermission = android.Manifest.permission.READ_EXTERNAL_STORAGE

    // 뷰 바인딩을 위한 변수 선언. 나중에 초기화됩니다.
    private lateinit var binding: ActivityMainBinding

    // 다이얼러 매니저와 콜 트래킹 매니저 객체를 선언합니다.
    private lateinit var dialerManager: DialerManager

    // 콜 트래킹 매니저 객체를 선언합니다.
    private lateinit var callTrackingManager: CallTrackingManager

    // 액티비티 런처를 선언합니다.
    private lateinit var activityLauncher: ActivityResultLauncher<Intent>

    // 액티비티가 생성될 때 호출되는 메소드.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 뷰 바인딩을 사용하여 레이아웃을 설정합니다.
        activityLauncher =
            registerForActivityResult(
                // 액티비티를 시작하고 결과를 처리하는 런처를 생성합니다.
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                initScreen()
                // 뷰 바인딩을 사용하여 레이아웃을 설정합니다.
            }

        // 객체 초기화
        dialerManager = DialerManager(this)
        callTrackingManager = CallTrackingManager(this)

        // 다이얼러 시작 여부 확인 후, 시작되지 않았다면 시작
        if (!callTrackingManager.isStartDialerCalled()) {
            // 다이얼러 시작
            dialerManager.startDialer(activityLauncher)
            // 다이얼러 시작 여부 저장
            callTrackingManager.markStartDialerCalled()
            // 다이얼러 시작 여부 로그 출력
            Log.d("dialer_start", "다이얼러 시작 여부: ${callTrackingManager.isStartDialerCalled()}")
        } else initScreen()
    }

    private fun initScreen() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 앱에 필요한 권한들을 확인하고 요청하는 메소드를 호출합니다.
        checkPermissions()

        // 네비게이션 컴포넌트를 설정합니다.
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.my_nav_host) as NavHostFragment
        val navController = navHostFragment.navController

        // 바텀 네비게이션과 네비게이션 컨트롤러를 연결합니다.
        NavigationUI.setupWithNavController(binding.menuBottomNavigation, navController)

        // 인텐트를 통해 특정 프래그먼트를 바로 보여줄지 결정합니다.
        if (intent.getBooleanExtra("showHomeFragment", false)) {
            navController.navigate(R.id.homeFragment)
        }
    }

    // 권한을 확인하고 요청하는 메소드.
    private fun checkPermissions() {
        // 각 권한이 승인되었는지 확인합니다.
        val isRecordOK = ContextCompat.checkSelfPermission(this, recordPermission) ==
                PackageManager.PERMISSION_GRANTED
        val isStateOK = ContextCompat.checkSelfPermission(this, statePermission) ==
                PackageManager.PERMISSION_GRANTED
        val isAudioOK = ContextCompat.checkSelfPermission(this, audioPermission) ==
                PackageManager.PERMISSION_GRANTED
        val isContactOK = ContextCompat.checkSelfPermission(this, contactPermission) ==
                PackageManager.PERMISSION_GRANTED
        val isStorageOK = ContextCompat.checkSelfPermission(this, storagePermission) ==
                PackageManager.PERMISSION_GRANTED
        val isReadOK = ContextCompat.checkSelfPermission(this, readPermission) ==
                PackageManager.PERMISSION_GRANTED

        // Android 버전에 따라 권한 요청을 다르게 처리합니다.
        when {
            // Android 13 이상
            SDK_INT >= 33 -> {
                if (!isRecordOK || !isAudioOK || !isStorageOK || !isStateOK || !isContactOK) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(
                            recordPermission,
                            audioPermission, // Android 13에서는 READ_MEDIA_AUDIO 권한 사용
                            statePermission,
                            contactPermission
                        ),
                        1000
                    )
                }
            }

            // Android 11 이상 (Android 13 미만)
            SDK_INT >= 30 -> {
                if (!isRecordOK || !isStorageOK || !isStateOK || !isContactOK) {
                    if (!Environment.isExternalStorageManager()) {
                        // Android 11 이상에서는 MANAGE_EXTERNAL_STORAGE 권한을 요청합니다.
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    } else {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(
                                recordPermission,
                                statePermission,
                                contactPermission
                            ),
                            1000
                        )
                    }
                }
            }

            // Android 10 이하
            else -> {
                if (!isRecordOK || !isStorageOK || !isReadOK || !isStateOK || !isContactOK) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(
                            recordPermission,
                            storagePermission,
                            readPermission,
                            statePermission,
                            contactPermission
                        ),
                        1000
                    )
                }
            }
        }
    }


    // 사용자의 권한 요청 응답을 처리하는 메소드.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // 요청 코드가 1000이고, 모든 권한이 승인되었는지 확인합니다.
        if (requestCode == 1000) {
            if (grantResults.size > 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED
            ) {
                // 모든 퍼미션 허용된 경우 처리
            } else {
                // 하나라도 거부된 권한이 있다면, 사용자에게 필요성을 알리는 토스트 메시지를 띄웁니다.
                Toast.makeText(this, "권한이 거부되었습니다. 앱의 기능을 사용하기 위해서는 권한이 필요합니다.", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }
}
