package com.polstat.simcat.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Borrow(
    val id: Long? = null,
    val userId: Long,
    val equipmentId: Long,
    val borrowDate: String,
    val returnDate: String? = null,
    val borrowStatus: String,
    val overdueDays: Int? = null,
    val jumlahDipinjam: Int,
    val notes: String? = null
) : Parcelable