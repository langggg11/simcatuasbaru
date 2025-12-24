package com.polstat.simcat.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: Long? = null,
    val name: String,
    val email: String,
    val password: String? = null,
    val role: String? = "MEMBER",
    val memberID: String? = null,
    val phoneNumber: String? = null,
    val address: String? = null
) : Parcelable