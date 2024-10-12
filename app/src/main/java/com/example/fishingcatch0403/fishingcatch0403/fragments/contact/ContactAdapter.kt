package com.example.fishingcatch0403.fishingcatch0403.fragments.contact

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fishingcatch0403.R

class ContactAdapter(
    private var contactList: List<Contact>,
    private val onContactClick: (Contact) -> Unit // 클릭 리스너를 위한 람다 함수 추가
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.textViewName)
        val numberTextView: TextView = itemView.findViewById(R.id.textViewNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contactList[position]
        holder.nameTextView.text = contact.name
        holder.numberTextView.text = contact.number

        holder.itemView.setOnClickListener {
            onContactClick(contact) // 클릭 시 리스너 호출
        }
    }

    override fun getItemCount() = contactList.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<Contact>) {
        contactList = newList
        notifyDataSetChanged()
    }
}
