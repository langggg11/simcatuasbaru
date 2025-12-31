package com.polstat.simcat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.polstat.simcat.databinding.ItemMyActivityBinding
import com.polstat.simcat.model.Participation
import com.polstat.simcat.model.Schedule

class MyActivitiesAdapter(
    private var activities: List<Pair<Participation, Schedule>>,
    private val onCancelClick: (Participation, Schedule) -> Unit
) : RecyclerView.Adapter<MyActivitiesAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemMyActivityBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyActivityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (participation, schedule) = activities[position]

        with(holder.binding) {
            tvTitle.text = schedule.title

            val dateTimeParts = schedule.dateTime.split("•")
            val date = dateTimeParts.getOrNull(0)?.trim() ?: schedule.dateTime
            val time = dateTimeParts.getOrNull(1)?.trim() ?: ""

            tvDateTime.text = "$date • $time"
            tvLocation.text = schedule.location

            if (schedule.maxParticipants != null && schedule.maxParticipants > 0) {
                tvParticipants.text = "${schedule.maxParticipants} peserta"
            } else {
                tvParticipants.text = "Tidak dibatasi"
            }

            btnBatalkan.setOnClickListener {
                onCancelClick(participation, schedule)
            }
        }
    }

    override fun getItemCount() = activities.size

    fun updateData(newActivities: List<Pair<Participation, Schedule>>) {
        activities = newActivities
        notifyDataSetChanged()
    }
}