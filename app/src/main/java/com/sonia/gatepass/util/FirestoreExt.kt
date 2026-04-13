package com.sonia.gatepass.util

import com.google.firebase.firestore.DocumentSnapshot
import com.sonia.gatepass.data.model.User

/**
 * Safely converts a Firestore DocumentSnapshot to a User object,
 * handling the case where isActive might be stored as a String ("true"/"false")
 * instead of a proper Boolean.
 */
fun DocumentSnapshot.toUser(): User? {
    return try {
        val data = this.data ?: return null
        val userId = data["userId"] as? String ?: this.id
        val name = data["name"] as? String ?: ""
        val role = data["role"] as? String ?: Constants.ROLE_USER
        val email = data["email"] as? String ?: ""
        val createdAt = data["createdAt"] as? String ?: ""
        val isActiveRaw = data["isActive"]
        val isActive = User.parseIsActive(isActiveRaw)

        User(
            userId = userId,
            name = name,
            role = role,
            email = email,
            createdAt = createdAt,
            isActive = isActive
        )
    } catch (e: Exception) {
        null
    }
}

/**
 * Converts a list of DocumentSnapshots to a list of User objects.
 */
fun List<DocumentSnapshot>.toUserList(): List<User> {
    return mapNotNull { it.toUser() }
}
