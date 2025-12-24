package com.polstat.simcat.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.polstat.simcat.api.RetrofitClient
import com.polstat.simcat.auth.AuthRequest
import com.polstat.simcat.databinding.ActivityLoginBinding
import com.polstat.simcat.ui.main.DashboardAdminActivity
import com.polstat.simcat.ui.main.DashboardMemberActivity
import com.polstat.simcat.utils.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Check if already logged in
        if (sessionManager.isLoggedIn()) {
            navigateToDashboard(sessionManager.getUserRole() ?: "MEMBER", "")
            return
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInput(email, password)) {
                loginUser(email, password)
            }
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "Email tidak boleh kosong"
            binding.etEmail.requestFocus()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Format email tidak valid"
            binding.etEmail.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Password tidak boleh kosong"
            binding.etPassword.requestFocus()
            return false
        }

        if (password.length < 6) {
            binding.etPassword.error = "Password minimal 6 karakter"
            binding.etPassword.requestFocus()
            return false
        }

        return true
    }

    private fun loginUser(email: String, password: String) {
        binding.btnLogin.isEnabled = false

        lifecycleScope.launch {
            try {
                val request = AuthRequest(email, password)
                val response = RetrofitClient.apiService.login(request)

                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!

                    // Save token
                    sessionManager.saveAuthToken(authResponse.accessToken ?: "")

                    // Get user profile to get userId and name
                    val profileResponse = RetrofitClient.apiService.getProfile(
                        "Bearer ${authResponse.accessToken}"
                    )

                    if (profileResponse.isSuccessful && profileResponse.body() != null) {
                        val user = profileResponse.body()!!
                        sessionManager.saveUserData(
                            email = user.email,
                            role = user.role ?: "MEMBER",
                            userId = user.id ?: -1L
                        )

                        Toast.makeText(
                            this@LoginActivity,
                            "Login berhasil!",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Navigate based on role
                        navigateToDashboard(user.role ?: "MEMBER", user.name)
                    }

                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Login gagal: Email atau password salah",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            } finally {
                binding.btnLogin.isEnabled = true
            }
        }
    }

    private fun navigateToDashboard(role: String, userName: String) {
        val intent = if (role == "ADMIN") {
            Intent(this, DashboardAdminActivity::class.java)
        } else {
            Intent(this, DashboardMemberActivity::class.java)
        }

        intent.putExtra("USER_NAME", userName)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}