package com.polstat.simcat.ui.main

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.polstat.simcat.adapter.EquipmentMemberAdapter
import com.polstat.simcat.api.RetrofitClient
import com.polstat.simcat.databinding.ActivityEquipmentListBinding
import com.polstat.simcat.databinding.DialogBorrowEquipmentBinding
import com.polstat.simcat.model.Borrow
import com.polstat.simcat.model.Equipment
import com.polstat.simcat.utils.SessionManager
import kotlinx.coroutines.launch
import java.time.LocalDate

class EquipmentListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEquipmentListBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: EquipmentMemberAdapter
    private var equipmentList = listOf<Equipment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEquipmentListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupRecyclerView()
        setupListeners()
        loadEquipment()
    }

    private fun setupRecyclerView() {
        adapter = EquipmentMemberAdapter(equipmentList) { equipment ->
            showBorrowDialog(equipment)
        }

        binding.rvEquipment.layoutManager = LinearLayoutManager(this)
        binding.rvEquipment.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadEquipment() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getAllEquipment()

                if (response.isSuccessful && response.body() != null) {
                    equipmentList = response.body()!!
                    adapter.updateData(equipmentList)
                } else {
                    Toast.makeText(
                        this@EquipmentListActivity,
                        "Gagal memuat peralatan",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@EquipmentListActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showBorrowDialog(equipment: Equipment) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val dialogBinding = DialogBorrowEquipmentBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        with(dialogBinding) {
            tvEquipmentName.text = equipment.nama
            tvAvailable.text = "${equipment.jumlahTersedia ?: equipment.jumlah} unit"
            etQuantity.setText("1")

            // Load dummy image
            ivEquipment.setBackgroundColor(
                Color.parseColor("#${Integer.toHexString(equipment.id?.toInt() ?: 0).padStart(6, '0')}")
            )

            btnClose.setOnClickListener {
                dialog.dismiss()
            }

            btnConfirm.setOnClickListener {
                val quantity = etQuantity.text.toString().toIntOrNull() ?: 0

                if (quantity <= 0) {
                    Toast.makeText(this@EquipmentListActivity, "Jumlah harus lebih dari 0", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val available = equipment.jumlahTersedia ?: equipment.jumlah
                if (quantity > available) {
                    Toast.makeText(this@EquipmentListActivity, "Stok tidak mencukupi", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                borrowEquipment(equipment, quantity, dialog)
            }
        }

        dialog.show()
    }

    private fun borrowEquipment(equipment: Equipment, quantity: Int, dialog: Dialog) {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getAuthToken()
                val userId = sessionManager.getUserId()

                val borrow = Borrow(
                    userId = userId,
                    equipmentId = equipment.id ?: 0,
                    borrowDate = LocalDate.now().toString(),
                    borrowStatus = "BORROWED",
                    jumlahDipinjam = quantity
                )

                val response = RetrofitClient.apiService.borrowEquipment(
                    "Bearer $token",
                    borrow
                )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@EquipmentListActivity,
                        "Peralatan berhasil dipinjam!",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                    loadEquipment() // Refresh list
                } else {
                    val errorMessage = try {
                        response.errorBody()?.string() ?: "Gagal meminjam peralatan"
                    } catch (e: Exception) {
                        "Gagal meminjam peralatan"
                    }

                    Toast.makeText(
                        this@EquipmentListActivity,
                        errorMessage,
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@EquipmentListActivity,
                    "Error: ${e.localizedMessage ?: "Terjadi kesalahan"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}