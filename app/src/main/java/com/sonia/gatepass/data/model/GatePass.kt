package com.sonia.gatepass.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GatePass(
    val gpid: String = "",
    val styleNo: String = "",
    val goodsName: String = "",
    val concernedPeopleEmail: String = "",
    val destination: String = "",
    val purpose: String = "",
    val totalSent: Int = 0,
    val totalReturned: Int = 0,
    val totalRedispatched: Int = 0,
    val balanceQuantity: Int = 0,
    val returnableDate: String = "",
    val status: String = "",
    val createdBy: String = "",
    val createdByName: String = "",
    val approvedBy: String = "",
    val approvedByName: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val completedAt: String = "",
    val reopeningCount: Int = 0,
    val auditLog: List<AuditLogEntry> = emptyList()
) : Parcelable {
    
    fun calculateBalance(): Int {
        return totalSent - totalReturned + totalRedispatched
    }
    
    fun isOverdue(): Boolean {
        return com.sonia.gatepass.util.DateUtil.isDatePassed(returnableDate) &&
                status !in listOf(
                    com.sonia.gatepass.util.Constants.STATUS_COMPLETED,
                    com.sonia.gatepass.util.Constants.STATUS_REJECTED
                )
    }
}

@Parcelize
data class AuditLogEntry(
    val action: String = "",
    val performedBy: String = "",
    val performedByName: String = "",
    val timestamp: String = "",
    val details: String = ""
) : Parcelable
