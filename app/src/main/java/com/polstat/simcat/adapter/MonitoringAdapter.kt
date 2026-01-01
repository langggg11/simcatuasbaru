package com.polstat.simcat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.polstat.simcat.R
import com.polstat.simcat.databinding.ItemMonitoringBinding
import com.polstat.simcat.model.Schedule

class MonitoringAdapter(
    private var scheduleList: List<Schedule>,
    private val onDetailClick: (Schedule) -> Unit
) : RecyclerView.Adapter<MonitoringAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemMonitoringBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMonitoringBinding.inflate(
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

            val dateTimeParts = schedule.dateTime.split("•")
            val date = dateTimeParts.getOrNull(0)?.trim() ?: schedule.dateTime
            val time = dateTimeParts.getOrNull(1)?.trim() ?: ""

            tvDateTime.text = "$date • $time"
            tvLocation.text = schedule.location

            // DIHAPUS - Tidak lagi menampilkan tvParticipants

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

            btnLihatDetail.setOnClickListener {
                onDetailClick(schedule)
            }
        }
    }

    override fun getItemCount() = scheduleList.size

    fun updateData(newList: List<Schedule>) {
        scheduleList = newList
        notifyDataSetChanged()
    }
}