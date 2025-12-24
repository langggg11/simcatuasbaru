package com.polstat.simcat.ui.main

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.polstat.simcat.adapter.EquipmentAdminAdapter
import com.polstat.simcat.api.RetrofitClient
import com.polstat.simcat.databinding.ActivityEquipmentAdminBinding
import com.polstat.simcat.databinding.DialogAddEquipmentBinding
import com.polstat.simcat.model.Equipment
import com.polstat.simcat.utils.SessionManager
import kotlinx.coroutines.launch

class EquipmentAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEquipmentAdminBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: EquipmentAdminAdapter
    private var equipmentList = listOf<Equipment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEquipmentAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupRecyclerView()
        setupListeners()
        loadEquipment()
    }

    private fun setupRecyclerView() {
        adapter = EquipmentAdminAdapter(
            equipmentList,
            onEditClick = { equipment -> showEditDialog(equipment) },
            onDeleteClick = { equipment -> showDeleteDialog(equipment) }
        )

        binding.rvEquipment.layoutManager = LinearLayoutManager(this)
        binding.rvEquipment.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnAdd.setOnClickListener {
            showAddDialog()
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
                    val errorMsg = response.errorBody()?.string() ?: "Gagal memuat peralatan"
                    Log.e("EquipmentAdmin", "Load error: $errorMsg")
                    Toast.makeText(this@EquipmentAdminActivity, "Gagal memuat peralatan", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("EquipmentAdmin", "Load exception", e)
                Toast.makeText(this@EquipmentAdminActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val dialogBinding = DialogAddEquipmentBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        with(dialogBinding) {
            tvTitle.text = "Tambah Peralatan"

            btnClose.setOnClickListener {
                dialog.dismiss()
            }

            btnSave.setOnClickListener {
                val name = etName.text.toString().trim()
                val brand = etBrand.text.toString().trim()
                val priceStr = etPrice.text.toString().trim()
                val totalStr = etTotal.text.toString().trim()
                val availableStr = etAvailable.text.toString().trim()

                if (validateInput(name, brand, priceStr, totalStr, availableStr)) {
                    val equipment = Equipment(
                        nama = name,
                        tipe = "CATUR",
                        merek = brand,
                        harga = priceStr.toDouble(),
                        jumlah = totalStr.toInt(),
                        jumlahTersedia = availableStr.toInt()
                    )
                    createEquipment(equipment, dialog)
                }
            }
        }

        dialog.show()
    }

    private fun showEditDialog(equipment: Equipment) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val dialogBinding = DialogAddEquipmentBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        with(dialogBinding) {
            tvTitle.text = "Edit Peralatan"

            etName.setText(equipment.nama)
            etBrand.setText(equipment.merek)
            etPrice.setText(equipment.harga.toString())
            etTotal.setText(equipment.jumlah.toString())
            etAvailable.setText((equipment.jumlahTersedia ?: equipment.jumlah).toString())

            btnClose.setOnClickListener {
                dialog.dismiss()
            }

            btnSave.setOnClickListener {
                val name = etName.text.toString().trim()
                val brand = etBrand.text.toString().trim()
                val priceStr = etPrice.text.toString().trim()
                val totalStr = etTotal.text.toString().trim()
                val availableStr = etAvailable.text.toString().trim()

                if (validateInput(name, brand, priceStr, totalStr, availableStr)) {
                    val updatedEquipment = Equipment(
                        id = equipment.id,
                        nama = name,
                        tipe = equipment.tipe,
                        merek = brand,
                        harga = priceStr.toDouble(),
                        jumlah = totalStr.toInt(),
                        jumlahTersedia = availableStr.toInt()
                    )
                    updateEquipment(updatedEquipment, dialog)
                }
            }
        }

        dialog.show()
    }

    private fun showDeleteDialog(equipment: Equipment) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Peralatan")
            .setMessage("Apakah Anda yakin ingin menghapus ${equipment.nama}?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteEquipment(equipment)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun validateInput(name: String, brand: String, price: String, total: String, available: String): Boolean {
        if (name.isEmpty()) {
            Toast.makeText(this, "Nama peralatan tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return false
        }
        if (brand.isEmpty()) {
            Toast.makeText(this, "Merk tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return false
        }
        if (price.isEmpty() || price.toDoubleOrNull() == null) {
            Toast.makeText(this, "Harga tidak valid", Toast.LENGTH_SHORT).show()
            return false
        }
        if (total.isEmpty() || total.toIntOrNull() == null) {
            Toast.makeText(this, "Total tidak valid", Toast.LENGTH_SHORT).show()
            return false
        }
        if (available.isEmpty() || available.toIntOrNull() == null) {
            Toast.makeText(this, "Tersedia tidak valid", Toast.LENGTH_SHORT).show()
            return false
        }
        val availableInt = available.toInt()
        val totalInt = total.toInt()
        if (availableInt > totalInt) {
            Toast.makeText(this, "Tersedia tidak boleh lebih dari total", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun createEquipment(equipment: Equipment, dialog: Dialog) {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getAuthToken()
                Log.d("EquipmentAdmin", "Creating equipment with token: Bearer $token")
                Log.d("EquipmentAdmin", "Equipment data: $equipment")

                val response = RetrofitClient.apiService.createEquipment(
                    "Bearer $token",
                    equipment
                )

                if (response.isSuccessful) {
                    Toast.makeText(this@EquipmentAdminActivity, "Peralatan berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadEquipment()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Gagal menambahkan peralatan"
                    Log.e("EquipmentAdmin", "Create error: ${response.code()} - $errorMsg")
                    Toast.makeText(this@EquipmentAdminActivity, "Gagal: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("EquipmentAdmin", "Create exception", e)
                Toast.makeText(this@EquipmentAdminActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateEquipment(equipment: Equipment, dialog: Dialog) {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getAuthToken()
                Log.d("EquipmentAdmin", "Updating equipment ID: ${equipment.id}")

                val response = RetrofitClient.apiService.updateEquipment(
                    "Bearer $token",
                    equipment.id ?: 0,
                    equipment
                )

                if (response.isSuccessful) {
                    Toast.makeText(this@EquipmentAdminActivity, "Peralatan berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadEquipment()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Gagal memperbarui peralatan"
                    Log.e("EquipmentAdmin", "Update error: ${response.code()} - $errorMsg")
                    Toast.makeText(this@EquipmentAdminActivity, "Gagal: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("EquipmentAdmin", "Update exception", e)
                Toast.makeText(this@EquipmentAdminActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun deleteEquipment(equipment: Equipment) {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getAuthToken()
                Log.d("EquipmentAdmin", "Deleting equipment ID: ${equipment.id}")

                val response = RetrofitClient.apiService.deleteEquipment(
                    "Bearer $token",
                    equipment.id ?: 0
                )

                if (response.isSuccessful) {
                    Toast.makeText(this@EquipmentAdminActivity, "Peralatan berhasil dihapus", Toast.LENGTH_SHORT).show()
                    loadEquipment()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Gagal menghapus peralatan"
                    Log.e("EquipmentAdmin", "Delete error: ${response.code()} - $errorMsg")
                    Toast.makeText(this@EquipmentAdminActivity, "Gagal: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("EquipmentAdmin", "Delete exception", e)
                Toast.makeText(this@EquipmentAdminActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}