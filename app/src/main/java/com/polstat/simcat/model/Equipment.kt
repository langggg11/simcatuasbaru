package com.polstat.simcat.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Equipment(
    val id: Long? = null,
    val nama: String,
    val tipe: String,
    val merek: String,
    val harga: Double,
    val jumlah: Int,
    val jumlahTersedia: Int? = null,
    val deskripsi: String? = null,
    val kondisi: String? = "BAIK"
) : Parcelable