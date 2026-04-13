package com.sonia.gatepass.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtil {
    
    private val dateFormat = SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat(Constants.DATE_TIME_FORMAT, Locale.getDefault())
    
    fun getCurrentDate(): String {
        return dateFormat.format(Date())
    }
    
    fun getCurrentDateTime(): String {
        return dateTimeFormat.format(Date())
    }
    
    fun formatDate(date: Date): String {
        return dateFormat.format(date)
    }
    
    fun formatDate(dateString: String): Date? {
        return try {
            dateFormat.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
    
    fun isDatePassed(dateString: String): Boolean {
        val date = formatDate(dateString) ?: return false
        return date.before(Date())
    }
    
    fun getDaysDifference(dateString: String): Long {
        val date = formatDate(dateString) ?: return 0
        val diffInMillis = date.time - Date().time
        return diffInMillis / (24 * 60 * 60 * 1000)
    }
    
    fun addDays(days: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, days)
        return dateFormat.format(calendar.time)
    }
}
