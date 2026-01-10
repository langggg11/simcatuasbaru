package com.polstat.simcat.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.polstat.simcat.databinding.ActivityDashboardAdminBinding
import com.polstat.simcat.ui.auth.LoginActivity
import com.polstat.simcat.utils.SessionManager

class DashboardAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardAdminBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        val userEmail = sessionManager.getAuthToken()
        val userName = intent.getStringExtra("USER_NAME") ?: "Admin"

        binding.tvWelcome.text = "Selamat Datang, $userName"

        setupMenuListeners()
    }

    private fun setupMenuListeners() {
        binding.menuProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.menuEquipment.setOnClickListener {
            startActivity(Intent(this, EquipmentAdminActivity::class.java))
        }

        binding.menuBorrows.setOnClickListener {
            startActivity(Intent(this, BorrowListActivity::class.java))
        }

        binding.menuSchedules.setOnClickListener {
            startActivity(Intent(this, ScheduleListActivity::class.java))
        }

        binding.menuMonitoring.setOnClickListener {
            startActivity(Intent(this, MonitoringActivity::class.java))
        }



        binding.menuLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Keluar")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya") { _, _ ->
                sessionManager.clearSession()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}