package com.polstat.simcat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.polstat.simcat.R
import com.polstat.simcat.databinding.ItemEquipmentMemberBinding
import com.polstat.simcat.model.Equipment

class EquipmentMemberAdapter(
    private var equipmentList: List<Equipment>,
    private val onBorrowClick: (Equipment) -> Unit
) : RecyclerView.Adapter<EquipmentMemberAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemEquipmentMemberBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEquipmentMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val equipment = equipmentList[position]

        with(holder.binding) {
            tvName.text = equipment.nama
            tvBrand.text = equipment.merek

            // Available stock
            val available = equipment.jumlahTersedia ?: equipment.jumlah
            tvAvailable.text = "$available unit"

            // Enable/disable button based on availability
            btnBorrow.isEnabled = available > 0
            btnBorrow.alpha = if (available > 0) 1.0f else 0.5f

            btnBorrow.setOnClickListener {
                onBorrowClick(equipment)
            }

            // SET GAMBAR BERDASARKAN TIPE
            val drawableId = when (equipment.tipe?.uppercase()) {
                "PAPAN_CATUR" -> R.drawable.chess_board_standard
                "TIMER" -> R.drawable.chess_clock_manual
                "SET_CATUR" -> R.drawable.chess_pieces_set
                "PAPAN_CATUR_TRAVEL" -> R.drawable.chess_board_travel
                "TIMER_DIGITAL" -> R.drawable.chess_clock_digital
                else -> R.drawable.equipment_placeholder
            }

            ivEquipment.setImageResource(drawableId)
            ivEquipment.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
        }
    }

    override fun getItemCount() = equipmentList.size

    fun updateData(newList: List<Equipment>) {
        equipmentList = newList
        notifyDataSetChanged()
    }
}