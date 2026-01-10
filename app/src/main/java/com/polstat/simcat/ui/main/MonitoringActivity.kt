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
    private var userMap = mapOf<Long, String>() // ✅ Map untuk menyimpan Nama User
    private var currentFilter = "upcoming"
    private lateinit var adapter: MonitoringAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonitoringBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupUI()
        setupListeners()
        loadData()
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

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getAuthToken()

                // 1. Ambil Data User untuk Mapping Nama
                try {
                    val usersResponse = RetrofitClient.apiService.getAllUsers("Bearer $token")
                    if (usersResponse.isSuccessful && usersResponse.body() != null) {
                        userMap = usersResponse.body()!!.associate { it.id!! to it.name }
                    } else {
                        userMap = emptyMap()
                    }
                } catch (e: Exception) {
                    userMap = emptyMap()
                }

                // 2. Ambil Data Jadwal
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

    // Ganti fungsi isUpcoming() di MonitoringActivity dan MyActivitiesActivity

    private fun isUpcoming(schedule: Schedule): Boolean {
        return try {
            // Ambil bagian tanggal saja (sebelum spasi atau karakter pemisah)
            val dateStr = when {
                schedule.dateTime.contains("•") ->
                    schedule.dateTime.split("•").getOrNull(0)?.trim() ?: schedule.dateTime
                schedule.dateTime.contains(",") ->
                    schedule.dateTime.split(",").getOrNull(0)?.trim() ?: schedule.dateTime
                schedule.dateTime.contains(" ") ->
                    schedule.dateTime.split(" ").getOrNull(0)?.trim() ?: schedule.dateTime
                else -> schedule.dateTime
            }

            val formats = listOf(
                // Format dari database: 2025-12-15
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                // Format Indonesia: 15 Desember 2025
                SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")),
                // Format dengan garis miring: 15/12/2025
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
                // Format dengan strip: 15-12-2025
                SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            )

            var scheduleDate: Date? = null

            // Coba parsing dengan semua format
            for (format in formats) {
                try {
                    format.isLenient = false // Strict parsing
                    scheduleDate = format.parse(dateStr)
                    if (scheduleDate != null) {
                        break // Berhasil parsing, keluar dari loop
                    }
                } catch (e: Exception) {
                    continue // Coba format berikutnya
                }
            }

            // Jika parsing gagal semua, anggap sudah selesai
            if (scheduleDate == null) {
                return false
            }

            val currentDate = Date()

            // Bandingkan tanggal (ignore waktu)
            val calSchedule = Calendar.getInstance().apply { time = scheduleDate }
            val calCurrent = Calendar.getInstance().apply { time = currentDate }

            // Set waktu ke 00:00:00 untuk compare tanggal saja
            calSchedule.set(Calendar.HOUR_OF_DAY, 0)
            calSchedule.set(Calendar.MINUTE, 0)
            calSchedule.set(Calendar.SECOND, 0)
            calSchedule.set(Calendar.MILLISECOND, 0)

            calCurrent.set(Calendar.HOUR_OF_DAY, 0)
            calCurrent.set(Calendar.MINUTE, 0)
            calCurrent.set(Calendar.SECOND, 0)
            calCurrent.set(Calendar.MILLISECOND, 0)

            // Kegiatan dianggap "Akan Datang" jika tanggal kegiatan > hari ini
            calSchedule.time.after(calCurrent.time)

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun showDetailDialog(schedule: Schedule) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val dialogBinding = DialogDetailScheduleBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.8).toInt()
        val height = WindowManager.LayoutParams.WRAP_CONTENT

        dialog.window?.setLayout(width, height)
        dialog.window?.setGravity(Gravity.CENTER)

        with(dialogBinding) {
            tvTitle.text = schedule.title
            tvDateTime.text = schedule.dateTime
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
                val response = RetrofitClient.apiService.getParticipationsBySchedule("Bearer $token", schedule.id ?: 0)

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

                        layoutParticipants.removeAllViews()
                        participants.forEachIndexed { index, p ->
                            // Ambil nama dari map, fallback ke ID jika tidak ada
                            val participantName = userMap[p.userId] ?: "User ID: ${p.userId}"

                            val tv = android.widget.TextView(this@MonitoringActivity).apply {
                                text = "${index + 1}. $participantName"
                                setPadding(0, 4, 0, 4)
                                setTextColor(android.graphics.Color.BLACK)
                                textSize = 14f
                            }
                            layoutParticipants.addView(tv)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}