package com.sonia.gatepass.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Movement(
    val movementId: String = "",
    val gpid: String = "",
    val type: String = "", // OUTWARD, INWARD, RE_DISPATCH
    val quantity: Int = 0,
    val date: String = "",
    val recordedBy: String = "",
    val recordedByName: String = "",
    val remarks: String = "",
    val createdAt: String = ""
) : Parcelable
