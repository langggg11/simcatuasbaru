package com.polstat.simcat.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.polstat.simcat.R
import com.polstat.simcat.databinding.ItemScheduleMemberBinding
import com.polstat.simcat.model.Schedule
import java.text.SimpleDateFormat
import java.util.*

class ScheduleMemberAdapter(
    private var scheduleList: List<Schedule>,
    private var joinedScheduleIds: Set<Long> = emptySet(),
    private val onJoinClick: (Schedule) -> Unit
) : RecyclerView.Adapter<ScheduleMemberAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemScheduleMemberBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(schedule: Schedule, isJoined: Boolean) {
            binding.apply {
                tvTitle.text = schedule.title
                tvDateTime.text = schedule.dateTime
                tvLocation.text = schedule.location

                // Show participants
                if (schedule.maxParticipants != null && schedule.maxParticipants > 0) {
                    tvParticipants.text = "Peserta: 0 / ${schedule.maxParticipants} orang"
                } else {
                    tvParticipants.text = "Peserta: Tidak dibatasi"
                }

                // Set badge
                when (schedule.tipeKegiatan.uppercase()) {
                    "LATIHAN" -> {
                        tvTypeBadge.text = "Latihan"
                        tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_latihan)
                    }
                    "TURNAMEN" -> {
                        tvTypeBadge.text = "Turnamen"
                        tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_turnamen)
                    }
                    "RAPAT" -> {
                        tvTypeBadge.text = "Rapat"
                        tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_rapat)
                    }
                    else -> {
                        tvTypeBadge.text = schedule.tipeKegiatan
                        tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_status)
                    }
                }

                // ✅ CEK APAKAH KEGIATAN SUDAH LEWAT
                val isPast = isSchedulePast(schedule)

                when {
                    // Kegiatan sudah lewat
                    isPast -> {
                        btnJoin.text = "Kegiatan Selesai"
                        btnJoin.setBackgroundResource(R.drawable.bg_button_disabled)
                        btnJoin.setTextColor(Color.parseColor("#9E9E9E"))
                        btnJoin.alpha = 0.6f
                        btnJoin.isClickable = false
                        btnJoin.isFocusable = false
                        btnJoin.setOnClickListener(null)
                    }
                    // Sudah join
                    isJoined -> {
                        btnJoin.text = "✓ Anda sudah terdaftar"
                        btnJoin.setBackgroundResource(R.drawable.bg_button_success)
                        btnJoin.setTextColor(ContextCompat.getColor(btnJoin.context, R.color.white))
                        btnJoin.alpha = 0.7f
                        btnJoin.isClickable = false
                        btnJoin.isFocusable = false
                        btnJoin.setOnClickListener(null)
                    }
                    // Belum join dan masih upcoming
                    else -> {
                        btnJoin.text = "Ikuti Kegiatan"
                        btnJoin.setBackgroundResource(R.drawable.bg_button_primary)
                        btnJoin.setTextColor(ContextCompat.getColor(btnJoin.context, R.color.white))
                        btnJoin.alpha = 1.0f
                        btnJoin.isClickable = true
                        btnJoin.isFocusable = true
                        btnJoin.isEnabled = true
                        btnJoin.setOnClickListener { onJoinClick(schedule) }
                    }
                }
            }
        }

        private fun isSchedulePast(schedule: Schedule): Boolean {
            return try {
                // Ambil bagian tanggal saja
                val dateStr = when {
                    schedule.dateTime.contains("•") ->
                        schedule.dateTime.split("•").getOrNull(0)?.trim() ?: schedule.dateTime
                    schedule.dateTime.contains(",") ->
                        schedule.dateTime.split(",").getOrNull(0)?.trim() ?: schedule.dateTime
                    schedule.dateTime.contains(" ") ->
                        schedule.dateTime.split(" ").getOrNull(0)?.trim() ?: schedule.dateTime
                    else -> schedule.dateTime
                }

                // Multiple format parser
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

                // Compare tanggal saja (ignore waktu)
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

                // Kegiatan dianggap "past" jika tanggal kegiatan <= hari ini
                !calSchedule.time.after(calCurrent.time)

            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScheduleMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val schedule = scheduleList[position]
        val isJoined = joinedScheduleIds.contains(schedule.id)
        holder.bind(schedule, isJoined)
    }

    override fun getItemCount() = scheduleList.size

    fun updateData(newList: List<Schedule>, newJoinedIds: Set<Long> = emptySet()) {
        scheduleList = newList
        joinedScheduleIds = newJoinedIds
        notifyDataSetChanged()
    }
}