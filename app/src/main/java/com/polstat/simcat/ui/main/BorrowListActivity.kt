package com.polstat.simcat.ui.main

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.polstat.simcat.adapter.BorrowAdapter
import com.polstat.simcat.api.RetrofitClient
import com.polstat.simcat.databinding.ActivityBorrowListBinding
import com.polstat.simcat.databinding.DialogBorrowDetailBinding
import com.polstat.simcat.databinding.DialogReturnEquipmentBinding
import com.polstat.simcat.model.Borrow
import com.polstat.simcat.utils.SessionManager
import kotlinx.coroutines.launch


class BorrowListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBorrowListBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: BorrowAdapter
    private var borrowList = listOf<Borrow>()
    private var filteredList = listOf<Borrow>()
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBorrowListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        isAdmin = sessionManager.getUserRole() == "ADMIN"

        setupUI()
        setupRecyclerView()
        setupListeners()
        loadBorrows()
    }

    private fun setupUI() {
        if (isAdmin) {
            binding.tvTitle.text = "Semua Peminjaman"
            binding.tvSubtitle.text = "Kelola peminjaman dari semua member"
        } else {
            binding.tvTitle.text = "Riwayat Peminjaman"
            binding.tvSubtitle.text = "Peralatan yang Anda pinjam"
        }
    }

    private fun setupRecyclerView() {
        adapter = BorrowAdapter(
            borrowList = filteredList,
            isAdmin = isAdmin,
            onDetailClick = { borrow -> showDetailDialog(borrow) },
            onReturnClick = if (!isAdmin) { { borrow -> showReturnDialog(borrow) } } else null
        )

        binding.rvBorrows.layoutManager = LinearLayoutManager(this)
        binding.rvBorrows.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.tabBorrowed.setOnClickListener {
            selectTab("BORROWED")
        }

        binding.tabReturned.setOnClickListener {
            selectTab("RETURNED")
        }

        binding.tabAll.setOnClickListener {
            selectTab("ALL")
        }
    }

    private fun selectTab(status: String) {
        // Reset all tabs
        binding.tabBorrowed.apply {
            setBackgroundResource(android.R.drawable.screen_background_light_transparent)
            setTextColor(Color.parseColor("#6C757D"))
        }
        binding.tabReturned.apply {
            setBackgroundResource(android.R.drawable.screen_background_light_transparent)
            setTextColor(Color.parseColor("#6C757D"))
        }
        binding.tabAll.apply {
            setBackgroundResource(android.R.drawable.screen_background_light_transparent)
            setTextColor(Color.parseColor("#6C757D"))
        }

        // Set selected tab
        when (status) {
            "BORROWED" -> {
                binding.tabBorrowed.apply {
                    setBackgroundResource(com.polstat.simcat.R.drawable.bg_tab_selected)
                    setTextColor(Color.parseColor("#1A2332"))
                }
                filteredList = borrowList.filter { it.borrowStatus.uppercase() == "BORROWED" }
            }
            "RETURNED" -> {
                binding.tabReturned.apply {
                    setBackgroundResource(com.polstat.simcat.R.drawable.bg_tab_selected)
                    setTextColor(Color.parseColor("#1A2332"))
                }
                filteredList = borrowList.filter { it.borrowStatus.uppercase() == "RETURNED" }
            }
            "ALL" -> {
                binding.tabAll.apply {
                    setBackgroundResource(com.polstat.simcat.R.drawable.bg_tab_selected)
                    setTextColor(Color.parseColor("#1A2332"))
                }
                filteredList = borrowList
            }
        }

        adapter.updateData(filteredList)
    }

    private fun loadBorrows() {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getAuthToken()
                val response = if (isAdmin) {
                    RetrofitClient.apiService.getAllBorrows("Bearer $token")
                } else {
                    val userId = sessionManager.getUserId()
                    RetrofitClient.apiService.getBorrowsByUser("Bearer $token", userId)
                }

                if (response.isSuccessful && response.body() != null) {
                    borrowList = response.body()!!
                    selectTab("BORROWED") // Default tab
                } else {
                    Toast.makeText(
                        this@BorrowListActivity,
                        "Gagal memuat peminjaman",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@BorrowListActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showDetailDialog(borrow: Borrow) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // TAMBAHKAN KONFIGURASI INI UNTUK LEBAR DIALOG
        val width = (resources.displayMetrics.widthPixels * 0.80).toInt()
        val height = ViewGroup.LayoutParams.WRAP_CONTENT

        val dialogBinding = DialogBorrowDetailBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        // Atur ukuran dialog
        dialog.window?.setLayout(width, height)
        dialog.window?.setGravity(Gravity.CENTER)

        with(dialogBinding) {
            // Status
            when (borrow.borrowStatus.uppercase()) {
                "BORROWED" -> {
                    tvStatus.text = "Dipinjam"
                    tvStatus.setTextColor(Color.parseColor("#FFC107"))
                }
                "RETURNED" -> {
                    tvStatus.text = "Dikembalikan"
                    tvStatus.setTextColor(Color.parseColor("#28A745"))
                }
                "OVERDUE" -> {
                    tvStatus.text = "Terlambat"
                    tvStatus.setTextColor(Color.parseColor("#DC3545"))
                }
            }

            tvBorrower.text = "User ID: ${borrow.userId}"
            tvEquipment.text = "Equipment ID: ${borrow.equipmentId}"
            tvQuantity.text = "${borrow.jumlahDipinjam} unit"
            tvBorrowDate.text = borrow.borrowDate

            if (borrow.returnDate != null) {
                layoutReturnDate.visibility = View.VISIBLE
                tvReturnDate.text = borrow.returnDate
                tvReceiver.text = borrow.notes ?: "-"
            }

            btnClose.setOnClickListener {
                dialog.dismiss()
            }

            btnOk.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    // Juga perbaiki showReturnDialog dengan cara yang sama:
    private fun showReturnDialog(borrow: Borrow) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // TAMBAHKAN KONFIGURASI INI UNTUK LEBAR DIALOG
        val width = (resources.displayMetrics.widthPixels * 0.80).toInt()
        val height = ViewGroup.LayoutParams.WRAP_CONTENT

        val dialogBinding = DialogReturnEquipmentBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        // Atur ukuran dialog
        dialog.window?.setLayout(width, height)
        dialog.window?.setGravity(Gravity.CENTER)

        with(dialogBinding) {
            tvEquipmentName.text = "Equipment ID: ${borrow.equipmentId}"
            tvQuantity.text = "${borrow.jumlahDipinjam} unit"
            tvBorrowDate.text = borrow.borrowDate

            // Set tanggal hari ini sebagai default
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date())
            etReturnDate.setText(today)

            btnClose.setOnClickListener {
                dialog.dismiss()
            }

            // Date Picker
            etReturnDate.setOnClickListener {
                val calendar = java.util.Calendar.getInstance()
                val datePickerDialog = android.app.DatePickerDialog(
                    this@BorrowListActivity,
                    { _, year, month, dayOfMonth ->
                        val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                        etReturnDate.setText(selectedDate)
                    },
                    calendar.get(java.util.Calendar.YEAR),
                    calendar.get(java.util.Calendar.MONTH),
                    calendar.get(java.util.Calendar.DAY_OF_MONTH)
                )
                datePickerDialog.show()
            }

            btnConfirm.setOnClickListener {
                val receiver = etReceiver.text.toString().trim()

                if (receiver.isEmpty()) {
                    Toast.makeText(this@BorrowListActivity, "Penerima barang harus diisi", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                returnEquipment(borrow, dialog)
            }
        }

        dialog.show()
    }

    private fun returnEquipment(borrow: Borrow, dialog: Dialog) {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getAuthToken()
                val response = RetrofitClient.apiService.returnEquipment(
                    "Bearer $token",
                    borrow.id ?: 0
                )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@BorrowListActivity,
                        "Peralatan berhasil dikembalikan!",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                    loadBorrows()
                } else {
                    val errorMessage = try {
                        response.errorBody()?.string() ?: "Gagal mengembalikan peralatan"
                    } catch (e: Exception) {
                        "Gagal mengembalikan peralatan"
                    }

                    Toast.makeText(
                        this@BorrowListActivity,
                        errorMessage,
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@BorrowListActivity,
                    "Error: ${e.localizedMessage ?: "Terjadi kesalahan"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}