package com.polstat.simcat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.polstat.simcat.databinding.ItemEquipmentAdminBinding
import com.polstat.simcat.model.Equipment
import java.text.NumberFormat
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
            // ✅ Gunakan ID yang sesuai dengan layout
            tvName.text = equipment.nama
            tvBrand.text = equipment.merek

            // Format price
            val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            tvPrice.text = formatter.format(equipment.harga)

            // Available stock
            val available = equipment.jumlahTersedia ?: equipment.jumlah
            tvAvailable.text = "Tersedia: $available / ${equipment.jumlah}"

            btnEdit.setOnClickListener {
                onEditClick(equipment)
            }

            btnDelete.setOnClickListener {
                onDeleteClick(equipment)
            }

            // Load dummy image
            ivEquipment.setBackgroundColor(
                android.graphics.Color.parseColor("#${Integer.toHexString(equipment.id?.toInt() ?: 0).padStart(6, '0')}")
            )
        }
    }

    override fun getItemCount() = equipmentList.size

    fun updateData(newList: List<Equipment>) {
        equipmentList = newList
        notifyDataSetChanged()
    }
}