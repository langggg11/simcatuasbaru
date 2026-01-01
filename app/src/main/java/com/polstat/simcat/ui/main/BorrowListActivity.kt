package com.polstat.simcat.ui.main

import android.app.DatePickerDialog
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BorrowListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBorrowListBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: BorrowAdapter
    private var borrowList = listOf<Borrow>()
    private var filteredList = listOf<Borrow>()
    private var equipmentMap = mapOf<Long, String>()
    private var userMap = mapOf<Long, String>()
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
        binding.btnBack.setOnClickListener { finish() }
        binding.tabBorrowed.setOnClickListener { selectTab("BORROWED") }
        binding.tabReturned.setOnClickListener { selectTab("RETURNED") }
        binding.tabAll.setOnClickListener { selectTab("ALL") }
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

        // Update adapter dengan data yang sudah difilter dan map
        adapter.updateData(filteredList, equipmentMap, userMap)
    }

    private fun loadBorrows() {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getAuthToken()

                // 1. Load equipment names
                val equipRes = RetrofitClient.apiService.getAllEquipment()
                if (equipRes.isSuccessful) {
                    equipmentMap = equipRes.body()?.associate { it.id!! to it.nama } ?: emptyMap()
                }

                // 2. Load user names (only if admin)
                if (isAdmin) {
                    try {
                        val userRes = RetrofitClient.apiService.getAllUsers("Bearer $token")
                        if (userRes.isSuccessful) {
                            userMap = userRes.body()?.associate { it.id!! to it.name } ?: emptyMap()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // 3. Load borrow data
                val response = if (isAdmin) {
                    RetrofitClient.apiService.getAllBorrows("Bearer $token")
                } else {
                    RetrofitClient.apiService.getBorrowsByUser(
                        "Bearer $token",
                        sessionManager.getUserId()
                    )
                }

                if (response.isSuccessful && response.body() != null) {
                    borrowList = response.body()!!
                    selectTab("BORROWED")
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
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        val width = (resources.displayMetrics.widthPixels * 0.80).toInt()
        val height = ViewGroup.LayoutParams.WRAP_CONTENT

        val dialogBinding = DialogBorrowDetailBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setLayout(width, height)
        dialog.window?.setGravity(Gravity.CENTER)

        with(dialogBinding) {
            // Set status dengan warna yang sesuai
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
                else -> {
                    tvStatus.text = borrow.borrowStatus
                    tvStatus.setTextColor(Color.parseColor("#6C757D"))
                }
            }

            // Tampilkan nama peminjam
            tvBorrower.text = if (isAdmin) {
                userMap[borrow.userId] ?: "User ID: ${borrow.userId}"
            } else {
                "Anda"
            }

            tvEquipment.text = equipmentMap[borrow.equipmentId] ?: "Equipment ID: ${borrow.equipmentId}"
            tvQuantity.text = "${borrow.jumlahDipinjam} unit"
            tvBorrowDate.text = borrow.borrowDate

            // Jika sudah dikembalikan, tampilkan info pengembalian
            if (borrow.returnDate != null) {
                layoutReturnDate.visibility = View.VISIBLE
                tvReturnDate.text = borrow.returnDate
                // DIHAPUS: tvReceiver sudah tidak ada di XML
            } else {
                layoutReturnDate.visibility = View.GONE
            }

            btnClose.setOnClickListener { dialog.dismiss() }
            btnOk.setOnClickListener { dialog.dismiss() }
        }

        dialog.show()
    }

    private fun showReturnDialog(borrow: Borrow) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        val width = (resources.displayMetrics.widthPixels * 0.80).toInt()
        val height = ViewGroup.LayoutParams.WRAP_CONTENT

        val dialogBinding = DialogReturnEquipmentBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setLayout(width, height)
        dialog.window?.setGravity(Gravity.CENTER)

        with(dialogBinding) {
            tvEquipmentName.text = equipmentMap[borrow.equipmentId] ?: "ID: ${borrow.equipmentId}"
            tvQuantity.text = "${borrow.jumlahDipinjam} unit"
            tvBorrowDate.text = borrow.borrowDate

            // Set tanggal hari ini sebagai default
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            etReturnDate.setText(today)

            btnClose.setOnClickListener { dialog.dismiss() }

            // Date picker untuk tanggal pengembalian
            etReturnDate.setOnClickListener {
                val calendar = Calendar.getInstance()
                DatePickerDialog(
                    this@BorrowListActivity,
                    { _, year, month, dayOfMonth ->
                        val selectedDate = String.format(
                            "%04d-%02d-%02d",
                            year,
                            month + 1,
                            dayOfMonth
                        )
                        etReturnDate.setText(selectedDate)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            btnConfirm.setOnClickListener {
                val receiver = etReceiver.text.toString().trim()
                if (receiver.isEmpty()) {
                    Toast.makeText(
                        this@BorrowListActivity,
                        "Penerima barang harus diisi",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                returnEquipment(borrow, receiver, dialog)
            }
        }

        dialog.show()
    }

    private fun returnEquipment(borrow: Borrow, receiverName: String, dialog: Dialog) {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getAuthToken()

                // Kirim Map<String, String> sesuai dengan API yang diharapkan
                val notesMap = mapOf("notes" to receiverName)

                val response = RetrofitClient.apiService.returnEquipment(
                    "Bearer $token",
                    borrow.id ?: 0,
                    notesMap
                )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@BorrowListActivity,
                        "Peralatan berhasil dikembalikan!",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                    loadBorrows() // Refresh data
                } else {
                    val error = response.errorBody()?.string() ?: "Gagal mengembalikan peralatan"
                    Toast.makeText(
                        this@BorrowListActivity,
                        error,
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@BorrowListActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}