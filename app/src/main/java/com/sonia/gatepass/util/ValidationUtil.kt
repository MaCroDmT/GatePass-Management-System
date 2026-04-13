package com.sonia.gatepass.util

import android.util.Patterns
import java.util.Locale

object ValidationUtil {
    
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }
    
    fun isValidQuantity(quantity: String): Boolean {
        return quantity.isNotBlank() && quantity.toIntOrNull() ?: 0 > 0
    }
    
    fun isNotBlank(value: String): Boolean {
        return value.trim().isNotBlank()
    }
    
    fun validateEmail(email: String): String? {
        return when {
            !isNotBlank(email) -> "Email is required"
            !isValidEmail(email) -> "Invalid email address"
            else -> null
        }
    }
    
    fun validatePassword(password: String): String? {
        return when {
            !isNotBlank(password) -> "Password is required"
            !isValidPassword(password) -> "Password must be at least 6 characters"
            else -> null
        }
    }
    
    fun validateRequired(value: String?, fieldName: String): String? {
        return when {
            value.isNullOrBlank() -> "$fieldName is required"
            else -> null
        }
    }
    
    fun validateQuantity(value: String?): String? {
        return when {
            value.isNullOrBlank() -> "Quantity is required"
            value.toIntOrNull() == null -> "Quantity must be a number"
            value.toInt() <= 0 -> "Quantity must be greater than 0"
            else -> null
        }
    }
    
    fun capitalizeFirst(str: String): String {
        return str.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}
