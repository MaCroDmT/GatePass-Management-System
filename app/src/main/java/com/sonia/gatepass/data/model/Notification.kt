package com.sonia.gatepass.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Notification(
    val notificationId: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val gpid: String = "",
    val type: String = "", // NEW_REQUEST, APPROVED, REJECTED, COMPLETED, REOPENED
    val status: String = "", // READ, UNREAD
    val createdAt: String = ""
) : Parcelable
