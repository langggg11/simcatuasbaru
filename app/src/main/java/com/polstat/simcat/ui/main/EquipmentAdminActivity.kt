package com.polstat.simcat.ui.main

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.Toast
import com.polstat.simcat.R
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
import android.view.WindowManager
import android.view.Gravity
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView

class EquipmentAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEquipmentAdminBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: EquipmentAdminAdapter
    private var equipmentList = listOf<Equipment>()

    // Daftar tipe peralatan
    private val tipeList = listOf(
        "PAPAN_CATUR",
        "PAPAN_CATUR_TRAVEL",
        "TIMER",
        "TIMER_DIGITAL",
        "SET_CATUR"
    )

    private val tipeDisplayNames = listOf(
        "Papan Catur Standar",
        "Papan Catur Travel",
        "Timer Manual",
        "Timer Digital",
        "Set Catur"
    )

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

            // Setup spinner untuk tipe peralatan
            val spinnerAdapter = ArrayAdapter(
                this@EquipmentAdminActivity,
                android.R.layout.simple_spinner_item,
                tipeDisplayNames
            )
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerTipe.adapter = spinnerAdapter

            // Preview gambar berdasarkan tipe yang dipilih
            spinnerTipe.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                    updatePreviewImage(tipeList[position], ivPreview)
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            // Set default preview
            updatePreviewImage(tipeList[0], ivPreview)

            btnClose.setOnClickListener {
                dialog.dismiss()
            }

            btnSave.setOnClickListener {
                val name = etName.text.toString().trim()
                val brand = etBrand.text.toString().trim()
                val priceStr = etPrice.text.toString().trim()
                val totalStr = etTotal.text.toString().trim()
                val availableStr = etAvailable.text.toString().trim()
                val selectedTipe = tipeList[spinnerTipe.selectedItemPosition]

                if (validateInput(name, brand, priceStr, totalStr, availableStr)) {
                    val equipment = Equipment(
                        nama = name,
                        tipe = selectedTipe,  // Tipe menentukan gambar
                        merek = brand,
                        harga = priceStr.toDouble(),
                        jumlah = totalStr.toInt(),
                        jumlahTersedia = availableStr.toInt()
                    )
                    createEquipment(equipment, dialog)
                }
            }
        }

        // Atur ukuran dialog menjadi 80% lebar layar
        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.8).toInt()
        val height = WindowManager.LayoutParams.WRAP_CONTENT

        dialog.window?.setLayout(width, height)
        dialog.window?.setGravity(Gravity.CENTER)

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

            // Setup spinner untuk tipe peralatan
            val spinnerAdapter = ArrayAdapter(
                this@EquipmentAdminActivity,
                android.R.layout.simple_spinner_item,
                tipeDisplayNames
            )
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerTipe.adapter = spinnerAdapter

            // Set selected tipe
            val selectedIndex = tipeList.indexOf(equipment.tipe)
            if (selectedIndex >= 0) {
                spinnerTipe.setSelection(selectedIndex)
            }

            // Preview gambar berdasarkan tipe yang dipilih
            spinnerTipe.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                    updatePreviewImage(tipeList[position], ivPreview)
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            // Set preview awal
            updatePreviewImage(equipment.tipe, ivPreview)

            btnClose.setOnClickListener {
                dialog.dismiss()
            }

            btnSave.setOnClickListener {
                val name = etName.text.toString().trim()
                val brand = etBrand.text.toString().trim()
                val priceStr = etPrice.text.toString().trim()
                val totalStr = etTotal.text.toString().trim()
                val availableStr = etAvailable.text.toString().trim()
                val selectedTipe = tipeList[spinnerTipe.selectedItemPosition]

                if (validateInput(name, brand, priceStr, totalStr, availableStr)) {
                    val updatedEquipment = Equipment(
                        id = equipment.id,
                        nama = name,
                        tipe = selectedTipe,
                        merek = brand,
                        harga = priceStr.toDouble(),
                        jumlah = totalStr.toInt(),
                        jumlahTersedia = availableStr.toInt()
                    )
                    updateEquipment(updatedEquipment, dialog)
                }
            }
        }

        // Atur ukuran dialog menjadi 80% lebar layar
        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.8).toInt()
        val height = WindowManager.LayoutParams.WRAP_CONTENT

        dialog.window?.setLayout(width, height)
        dialog.window?.setGravity(Gravity.CENTER)

        dialog.show()
    }

    private fun updatePreviewImage(tipe: String, imageView: ImageView) {
        val drawableId = when (tipe.uppercase()) {
            "PAPAN_CATUR" -> R.drawable.chess_board_standard
            "TIMER" -> R.drawable.chess_clock_manual
            "SET_CATUR" -> R.drawable.chess_pieces_set
            "PAPAN_CATUR_TRAVEL" -> R.drawable.chess_board_travel
            "TIMER_DIGITAL" -> R.drawable.chess_clock_digital
            else -> R.drawable.equipment_placeholder
        }
        imageView.setImageResource(drawableId)
        imageView.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
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
        val p = price.toDoubleOrNull()
        if (price.isEmpty() || p == null) {
            Toast.makeText(this, "Harga tidak valid", Toast.LENGTH_SHORT).show()
            return false
        }
        val t = total.toIntOrNull()
        if (total.isEmpty() || t == null) {
            Toast.makeText(this, "Total tidak valid", Toast.LENGTH_SHORT).show()
            return false
        }
        val a = available.toIntOrNull()
        if (available.isEmpty() || a == null) {
            Toast.makeText(this, "Tersedia tidak valid", Toast.LENGTH_SHORT).show()
            return false
        }
        if (a > t) {
            Toast.makeText(this, "Tersedia tidak boleh lebih dari total", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun createEquipment(equipment: Equipment, dialog: Dialog) {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getAuthToken()
                val response = RetrofitClient.apiService.createEquipment(
                    "Bearer $token",
                    equipment
                )

                if (response.isSuccessful) {
                    val message = response.body()?.string() ?: "Peralatan berhasil ditambahkan"
                    Toast.makeText(this@EquipmentAdminActivity, message, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadEquipment()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Gagal: ${response.code()}"
                    Log.e("EquipmentAdmin", "Create error: $errorMsg")
                    Toast.makeText(this@EquipmentAdminActivity, "Error ${response.code()}: Periksa hak akses admin.", Toast.LENGTH_LONG).show()
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
                val response = RetrofitClient.apiService.updateEquipment(
                    "Bearer $token",
                    equipment.id ?: 0,
                    equipment
                )

                if (response.isSuccessful) {
                    val message = response.body()?.string() ?: "Peralatan berhasil diperbarui"
                    Toast.makeText(this@EquipmentAdminActivity, message, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadEquipment()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Gagal: ${response.code()}"
                    Log.e("EquipmentAdmin", "Update error: $errorMsg")
                    Toast.makeText(this@EquipmentAdminActivity, "Error ${response.code()}: Gagal update.", Toast.LENGTH_LONG).show()
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
                val response = RetrofitClient.apiService.deleteEquipment(
                    "Bearer $token",
                    equipment.id ?: 0
                )

                if (response.isSuccessful) {
                    val message = response.body()?.string() ?: "Peralatan berhasil dihapus"
                    Toast.makeText(this@EquipmentAdminActivity, message, Toast.LENGTH_SHORT).show()
                    loadEquipment()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Gagal: ${response.code()}"
                    Log.e("EquipmentAdmin", "Delete error: $errorMsg")
                    Toast.makeText(this@EquipmentAdminActivity, "Error ${response.code()}: Gagal hapus.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("EquipmentAdmin", "Delete exception", e)
                Toast.makeText(this@EquipmentAdminActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}