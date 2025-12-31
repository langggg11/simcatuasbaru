package com.polstat.simcat.ui.main

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.polstat.simcat.adapter.MonitoringAdapter
import com.polstat.simcat.api.RetrofitClient
import com.polstat.simcat.databinding.ActivityMonitoringBinding
import com.polstat.simcat.databinding.DialogDetailScheduleBinding
import com.polstat.simcat.model.Schedule
import com.polstat.simcat.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MonitoringActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMonitoringBinding
    private lateinit var sessionManager: SessionManager
    private var allSchedules = listOf<Schedule>()
    private var currentFilter = "upcoming"
    private lateinit var adapter: MonitoringAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonitoringBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupUI()
        setupListeners()
        loadSchedules()
    }

    private fun setupUI() {
        binding.rvSchedules.layoutManager = LinearLayoutManager(this)
        adapter = MonitoringAdapter(
            emptyList(),
            onDetailClick = { schedule -> showDetailDialog(schedule) }
        )
        binding.rvSchedules.adapter = adapter
        updateTabSelection("upcoming")
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.tabAkanDatang.setOnClickListener {
            currentFilter = "upcoming"
            updateTabSelection("upcoming")
            filterSchedules()
        }

        binding.tabSelesai.setOnClickListener {
            currentFilter = "completed"
            updateTabSelection("completed")
            filterSchedules()
        }

        binding.tabSemua.setOnClickListener {
            currentFilter = "all"
            updateTabSelection("all")
            filterSchedules()
        }
    }

    private fun updateTabSelection(selected: String) {
        // Reset semua background
        binding.tabAkanDatang.setBackgroundResource(com.polstat.simcat.R.drawable.bg_tab_unselected)
        binding.tabSelesai.setBackgroundResource(com.polstat.simcat.R.drawable.bg_tab_unselected)
        binding.tabSemua.setBackgroundResource(com.polstat.simcat.R.drawable.bg_tab_unselected)

        // Reset semua font
        binding.tabAkanDatang.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        binding.tabSelesai.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        binding.tabSemua.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)

        // Set yang aktif
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

    private fun loadSchedules() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getAllSchedules()

                if (response.isSuccessful && response.body() != null) {
                    allSchedules = response.body()!!
                    filterSchedules()
                } else {
                    Toast.makeText(this@MonitoringActivity, "Gagal memuat jadwal", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MonitoringActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun filterSchedules() {
        val filtered = when (currentFilter) {
            "upcoming" -> allSchedules.filter { isUpcoming(it) }
            "completed" -> allSchedules.filter { !isUpcoming(it) }
            else -> allSchedules
        }

        adapter.updateData(filtered)
    }

    private fun isUpcoming(schedule: Schedule): Boolean {
        return try {
            val dateStr = schedule.dateTime.split("•").getOrNull(0)?.trim() ?: return true
            val formatter = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
            val scheduleDate = formatter.parse(dateStr)
            val currentDate = Date()

            scheduleDate?.after(currentDate) ?: true
        } catch (e: Exception) {
            true
        }
    }

    private fun showDetailDialog(schedule: Schedule) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val dialogBinding = DialogDetailScheduleBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        // Atur ukuran dialog menjadi 80% lebar layar
        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.8).toInt()
        val height = WindowManager.LayoutParams.WRAP_CONTENT

        dialog.window?.setLayout(width, height)
        dialog.window?.setGravity(Gravity.CENTER)

        with(dialogBinding) {
            tvTitle.text = schedule.title
            tvDate.text = schedule.dateTime.split("•").getOrNull(0)?.trim() ?: schedule.dateTime
            tvTime.text = schedule.dateTime.split("•").getOrNull(1)?.trim() ?: ""
            tvLocation.text = schedule.location
            tvDescription.text = schedule.deskripsi ?: "-"

            when (schedule.tipeKegiatan.uppercase()) {
                "LATIHAN" -> {
                    tvTypeBadge.text = "Latihan"
                    tvTypeBadge.setBackgroundResource(com.polstat.simcat.R.drawable.bg_badge_latihan)
                }
                "TURNAMEN" -> {
                    tvTypeBadge.text = "Turnamen"
                    tvTypeBadge.setBackgroundResource(com.polstat.simcat.R.drawable.bg_badge_turnamen)
                }
                "RAPAT" -> {
                    tvTypeBadge.text = "Rapat"
                    tvTypeBadge.setBackgroundResource(com.polstat.simcat.R.drawable.bg_badge_rapat)
                }
            }

            val isCompleted = !isUpcoming(schedule)
            if (isCompleted) {
                tvStatusBadge.text = "Selesai"
                tvStatusBadge.visibility = View.VISIBLE
                tvStatusText.text = "Kegiatan telah selesai"
                progressBar.visibility = View.GONE
            } else {
                tvStatusBadge.visibility = View.GONE
                progressBar.visibility = View.VISIBLE
            }

            btnClose.setOnClickListener { dialog.dismiss() }
            btnTutup.setOnClickListener { dialog.dismiss() }

            loadParticipants(schedule, dialogBinding)
        }

        dialog.show()
    }

    private fun loadParticipants(schedule: Schedule, dialogBinding: DialogDetailScheduleBinding) {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getAuthToken()
                val response = RetrofitClient.apiService.getParticipationsBySchedule(
                    "Bearer $token",
                    schedule.id ?: 0
                )

                if (response.isSuccessful && response.body() != null) {
                    val participants = response.body()!!.filter { it.status.uppercase() == "REGISTERED" }
                    val participantCount = participants.size
                    val maxParticipants = schedule.maxParticipants ?: 0

                    with(dialogBinding) {
                        if (maxParticipants > 0) {
                            tvStatusKuota.text = "$participantCount / $maxParticipants"
                            val slotsLeft = maxParticipants - participantCount
                            tvSlotTersisa.text = "$slotsLeft slot tersisa"
                            progressBar.max = maxParticipants
                            progressBar.progress = participantCount
                        } else {
                            tvStatusKuota.text = "$participantCount peserta"
                            tvSlotTersisa.text = "Tidak dibatasi"
                            progressBar.visibility = View.GONE
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}