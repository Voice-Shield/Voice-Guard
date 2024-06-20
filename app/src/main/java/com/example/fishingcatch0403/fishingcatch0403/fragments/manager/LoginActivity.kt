package com.example.fishingcatch0403.fishingcatch0403.fragments.manager

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fishingcatch0403.MainActivity
import com.example.fishingcatch0403.R
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // ID 변경에 따른 변수 이름 및 참조 업데이트
        val etEmail = findViewById<EditText>(R.id.emailEditText)
        val etPassword = findViewById<EditText>(R.id.passwordEditText)
        val btnLogin = findViewById<Button>(R.id.loginButton)

        btnLogin.setOnClickListener {
            // 로그인 성공 여부 확인 로직 추가 (예: 사용자 이메일, 비밀번호 확인)
            val isLoginSuccess = checkLogin(etEmail.text.toString(), etPassword.text.toString())

            if (isLoginSuccess) {
                // 로그인 성공 시 MainActivity로 이동하고 HomeFragment를 보여줌
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("showHomeFragment", true)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "로그인 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkLogin(email: String, password: String): Boolean {
        // 실제 로그인 로직 구현 (여기서는 단순화된 예시로 admin@example.com과 password)
        return email == "admin@example.com" && password == "password"
    }
}
