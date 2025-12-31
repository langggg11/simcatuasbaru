package com.polstat.simcat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.polstat.simcat.databinding.ItemEquipmentAdminBinding
import com.polstat.simcat.model.Equipment
import java.text.NumberFormat
import com.polstat.simcat.R
import java.util.*

class EquipmentAdminAdapter(
    private var equipmentList: List<Equipment>,
    private val onEditClick: (Equipment) -> Unit,
    private val onDeleteClick: (Equipment) -> Unit
) : RecyclerView.Adapter<EquipmentAdminAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemEquipmentAdminBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEquipmentAdminBinding.inflate(
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

            // Format price
            val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            tvPrice.text = formatter.format(equipment.harga)

            // Available stock
            val available = equipment.jumlahTersedia ?: equipment.jumlah
            tvAvailable.text = "Tersedia: $available / ${equipment.jumlah}"

            btnEdit.setOnClickListener { onEditClick(equipment) }
            btnDelete.setOnClickListener { onDeleteClick(equipment) }

            // SET GAMBAR BERDASARKAN TIPE - PASTIKAN NAMA FILE SESUAI
            val drawableId = when (equipment.tipe.uppercase()) {
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