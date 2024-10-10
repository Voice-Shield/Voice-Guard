package com.example.fishingcatch0403.fishingcatch0403.fragments.phishing_log

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.recyclerview.widget.RecyclerView
import com.example.fishingcatch0403.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationAdapter(
    private var notifications: MutableList<NotificationItem>,
    private val deleteCallback: (NotificationItem) -> Unit,
    private val notificationDao: NotificationDao
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.notification_title)
        val message: TextView = itemView.findViewById(R.id.notification_message)
        val icon: ImageView = itemView.findViewById(R.id.notification_icon)
        val deleteButton: AppCompatImageButton = itemView.findViewById(R.id.delete_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.title.text = "보이스 피싱 분석 결과"
        holder.message.text =
            "${notification.dateTime}\n${notification.phoneNumber}\n${notification.result}"

        // 아이콘 설정 (결과에 따라 변경)
        if (notification.resultType == 1) {
            holder.icon.setImageResource(R.drawable.alert) // 의심될 경우 아이콘
            holder.title.setTextColor(Color.WHITE) // 제목 색상 변경
            holder.message.setTextColor(Color.BLACK) // 텍스트 색상 변경
            holder.itemView.setBackgroundColor(Color.RED) // 배경색 적용
        } else {
            holder.icon.setImageResource(R.drawable.shield) // 안전 아이콘
            holder.title.setTextColor(Color.BLACK) // 제목 색상 변경
            holder.message.setTextColor(Color.WHITE) // 텍스트 색상 변경
            holder.itemView.setBackgroundColor(Color.GREEN) // 배경색 적용
        }

        // 삭제 버튼 클릭 시 알림 삭제
        holder.deleteButton.setOnClickListener {
            if (position in 0 until notifications.size) { // 인덱스 범위 확인
                val removedNotification = notifications[position] // 삭제할 항목을 저장
                CoroutineScope(Dispatchers.IO).launch {
                    notificationDao.deleteNotification(removedNotification) // DB에서 삭제
                }
                notifications.removeAt(position) // 리스트에서 삭제
                deleteCallback(removedNotification) // 콜백 호출

                notifyItemRemoved(position) // RecyclerView 갱신
                notifyItemRangeChanged(position, notifications.size) // 아이템 변경 범위 갱신
            } else {
                // 유효하지 않은 인덱스 로그 출력
                Log.e("[APP] NotificationAdapter", "유효하지 않은 인덱스입니다: $position")
            }
        }
    }

    override fun getItemCount(): Int = notifications.size
}

