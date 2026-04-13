package com.sonia.gatepass.data.model

import android.os.Parcelable
import com.sonia.gatepass.util.Constants
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val userId: String = "",
    val name: String = "",
    val role: String = Constants.ROLE_USER,
    val email: String = "",
    val createdAt: String = "",
    val isActive: Boolean = true
) : Parcelable {

    // Custom Firestore deserialization constructor support
    constructor() : this("", "", Constants.ROLE_USER, "", "", true)

    companion object {
        /**
         * Helper to safely parse isActive from Firestore.
         * Handles both Boolean and String values ("true"/"false").
         */
        fun parseIsActive(value: Any?): Boolean {
            return when (value) {
                is Boolean -> value
                is String -> value.equals("true", ignoreCase = true)
                is Number -> value.toLong() != 0L
                else -> true
            }
        }
    }
}
