package com.polstat.simcat.ui.main

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.polstat.simcat.R
import com.polstat.simcat.api.RetrofitClient
import com.polstat.simcat.auth.ChangePasswordRequest
import com.polstat.simcat.databinding.ActivityProfileBinding
import com.polstat.simcat.databinding.DialogChangePasswordBinding
import com.polstat.simcat.databinding.DialogEditProfileBinding
import com.polstat.simcat.model.User
import com.polstat.simcat.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var sessionManager: SessionManager
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupListeners()
        loadProfile()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getAuthToken()
                if (token.isNullOrEmpty()) {
                    Toast.makeText(this@ProfileActivity, "Session expired", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                val response = RetrofitClient.apiService.getProfile("Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    currentUser = response.body()
                    displayProfile(currentUser!!)
                } else {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Gagal memuat profil",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ProfileActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun displayProfile(user: User) {
        binding.tvName.text = user.name
        binding.tvEmail.text = user.email
        binding.tvPhone.text = user.phoneNumber ?: "-"
        binding.tvAddress.text = user.address ?: "-"

        // Show badge if admin
        if (user.role == "ADMIN") {
            binding.badgeAdmin.visibility = View.VISIBLE
            binding.tvMemberSince.text = "Admin sejak Des 2023"
        } else {
            binding.badgeAdmin.visibility = View.GONE
            binding.tvMemberSince.text = "Member sejak Jan 2024"
        }
    }

    private fun showEditProfileDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val dialogBinding = DialogEditProfileBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        // Pre-fill data
        currentUser?.let { user ->
            dialogBinding.etName.setText(user.name)
            dialogBinding.etEmail.setText(user.email)
            dialogBinding.etPhone.setText(user.phoneNumber)
            dialogBinding.etAddress.setText(user.address)
        }

        dialogBinding.btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            val name = dialogBinding.etName.text.toString().trim()
            val email = dialogBinding.etEmail.text.toString().trim()
            val phone = dialogBinding.etPhone.text.toString().trim()
            val address = dialogBinding.etAddress.text.toString().trim()

            if (validateEditProfile(name, email, phone)) {
                updateProfile(name, phone, address, dialog)
            }
        }

        dialog.show()

        // Memaksa dialog agar melebar penuh mengikuti margin di XML
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun validateEditProfile(name: String, email: String, phone: String): Boolean {
        if (name.isEmpty()) {
            Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return false
        }

        if (email.isEmpty()) {
            Toast.makeText(this, "Email tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return false
        }

        if (phone.isEmpty()) {
            Toast.makeText(this, "Nomor telepon tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun updateProfile(name: String, phone: String, address: String, dialog: Dialog) {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getAuthToken()
                val updatedUser = User(
                    id = currentUser?.id,
                    name = name,
                    email = currentUser?.email ?: "",
                    phoneNumber = phone,
                    address = address,
                    role = currentUser?.role
                )

                val response = RetrofitClient.apiService.updateProfile(
                    "Bearer $token",
                    updatedUser
                )

                if (response.isSuccessful && response.body() != null) {
                    currentUser = response.body()
                    displayProfile(currentUser!!)
                    Toast.makeText(
                        this@ProfileActivity,
                        "Profil berhasil diperbarui",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Gagal memperbarui profil",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ProfileActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showChangePasswordDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val dialogBinding = DialogChangePasswordBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            val oldPassword = dialogBinding.etOldPassword.text.toString().trim()
            val newPassword = dialogBinding.etNewPassword.text.toString().trim()
            val confirmPassword = dialogBinding.etConfirmPassword.text.toString().trim()

            if (validateChangePassword(oldPassword, newPassword, confirmPassword)) {
                changePassword(oldPassword, newPassword, confirmPassword, dialog)
            }
        }

        dialog.show()

        // Memaksa dialog agar lebar penuh
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun validateChangePassword(
        oldPassword: String,
        newPassword: String,
        confirmPassword: String
    ): Boolean {
        if (oldPassword.isEmpty()) {
            Toast.makeText(this, "Password lama tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword.isEmpty()) {
            Toast.makeText(this, "Password baru tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword.length < 6) {
            Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
            return false
        }

        if (confirmPassword.isEmpty()) {
            Toast.makeText(this, "Konfirmasi password tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "Password tidak cocok", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun changePassword(
        oldPassword: String,
        newPassword: String,
        confirmPassword: String,
        dialog: Dialog
    ) {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getAuthToken()
                val request = ChangePasswordRequest(
                    oldPassword = oldPassword,
                    newPassword = newPassword,
                    confirmPassword = confirmPassword
                )

                val response = RetrofitClient.apiService.changePassword(
                    "Bearer $token",
                    request
                )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Password berhasil diubah",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Gagal mengubah password. Password lama mungkin salah.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ProfileActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}