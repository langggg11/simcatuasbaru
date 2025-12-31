package com.polstat.simcat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.polstat.simcat.databinding.ItemBorrowBinding
import com.polstat.simcat.model.Borrow

class BorrowAdapter(
    private var borrowList: List<Borrow>,
    private val isAdmin: Boolean = false,
    private var equipmentNames: Map<Long, String> = emptyMap(), // ✅ Diubah ke var
    private var userNames: Map<Long, String> = emptyMap(),      // ✅ Diubah ke var
    private val onDetailClick: (Borrow) -> Unit,
    private val onReturnClick: ((Borrow) -> Unit)? = null
) : RecyclerView.Adapter<BorrowAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemBorrowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBorrowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val borrow = borrowList[position]

        with(holder.binding) {
            // ✅ Menggunakan map untuk menampilkan nama alat
            tvEquipmentName.text = equipmentNames[borrow.equipmentId] ?: "Equipment ID: ${borrow.equipmentId}"

            // Status
            when (borrow.borrowStatus.uppercase()) {
                "BORROWED" -> {
                    tvStatus.text = "Dipinjam"
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#FFC107"))
                }
                "RETURNED" -> {
                    tvStatus.text = "Dikembalikan"
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#28A745"))
                }
                "OVERDUE" -> {
                    tvStatus.text = "Terlambat"
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#DC3545"))
                }
                else -> {
                    tvStatus.text = borrow.borrowStatus
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#6C757D"))
                }
            }

            // Borrower (admin only)
            if (isAdmin) {
                layoutBorrower.visibility = View.VISIBLE
                tvBorrower.text = userNames[borrow.userId] ?: "User ID: ${borrow.userId}"
            } else {
                layoutBorrower.visibility = View.GONE
            }

            // Quantity
            tvQuantity.text = "Jumlah: ${borrow.jumlahDipinjam} unit"

            // Borrow date
            tvBorrowDate.text = "Dipinjam: ${borrow.borrowDate}"

            // Return date
            if (borrow.returnDate != null) {
                tvReturnDate.visibility = View.VISIBLE
                tvReturnDate.text = "Dikembalikan: ${borrow.returnDate}"
            } else {
                tvReturnDate.visibility = View.GONE
            }

            // Action button
            if (borrow.borrowStatus.uppercase() == "BORROWED" && !isAdmin) {
                btnAction.text = "Kembalikan"
                btnAction.setOnClickListener {
                    onReturnClick?.invoke(borrow)
                }
            } else {
                btnAction.text = "Lihat Detail"
                btnAction.setOnClickListener {
                    onDetailClick(borrow)
                }
            }
        }
    }

    override fun getItemCount() = borrowList.size

    fun updateData(newList: List<Borrow>, newEquipmentNames: Map<Long, String> = emptyMap(), newUserNames: Map<Long, String> = emptyMap()) {
        borrowList = newList
        if (newEquipmentNames.isNotEmpty()) equipmentNames = newEquipmentNames
        if (newUserNames.isNotEmpty()) userNames = newUserNames
        notifyDataSetChanged()
    }
}