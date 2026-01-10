package com.polstat.simcat.ui.main

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.polstat.simcat.adapter.ScheduleAdminAdapter
import com.polstat.simcat.adapter.ScheduleMemberAdapter
import com.polstat.simcat.api.RetrofitClient
import com.polstat.simcat.databinding.ActivityScheduleListBinding
import com.polstat.simcat.databinding.DialogAddScheduleBinding
import com.polstat.simcat.databinding.DialogJoinScheduleBinding
import com.polstat.simcat.model.Participation
import com.polstat.simcat.model.Schedule
import com.polstat.simcat.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ScheduleListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScheduleListBinding
    private lateinit var sessionManager: SessionManager
    private var scheduleList = listOf<Schedule>()
    private var joinedScheduleIds = mutableSetOf<Long>()
    private var isAdmin = false
    private lateinit var memberAdapter: ScheduleMemberAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        isAdmin = sessionManager.getUserRole() == "ADMIN"

        setupUI()
        setupListeners()
        loadSchedules()
    }

    private fun setupUI() {
        if (isAdmin) {
            binding.tvSubtitle.text = "Kelola kegiatan UKM Catur"
            binding.btnAdd.visibility = View.VISIBLE
        } else {
            binding.tvSubtitle.text = "Kegiatan yang akan datang"
            binding.btnAdd.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        if (isAdmin) {
            binding.btnAdd.setOnClickListener {
                showAddDialog()
            }
        }
    }

    private fun loadSchedules() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getAllSchedules()

                if (response.isSuccessful && response.body() != null) {
                    scheduleList = response.body()!!

                    // Load joined schedules for member
                    if (!isAdmin) {
                        loadJoinedSchedules()
                    } else {
                        setupRecyclerView()
                    }
                } else {
                    Toast.makeText(
                        this@ScheduleListActivity,
                        "Gagal memuat jadwal",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ScheduleListActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            }
        }
    }

    private fun loadJoinedSchedules() {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getAuthToken()
                val userId = sessionManager.getUserId()

                val response = RetrofitClient.apiService.getParticipationsByUser(
                    "Bearer $token",
                    userId
                )

                if (response.isSuccessful && response.body() != null) {
                    val participations = response.body()!!
                    joinedScheduleIds = participations
                        .filter { it.status.uppercase() == "REGISTERED" }
                        .mapNotNull { it.scheduleId }
                        .toMutableSet()
                }

                setupRecyclerView()
            } catch (e: Exception) {
                e.printStackTrace()
                setupRecyclerView()
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvSchedules.layoutManager = LinearLayoutManager(this)

        if (isAdmin) {
            val adapter = ScheduleAdminAdapter(
                scheduleList,
                onEditClick = { schedule -> showEditDialog(schedule) },
                onDeleteClick = { schedule -> showDeleteDialog(schedule) }
            )
            binding.rvSchedules.adapter = adapter
        } else {
            memberAdapter = ScheduleMemberAdapter(
                scheduleList,
                joinedScheduleIds,
                onJoinClick = { schedule -> showJoinDialog(schedule) }
            )
            binding.rvSchedules.adapter = memberAdapter
        }
    }

    private fun showAddDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val dialogBinding = DialogAddScheduleBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        // Atur ukuran dialog menjadi 80% lebar layar
        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.8).toInt()
        val height = WindowManager.LayoutParams.WRAP_CONTENT

        dialog.window?.setLayout(width, height)
        dialog.window?.setGravity(Gravity.CENTER)

        // Setup spinner
        val types = arrayOf("Latihan", "Turnamen", "Rapat")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerType.adapter = spinnerAdapter

        with(dialogBinding) {
            tvTitle.text = "Tambah Kegiatan"
            btnSave.text = "Tambah Kegiatan"

            btnClose.setOnClickListener {
                dialog.dismiss()
            }

            btnSave.setOnClickListener {
                val name = etName.text.toString().trim()
                val type = spinnerType.selectedItem.toString().uppercase()
                val dateTime = etDateTime.text.toString().trim()
                val location = etLocation.text.toString().trim()
                val maxParticipantsStr = etMaxParticipants.text.toString().trim()
                val description = etDescription.text.toString().trim()

                if (validateInput(name, dateTime, location, maxParticipantsStr)) {
                    val schedule = Schedule(
                        title = name,
                        tipeKegiatan = type,
                        dateTime = dateTime,
                        location = location,
                        maxParticipants = maxParticipantsStr.toIntOrNull() ?: 0,
                        deskripsi = description
                    )
                    createSchedule(schedule, dialog)
                }
            }
        }

        dialog.show()
    }

    private fun showEditDialog(schedule: Schedule) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val dialogBinding = DialogAddScheduleBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        // Atur ukuran dialog menjadi 80% lebar layar
        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.8).toInt()
        val height = WindowManager.LayoutParams.WRAP_CONTENT

        dialog.window?.setLayout(width, height)
        dialog.window?.setGravity(Gravity.CENTER)

        // Setup spinner
        val types = arrayOf("Latihan", "Turnamen", "Rapat")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerType.adapter = spinnerAdapter

        with(dialogBinding) {
            tvTitle.text = "Edit Kegiatan"
            btnSave.text = "Simpan Perubahan"

            // Pre-fill data
            etName.setText(schedule.title)

            val typeIndex = types.indexOfFirst { it.uppercase() == schedule.tipeKegiatan.uppercase() }
            if (typeIndex >= 0) spinnerType.setSelection(typeIndex)

            etDateTime.setText(schedule.dateTime)
            etLocation.setText(schedule.location)
            etMaxParticipants.setText(schedule.maxParticipants?.toString() ?: "0")
            etDescription.setText(schedule.deskripsi ?: "")

            btnClose.setOnClickListener {
                dialog.dismiss()
            }

            btnSave.setOnClickListener {
                val name = etName.text.toString().trim()
                val type = spinnerType.selectedItem.toString().uppercase()
                val dateTime = etDateTime.text.toString().trim()
                val location = etLocation.text.toString().trim()
                val maxParticipantsStr = etMaxParticipants.text.toString().trim()
                val description = etDescription.text.toString().trim()

                if (validateInput(name, dateTime, location, maxParticipantsStr)) {
                    val updatedSchedule = Schedule(
                        id = schedule.id,
                        title = name,
                        tipeKegiatan = type,
                        dateTime = dateTime,
                        location = location,
                        maxParticipants = maxParticipantsStr.toIntOrNull() ?: 0,
                        deskripsi = description
                    )
                    updateSchedule(updatedSchedule, dialog)
                }
            }
        }

        dialog.show()
    }

    private fun showDeleteDialog(schedule: Schedule) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Kegiatan")
            .setMessage("Apakah Anda yakin ingin menghapus ${schedule.title}?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteSchedule(schedule)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun isSchedulePast(schedule: Schedule): Boolean {
        return try {
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
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")),
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
                SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            )

            var scheduleDate: Date? = null
            for (format in formats) {
                try {
                    format.isLenient = false
                    scheduleDate = format.parse(dateStr)
                    if (scheduleDate != null) break
                } catch (e: Exception) {
                    continue
                }
            }

            if (scheduleDate == null) return false

            val currentDate = Date()
            val calSchedule = Calendar.getInstance().apply {
                time = scheduleDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val calCurrent = Calendar.getInstance().apply {
                time = currentDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            !calSchedule.time.after(calCurrent.time)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    private fun showJoinDialog(schedule: Schedule) {
        if (isSchedulePast(schedule)) {
            Toast.makeText(
                this,
                "Tidak dapat mendaftar. Kegiatan ini sudah selesai.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val dialogBinding = DialogJoinScheduleBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        // Atur ukuran dialog menjadi 80% lebar layar
        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.8).toInt()
        val height = WindowManager.LayoutParams.WRAP_CONTENT

        dialog.window?.setLayout(width, height)
        dialog.window?.setGravity(Gravity.CENTER)

        with(dialogBinding) {
            tvScheduleName.text = schedule.title
            tvDateTime.text = schedule.dateTime
            tvLocation.text = schedule.location
            tvDescription.text = schedule.deskripsi ?: "-"

            if (schedule.maxParticipants != null && schedule.maxParticipants > 0) {
                tvParticipants.text = "0 / ${schedule.maxParticipants} orang"
            } else {
                tvParticipants.text = "Tidak dibatasi"
            }

            btnClose.setOnClickListener {
                dialog.dismiss()
            }

            btnConfirm.setOnClickListener {
                if (isSchedulePast(schedule)) {
                    Toast.makeText(
                        this@ScheduleListActivity,
                        "Tidak dapat mendaftar. Kegiatan ini sudah selesai.",
                        Toast.LENGTH_LONG
                    ).show()
                    dialog.dismiss()
                    return@setOnClickListener
                }
                joinSchedule(schedule, dialog)
            }
        }

        dialog.show()
    }

    private fun validateInput(
        name: String,
        dateTime: String,
        location: String,
        maxParticipants: String
    ): Boolean {
        if (name.isEmpty()) {
            Toast.makeText(this, "Nama kegiatan tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return false
        }
        if (dateTime.isEmpty()) {
            Toast.makeText(this, "Tanggal & waktu tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return false
        }
        if (location.isEmpty()) {
            Toast.makeText(this, "Lokasi tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return false
        }
        if (maxParticipants.isEmpty() || maxParticipants.toIntOrNull() == null) {
            Toast.makeText(this, "Max peserta tidak valid", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun createSchedule(schedule: Schedule, dialog: Dialog) {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getAuthToken()
                val response = RetrofitClient.apiService.createSchedule(
                    "Bearer $token",
                    schedule
                )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@ScheduleListActivity,
                        "Kegiatan berhasil ditambahkan",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                    loadSchedules()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Gagal menambahkan kegiatan"
                    Toast.makeText(
                        this@ScheduleListActivity,
                        errorMsg,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ScheduleListActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            }
        }
    }

    private fun updateSchedule(schedule: Schedule, dialog: Dialog) {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getAuthToken()
                val response = RetrofitClient.apiService.updateSchedule(
                    "Bearer $token",
                    schedule.id ?: 0,
                    schedule
                )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@ScheduleListActivity,
                        "Kegiatan berhasil diperbarui",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                    loadSchedules()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Gagal memperbarui kegiatan"
                    Toast.makeText(
                        this@ScheduleListActivity,
                        errorMsg,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ScheduleListActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            }
        }
    }

    private fun deleteSchedule(schedule: Schedule) {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getAuthToken()
                val response = RetrofitClient.apiService.deleteSchedule(
                    "Bearer $token",
                    schedule.id ?: 0
                )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@ScheduleListActivity,
                        "Kegiatan berhasil dihapus",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadSchedules()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Gagal menghapus kegiatan"
                    Toast.makeText(
                        this@ScheduleListActivity,
                        errorMsg,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ScheduleListActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            }
        }
    }

    private fun joinSchedule(schedule: Schedule, dialog: Dialog) {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getAuthToken()
                val userId = sessionManager.getUserId()

                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val currentDate = dateFormat.format(Date())

                val participation = Participation(
                    userId = userId,
                    scheduleId = schedule.id ?: 0,
                    registrationDate = currentDate,
                    status = "REGISTERED"
                )

                val response = RetrofitClient.apiService.registerParticipation(
                    "Bearer $token",
                    participation
                )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@ScheduleListActivity,
                        "Berhasil mendaftar kegiatan!",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()

                    // Add to joined list and update UI
                    joinedScheduleIds.add(schedule.id ?: 0)
                    memberAdapter.updateData(scheduleList, joinedScheduleIds)
                } else {
                    Toast.makeText(
                        this@ScheduleListActivity,
                        "Gagal mendaftar kegiatan",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ScheduleListActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            }
        }
    }
}