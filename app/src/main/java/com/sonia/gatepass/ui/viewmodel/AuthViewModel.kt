package com.sonia.gatepass.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonia.gatepass.data.model.User
import com.sonia.gatepass.data.repository.AuthRepository
import com.sonia.gatepass.data.repository.NotificationRepository
import com.sonia.gatepass.util.Resource
import com.sonia.gatepass.util.SharedPrefUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    // Null means "not attempted yet" — prevents false loading state
    private val _loginState = MutableStateFlow<Resource<User>?>(null)
    val loginState: StateFlow<Resource<User>?> = _loginState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = Resource.Loading()
            val result = repository.login(email, password)
            _loginState.value = result

            // If login succeeded, also update currentUser
            if (result is Resource.Success) {
                _currentUser.value = result.data
            }
        }
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            val userId = repository.getCurrentUserId()
            if (userId != null) {
                val result = repository.getUserData(userId)
                if (result is Resource.Success) {
                    _currentUser.value = result.data
                }
            }
        }
    }

    fun logout() {
        NotificationRepository().unsubscribeFromAllTopics()
        repository.logout()
        _currentUser.value = null
    }

    fun getCurrentUserId(): String? {
        return repository.getCurrentUserId()
    }

    fun getCurrentUserEmail(): String? {
        return repository.getCurrentUserEmail()
    }

    fun isLoggedIn(): Boolean {
        return repository.isLoggedIn()
    }

    fun resetLoginState() {
        _loginState.value = null
    }
}
