package com.example.fishingcatch0403

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.fishingcatch0403.databinding.ActivityMainBinding
import com.example.fishingcatch0403.dialer.CallTrackingManager
import com.example.fishingcatch0403.dialer.DialerManager
import com.example.fishingcatch0403.fishingcatch0403.fragments.manager.BatteryOptimizationHelper
import com.example.fishingcatch0403.manager.PermissionManager

// 메인 액티비티 클래스. AppCompatActivity를 상속받습니다.
class MainActivity : AppCompatActivity() {

    // 뷰 바인딩을 위한 변수 선언. 나중에 초기화됩니다.
    private lateinit var binding: ActivityMainBinding

    // 다이얼러 매니저와 콜 트래킹 매니저 객체를 선언합니다. (녹음 기능 완료 시 삭제 예정)
    private lateinit var dialerManager: DialerManager

    // 콜 트래킹 매니저 객체를 선언합니다. (녹음 기능 완료 시 삭제 예정)
    private lateinit var callTrackingManager: CallTrackingManager

    // 액티비티 런처를 선언합니다.
    private lateinit var activityLauncher: ActivityResultLauncher<Intent>

    // 권한 매니저 객체를 선언합니다.
    private lateinit var permissionManager: PermissionManager

    // 절전 모드 방지 도우미 객체를 선언합니다.
    private lateinit var batteryOptimizationHelper: BatteryOptimizationHelper

    // 액티비티가 생성될 때 호출되는 메소드.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 뷰 바인딩을 사용하여 레이아웃을 설정합니다.
        activityLauncher =
            registerForActivityResult(
                // 액티비티를 시작하고 결과를 처리하는 런처를 생성합니다.
                ActivityResultContracts.StartActivityForResult()
            ) { _ ->
                initScreen()
                // 뷰 바인딩을 사용하여 레이아웃을 설정합니다.
            }

        // 객체 초기화 (녹음 기능 완료 시 삭제 예정)
        dialerManager = DialerManager(this)
        callTrackingManager = CallTrackingManager(this)

        // 다이얼러 시작 여부 확인 후, 시작되지 않았다면 시작 (녹음 기능 완료 시 삭제 예정)
        if (!callTrackingManager.isStartDialerCalled()) {
            // 다이얼러 시작
            dialerManager.startDialer(activityLauncher)
            // 다이얼러 시작 여부 저장
            callTrackingManager.markStartDialerCalled()
            // 다이얼러 시작 여부 로그 출력
            Log.d("[APP] dialer_start", "다이얼러 시작 여부: ${callTrackingManager.isStartDialerCalled()}")
        } else initScreen()
    }

    private fun initScreen() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 앱에 필요한 권한들을 확인하고 요청하는 메소드를 호출합니다.
        permissionManager = PermissionManager(this)
        permissionManager.checkPermissions()

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

        // BatteryOptimizationHelper 인스턴스 생성
        batteryOptimizationHelper = BatteryOptimizationHelper(this)

        // 앱이 처음 실행되면 다이얼로그 표시
        if (batteryOptimizationHelper.isFirstRun()) {
            batteryOptimizationHelper.showBatteryOptimizationDialog()
        }
    }

    // 권한 요청 결과를 PermissionManager로 전달
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.handlePermissionsResult(requestCode, permissions, grantResults)
    }
}
