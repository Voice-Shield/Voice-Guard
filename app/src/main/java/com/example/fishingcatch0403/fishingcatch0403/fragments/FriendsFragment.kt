package com.example.fishingcatch0403.fishingcatch0403.fragments

import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fishingcatch0403.databinding.FragmentFriendsBinding
import com.example.fishingcatch0403.fishingcatch0403.fragments.contact.Contact
import com.example.fishingcatch0403.fishingcatch0403.fragments.contact.ContactAdapter

class FriendsFragment : Fragment() {

    private var mBinding: FragmentFriendsBinding? = null
    private val requestPermission = 100
    private lateinit var adapter: ContactAdapter
    private var contactList: List<Contact> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentFriendsBinding.inflate(inflater, container, false)
        mBinding = binding

        // RecyclerView에 LayoutManager 설정
        binding.recyclerViewContacts.layoutManager = LinearLayoutManager(requireContext())

        // 연락처 권한 확인 및 요청
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.READ_CONTACTS),
                requestPermission
            )
        } else {
            loadContacts()
        }

        // 검색창에서 입력한 텍스트에 따라 연락처 목록을 필터링
        binding.searchViewContacts.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterContacts(newText)
                return true
            }
        })

        return mBinding?.root
    }

    // 권한 요청 결과 처리
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestPermission && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadContacts()
        }
    }

    // 연락처 목록을 불러와서 RecyclerView에 표시
    private fun loadContacts() {
        val contacts = mutableListOf<Contact>()
        val contentResolver = requireContext().contentResolver
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null, null, null, null
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val name = it.getString(nameIndex)
                val number = it.getString(numberIndex)
                contacts.add(Contact(name, number))
            }
        }

        contacts.sortBy { it.name } // 연락처 이름으로 정렬
        contactList = contacts  // 전체 연락처 목록 저장
        adapter = ContactAdapter(contacts) { contact ->
            showContactOptions(contact) // 클릭 시 옵션 보여주기
        }
        mBinding?.recyclerViewContacts?.adapter = adapter
    }

    // 검색된 연락처 목록을 필터링하는 함수
    private fun filterContacts(query: String?) {
        val filteredList = if (query.isNullOrEmpty()) {
            contactList // 검색어가 없으면 전체 리스트
        } else {
            contactList.filter {
                it.name.contains(query, ignoreCase = true) // 연락처 이름에 검색어 포함 여부 확인
            }
        }
        adapter.updateList(filteredList) // 어댑터에 업데이트된 리스트 적용
    }

    // 연락처 클릭 시 옵션을 보여주는 함수
    private fun showContactOptions(contact: Contact) {
        val options = arrayOf("전화 걸기", "문자 보내기")

        AlertDialog.Builder(requireContext())
            .setTitle(contact.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> makePhoneCall(contact.number) // 전화 걸기 선택 시
                    1 -> sendSms(contact.number) // 문자 보내기 선택 시
                }
            }
            .show()
    }

    // 전화를 거는 함수
    private fun makePhoneCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        startActivity(intent)
    }

    // 문자를 보내는 함수
    private fun sendSms(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("sms:$phoneNumber")
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        mBinding = null
        super.onDestroyView()
    }
}
