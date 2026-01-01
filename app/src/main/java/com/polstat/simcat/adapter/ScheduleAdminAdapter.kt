package com.polstat.simcat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.polstat.simcat.R
import com.polstat.simcat.databinding.ItemScheduleAdminBinding
import com.polstat.simcat.model.Schedule

class ScheduleAdminAdapter(
    private var scheduleList: List<Schedule>,
    private val onEditClick: (Schedule) -> Unit,
    private val onDeleteClick: (Schedule) -> Unit
) : RecyclerView.Adapter<ScheduleAdminAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemScheduleAdminBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScheduleAdminBinding.inflate(
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

            // TAMPILKAN SELURUH dateTime TANPA DIPISAH
            tvDateTime.text = schedule.dateTime

            tvLocation.text = schedule.location

            // DIHAPUS - Tidak lagi menampilkan tvParticipants

            // Set badge based on type
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

            btnEdit.setOnClickListener {
                onEditClick(schedule)
            }

            btnDelete.setOnClickListener {
                onDeleteClick(schedule)
            }
        }
    }

    override fun getItemCount() = scheduleList.size

    fun updateData(newList: List<Schedule>) {
        scheduleList = newList
        notifyDataSetChanged()
    }
}