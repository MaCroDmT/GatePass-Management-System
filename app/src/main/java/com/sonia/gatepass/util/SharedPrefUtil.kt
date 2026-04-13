package com.sonia.gatepass.util

import android.content.Context
import android.content.SharedPreferences
import com.sonia.gatepass.util.Constants.PREF_IS_LOGGED_IN
import com.sonia.gatepass.util.Constants.PREF_NAME
import com.sonia.gatepass.util.Constants.PREF_USER_ID
import com.sonia.gatepass.util.Constants.PREF_USER_NAME
import com.sonia.gatepass.util.Constants.PREF_USER_ROLE

class SharedPrefUtil(context: Context) {
    
    private val preferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    fun saveUserId(userId: String) {
        preferences.edit().putString(PREF_USER_ID, userId).apply()
    }
    
    fun getUserId(): String? {
        return preferences.getString(PREF_USER_ID, null)
    }
    
    fun saveUserRole(role: String) {
        preferences.edit().putString(PREF_USER_ROLE, role).apply()
    }
    
    fun getUserRole(): String? {
        return preferences.getString(PREF_USER_ROLE, null)
    }
    
    fun saveUserName(name: String) {
        preferences.edit().putString(PREF_USER_NAME, name).apply()
    }
    
    fun getUserName(): String? {
        return preferences.getString(PREF_USER_NAME, null)
    }
    
    fun saveIsLoggedIn(isLoggedIn: Boolean) {
        preferences.edit().putBoolean(PREF_IS_LOGGED_IN, isLoggedIn).apply()
    }
    
    fun isLoggedIn(): Boolean {
        return preferences.getBoolean(PREF_IS_LOGGED_IN, false)
    }
    
    fun clear() {
        preferences.edit().clear().apply()
    }
    
    fun isSuperAdmin(): Boolean {
        return getUserRole() == Constants.ROLE_SUPER_ADMIN
    }
    
    fun isAdmin(): Boolean {
        return getUserRole() == Constants.ROLE_ADMIN || isSuperAdmin()
    }
    
    fun isProductionUser(): Boolean {
        return getUserRole() == Constants.ROLE_USER || isAdmin()
    }
}
