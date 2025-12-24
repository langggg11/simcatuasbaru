package com.polstat.simcat.ui.auth

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.polstat.simcat.api.RetrofitClient
import com.polstat.simcat.databinding.ActivityRegisterBinding
import com.polstat.simcat.model.User
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (validateInput(name, email, phone, password, confirmPassword)) {
                registerUser(name, email, phone, password)
            }
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun validateInput(
        name: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        if (name.isEmpty()) {
            binding.etName.error = "Nama tidak boleh kosong"
            binding.etName.requestFocus()
            return false
        }

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

        if (phone.isEmpty()) {
            binding.etPhone.error = "Nomor telepon tidak boleh kosong"
            binding.etPhone.requestFocus()
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

        if (confirmPassword.isEmpty()) {
            binding.etConfirmPassword.error = "Konfirmasi password tidak boleh kosong"
            binding.etConfirmPassword.requestFocus()
            return false
        }

        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Password tidak cocok"
            binding.etConfirmPassword.requestFocus()
            return false
        }

        return true
    }

    private fun registerUser(name: String, email: String, phone: String, password: String) {
        binding.btnRegister.isEnabled = false

        lifecycleScope.launch {
            try {
                val user = User(
                    name = name,
                    email = email,
                    password = password,
                    phoneNumber = phone,
                    role = "MEMBER"
                )

                val response = RetrofitClient.apiService.register(user)

                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Registrasi berhasil! Silakan login",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()

                } else {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Registrasi gagal: ${response.message()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@RegisterActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            } finally {
                binding.btnRegister.isEnabled = true
            }
        }
    }
}