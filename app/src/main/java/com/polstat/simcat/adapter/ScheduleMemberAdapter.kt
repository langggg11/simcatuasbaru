package com.polstat.simcat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.polstat.simcat.R
import com.polstat.simcat.databinding.ItemScheduleMemberBinding
import com.polstat.simcat.model.Schedule

class ScheduleMemberAdapter(
    private var scheduleList: List<Schedule>,
    private var joinedScheduleIds: Set<Long> = emptySet(),
    private val onJoinClick: (Schedule) -> Unit
) : RecyclerView.Adapter<ScheduleMemberAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemScheduleMemberBinding) :
        RecyclerView.ViewHolder(binding.root)

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

        with(holder.binding) {
            tvTitle.text = schedule.title

            // Parse dateTime
            val dateTimeParts = schedule.dateTime.split("•")
            tvDate.text = dateTimeParts.getOrNull(0)?.trim() ?: schedule.dateTime
            tvTime.text = dateTimeParts.getOrNull(1)?.trim() ?: ""

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

            // Check if user already joined
            val isJoined = joinedScheduleIds.contains(schedule.id)

            if (isJoined) {
                btnJoin.text = "✓ Anda sudah terdaftar"
                btnJoin.setBackgroundResource(R.drawable.bg_button_success)
                btnJoin.isEnabled = false
                btnJoin.alpha = 0.7f
            } else {
                btnJoin.text = "Ikuti Kegiatan"
                btnJoin.setBackgroundResource(R.drawable.bg_button_primary)
                btnJoin.isEnabled = true
                btnJoin.alpha = 1.0f
            }

            btnJoin.setOnClickListener {
                if (!isJoined) {
                    onJoinClick(schedule)
                }
            }
        }
    }

    override fun getItemCount() = scheduleList.size

    fun updateData(newList: List<Schedule>, newJoinedIds: Set<Long> = emptySet()) {
        scheduleList = newList
        joinedScheduleIds = newJoinedIds
        notifyDataSetChanged()
    }
}