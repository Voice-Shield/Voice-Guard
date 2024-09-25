package com.example.fishingcatch0403.system_manager

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log

class ContactUtil {
    // 수신 전화번호가 주소록에 없는 번호인지 확인하는 함수
    fun isNumInContacts(context: Context, phoneNumber: String): Boolean {
        val contentResolver = context.contentResolver // ContentResolver 객체 생성
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI // 주소록 URI
        val projection = arrayOf( // 조회할 컬럼 목록
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        // 주소록의 모든 번호를 조회
        val cursor: Cursor? = contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )

        cursor?.use {
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            if (numberIndex == -1) {
                Log.e("[APP] ContactUtils", "주소록에서 번호 찾기 실패")
                return false
            }
            while (cursor.moveToNext()) {
                val contactNumber = cursor.getString(numberIndex) // 주소록의 번호
                // 전화번호를 동일한 형식으로 비교하기 위한 공백 및 하이픈 제거
                val normalizedContactNumber = contactNumber.replace(Regex("\\D"), "")
                val normalizedIncomingPhoneNumber = phoneNumber.replace(Regex("\\D"), "")
                // 로그로 비교한 번호 출력
                Log.d(
                    "[APP] ContactUtils",
                    "Comparing: $normalizedContactNumber with $normalizedIncomingPhoneNumber"
                )
                // 연락처에 저장된 번호와 수신된 번호가 동일한지 비교
                if (normalizedContactNumber == normalizedIncomingPhoneNumber) {
                    return true // 연락처에 저장된 번호와 수신 번호가 일치하면 true 반환
                }
            }
        }
        return false // 연락처에 저장된 번호와 일치하는 번호가 없다면 false 반환
    }
}
