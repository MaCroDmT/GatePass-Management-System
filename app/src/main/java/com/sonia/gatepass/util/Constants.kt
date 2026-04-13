package com.sonia.gatepass.util

object Constants {
    
    // Firestore Collections
    const val COLLECTION_USERS = "Users"
    const val COLLECTION_GATE_PASS = "GatePass"
    const val COLLECTION_MOVEMENTS = "Movements"
    const val COLLECTION_NOTIFICATIONS = "Notifications"
    
    // User Roles
    const val ROLE_SUPER_ADMIN = "SuperAdmin"
    const val ROLE_ADMIN = "Admin"
    const val ROLE_USER = "User"
    
    // Gate Pass Status
    const val STATUS_PENDING = "PENDING"
    const val STATUS_APPROVED = "APPROVED"
    const val STATUS_IN_PROGRESS = "IN_PROGRESS"
    const val STATUS_PARTIALLY_RETURNED = "PARTIALLY_RETURNED"
    const val STATUS_REDISPATCHED = "REDISPATCHED"
    const val STATUS_COMPLETED = "COMPLETED"
    const val STATUS_REOPENED = "REOPENED"
    const val STATUS_REJECTED = "REJECTED"
    
    // Movement Types
    const val MOVEMENT_OUTWARD = "OUTWARD"
    const val MOVEMENT_INWARD = "INWARD"
    const val MOVEMENT_RE_DISPATCH = "RE_DISPATCH"
    
    // Notification Status
    const val NOTIFICATION_READ = "READ"
    const val NOTIFICATION_UNREAD = "UNREAD"
    
    // Shared Preferences
    const val PREF_NAME = "GatePassPrefs"
    const val PREF_USER_ID = "userId"
    const val PREF_USER_ROLE = "userRole"
    const val PREF_USER_NAME = "userName"
    const val PREF_IS_LOGGED_IN = "isLoggedIn"
    
    // PDF
    const val PDF_DIR = "GatePassPDFs"
    
    // Date Format
    const val DATE_FORMAT = "dd/MM/yyyy"
    const val DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm"
    const val GPID_PREFIX = "GP"
}
