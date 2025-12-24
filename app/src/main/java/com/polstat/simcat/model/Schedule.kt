package com.polstat.simcat.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Schedule(
    val id: Long? = null,
    val title: String,
    val dateTime: String,
    val location: String,
    val deskripsi: String? = null,
    val tipeKegiatan: String,
    val maxParticipants: Int? = null
) : Parcelable