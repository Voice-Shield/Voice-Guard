package com.example.fishingcatch0403.fishingcatch0403.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fishingcatch0403.R
import com.example.fishingcatch0403.fishingcatch0403.fragments.phishing_log.NotificationAdapter
import com.example.fishingcatch0403.fishingcatch0403.fragments.phishing_log.NotificationDao
import com.example.fishingcatch0403.fishingcatch0403.fragments.phishing_log.NotificationDatabase
import com.example.fishingcatch0403.fishingcatch0403.fragments.phishing_log.NotificationItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment() {

    private lateinit var notificationDao: NotificationDao
    private lateinit var notificationAdapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_setting, container, false)

        notificationDao = NotificationDatabase.getDatabase(requireContext()).notificationDao()

        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // 어댑터 초기화
        notificationAdapter = NotificationAdapter(emptyList()) { notification ->
            deleteNotification(notification)
        }
        recyclerView.adapter = notificationAdapter // 어댑터 설정

        // 알림 데이터 로드
        CoroutineScope(Dispatchers.IO).launch {
            val notifications = notificationDao.getAllNotifications()

            withContext(Dispatchers.Main) {
                notificationAdapter = NotificationAdapter(notifications) { notification ->
                    deleteNotification(notification)
                }
                recyclerView.adapter = notificationAdapter
            }
        }

        return view
    }

    // 알림 삭제 처리
    @SuppressLint("NotifyDataSetChanged")
    private fun deleteNotification(notification: NotificationItem) {
        CoroutineScope(Dispatchers.IO).launch {
            notificationDao.deleteNotification(notification)
            val notifications = notificationDao.getAllNotifications()

            withContext(Dispatchers.Main) {
                notificationAdapter = NotificationAdapter(notifications) { notification ->
                    deleteNotification(notification)
                }
                notificationAdapter.notifyDataSetChanged() // 데이터 갱신
            }
        }
    }
}

