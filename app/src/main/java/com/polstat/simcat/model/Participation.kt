package com.polstat.simcat.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Participation(
    val id: Long? = null,
    val userId: Long,
    val scheduleId: Long,
    val registrationDate: String,
    val status: String
) : Parcelable