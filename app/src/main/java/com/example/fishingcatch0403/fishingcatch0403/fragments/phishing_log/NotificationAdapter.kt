package com.example.fishingcatch0403.fishingcatch0403.fragments.phishing_log

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fishingcatch0403.R

class NotificationAdapter(
    private val notifications: List<NotificationItem>,
    private val deleteCallback: (NotificationItem) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.notification_title)
        val message: TextView = itemView.findViewById(R.id.notification_message)
        val icon: ImageView = itemView.findViewById(R.id.notification_icon)
        val deleteButton: Button = itemView.findViewById(R.id.delete_button)
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
        } else {
            holder.icon.setImageResource(R.drawable.shield) // 안전 아이콘
        }

        // 삭제 버튼 클릭 시 알림 삭제
        holder.deleteButton.setOnClickListener {
            deleteCallback(notification)
        }
    }

    override fun getItemCount(): Int = notifications.size
}

