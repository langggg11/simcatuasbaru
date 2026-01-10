package com.polstat.simcat.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.polstat.simcat.R
import com.polstat.simcat.databinding.ItemMyActivityBinding
import com.polstat.simcat.model.Participation
import com.polstat.simcat.model.Schedule
import java.text.SimpleDateFormat
import java.util.*

class MyActivitiesAdapter(
    private var activities: List<Pair<Participation, Schedule>>,
    private val onCancelClick: (Participation, Schedule) -> Unit
) : RecyclerView.Adapter<MyActivitiesAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemMyActivityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(participation: Participation, schedule: Schedule) {
            binding.apply {
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

                val isPast = isSchedulePast(schedule)

                if (isPast) {
                    btnBatalkan.text = "Kegiatan Selesai"
                    btnBatalkan.setBackgroundResource(R.drawable.bg_button_disabled)
                    btnBatalkan.setTextColor(Color.parseColor("#9E9E9E"))
                    btnBatalkan.alpha = 0.6f
                    btnBatalkan.isClickable = false
                    btnBatalkan.isFocusable = false
                    btnBatalkan.setOnClickListener(null)
                } else {
                    btnBatalkan.text = "Batalkan Pendaftaran"
                    btnBatalkan.setBackgroundResource(R.drawable.bg_button_danger)
                    btnBatalkan.setTextColor(Color.WHITE)
                    btnBatalkan.alpha = 1.0f
                    btnBatalkan.isClickable = true
                    btnBatalkan.isFocusable = true
                    btnBatalkan.setOnClickListener {
                        onCancelClick(participation, schedule)
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
        val binding = ItemMyActivityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (participation, schedule) = activities[position]
        holder.bind(participation, schedule)
    }

    override fun getItemCount() = activities.size

    fun updateData(newActivities: List<Pair<Participation, Schedule>>) {
        activities = newActivities
        notifyDataSetChanged()
    }
}