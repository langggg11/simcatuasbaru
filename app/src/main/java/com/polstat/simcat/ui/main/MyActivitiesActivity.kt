package com.polstat.simcat.ui.main

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.polstat.simcat.adapter.MyActivitiesAdapter
import com.polstat.simcat.api.RetrofitClient
import com.polstat.simcat.databinding.ActivityMyActivitiesBinding
import com.polstat.simcat.databinding.DialogCancelParticipationBinding
import com.polstat.simcat.model.Participation
import com.polstat.simcat.model.Schedule
import com.polstat.simcat.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MyActivitiesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyActivitiesBinding
    private lateinit var sessionManager: SessionManager
    private var allParticipations = listOf<Pair<Participation, Schedule>>()
    private var currentFilter = "upcoming"
    private lateinit var adapter: MyActivitiesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyActivitiesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupUI()
        setupListeners()
        loadMyActivities()
    }

    private fun setupUI() {
        binding.rvActivities.layoutManager = LinearLayoutManager(this)
        adapter = MyActivitiesAdapter(
            emptyList(),
            onCancelClick = { participation, schedule ->
                showCancelDialog(participation, schedule)
            }
        )
        binding.rvActivities.adapter = adapter
        updateTabSelection("upcoming")
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnLihatJadwal.setOnClickListener {
            val intent = Intent(this, ScheduleListActivity::class.java)
            startActivity(intent)
        }

        binding.tabAkanDatang.setOnClickListener {
            currentFilter = "upcoming"
            updateTabSelection("upcoming")
            filterActivities()
        }

        binding.tabSelesai.setOnClickListener {
            currentFilter = "completed"
            updateTabSelection("completed")
            filterActivities()
        }

        binding.tabSemua.setOnClickListener {
            currentFilter = "all"
            updateTabSelection("all")
            filterActivities()
        }
    }

    private fun updateTabSelection(selected: String) {
        binding.tabAkanDatang.setBackgroundResource(com.polstat.simcat.R.drawable.bg_tab_unselected)
        binding.tabSelesai.setBackgroundResource(com.polstat.simcat.R.drawable.bg_tab_unselected)
        binding.tabSemua.setBackgroundResource(com.polstat.simcat.R.drawable.bg_tab_unselected)

        binding.tabAkanDatang.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        binding.tabSelesai.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        binding.tabSemua.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)

        when (selected) {
            "upcoming" -> {
                binding.tabAkanDatang.setBackgroundResource(com.polstat.simcat.R.drawable.bg_tab_selected)
                binding.tabAkanDatang.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
            }
            "completed" -> {
                binding.tabSelesai.setBackgroundResource(com.polstat.simcat.R.drawable.bg_tab_selected)
                binding.tabSelesai.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
            }
            "all" -> {
                binding.tabSemua.setBackgroundResource(com.polstat.simcat.R.drawable.bg_tab_selected)
                binding.tabSemua.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
            }
        }
    }

    private fun loadMyActivities() {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getAuthToken()
                val userId = sessionManager.getUserId()

                val participationsResponse = RetrofitClient.apiService.getParticipationsByUser(
                    "Bearer $token",
                    userId
                )

                if (!participationsResponse.isSuccessful || participationsResponse.body() == null) {
                    showEmptyState()
                    return@launch
                }

                val participations = participationsResponse.body()!!
                    .filter { it.status.uppercase() == "REGISTERED" }

                if (participations.isEmpty()) {
                    showEmptyState()
                    return@launch
                }

                val schedulesResponse = RetrofitClient.apiService.getAllSchedules()

                if (!schedulesResponse.isSuccessful || schedulesResponse.body() == null) {
                    showEmptyState()
                    return@launch
                }

                val schedules = schedulesResponse.body()!!

                allParticipations = participations.mapNotNull { participation ->
                    val schedule = schedules.find { it.id == participation.scheduleId }
                    if (schedule != null) {
                        Pair(participation, schedule)
                    } else null
                }

                if (allParticipations.isEmpty()) {
                    showEmptyState()
                } else {
                    filterActivities()
                }

            } catch (e: Exception) {
                Toast.makeText(this@MyActivitiesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
                showEmptyState()
            }
        }
    }

    private fun filterActivities() {
        val filtered = when (currentFilter) {
            "upcoming" -> allParticipations.filter { isUpcoming(it.second) }
            "completed" -> allParticipations.filter { !isUpcoming(it.second) }
            else -> allParticipations
        }

        if (filtered.isEmpty()) {
            showEmptyState()
        } else {
            binding.emptyState.visibility = View.GONE
            binding.rvActivities.visibility = View.VISIBLE
            adapter.updateData(filtered)
        }
    }

    private fun isUpcoming(schedule: Schedule): Boolean {
        return try {
            val dateStr = if (schedule.dateTime.contains("•")) {
                schedule.dateTime.split("•").getOrNull(0)?.trim() ?: schedule.dateTime
            } else if (schedule.dateTime.contains(",")) {
                schedule.dateTime.split(",").getOrNull(0)?.trim() ?: schedule.dateTime
            } else {
                schedule.dateTime
            }

            val formatter = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
            val scheduleDate = formatter.parse(dateStr)
            val currentDate = Date()

            scheduleDate?.after(currentDate) ?: true
        } catch (e: Exception) {
            true
        }
    }

    private fun showEmptyState() {
        binding.emptyState.visibility = View.VISIBLE
        binding.rvActivities.visibility = View.GONE
    }

    private fun showCancelDialog(participation: Participation, schedule: Schedule) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val dialogBinding = DialogCancelParticipationBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        with(dialogBinding) {
            tvScheduleName.text = schedule.title
            tvDateTime.text = schedule.dateTime

            btnClose.setOnClickListener { dialog.dismiss() }
            btnTidak.setOnClickListener { dialog.dismiss() }

            btnYaBatalkan.setOnClickListener {
                val reason = etReason.text.toString().trim()
                if (reason.isEmpty()) {
                    Toast.makeText(this@MyActivitiesActivity, "Mohon isi alasan pembatalan", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                cancelParticipation(participation, dialog)
            }
        }

        dialog.show()
    }

    private fun cancelParticipation(participation: Participation, dialog: Dialog) {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getAuthToken()
                val updatedParticipation = participation.copy(status = "CANCELLED")

                val response = RetrofitClient.apiService.cancelParticipation(
                    "Bearer $token",
                    participation.id ?: 0,
                    updatedParticipation
                )

                // ✅ Perbaikan: Sukses jika HTTP 200 (tanpa parse JSON response)
                if (response.isSuccessful) {
                    Toast.makeText(this@MyActivitiesActivity, "Pendaftaran berhasil dibatalkan", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadMyActivities()
                } else {
                    Toast.makeText(this@MyActivitiesActivity, "Gagal membatalkan pendaftaran", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MyActivitiesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }
}