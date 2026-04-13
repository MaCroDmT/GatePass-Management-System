package com.sonia.gatepass.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonia.gatepass.data.model.User
import com.sonia.gatepass.data.repository.AuthRepository
import com.sonia.gatepass.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserManagementViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _users = MutableStateFlow<Resource<List<User>>>(Resource.Loading())
    val users: StateFlow<Resource<List<User>>> = _users.asStateFlow()

    private val _actionResult = MutableStateFlow<Resource<Unit>?>(null)
    val actionResult: StateFlow<Resource<Unit>?> = _actionResult.asStateFlow()

    private val _createResult = MutableStateFlow<Resource<String>?>(null)
    val createResult: StateFlow<Resource<String>?> = _createResult.asStateFlow()

    fun observeAllUsers() {
        viewModelScope.launch {
            repository.observeAllUsers().collect { result ->
                _users.value = result
            }
        }
    }

    fun createUser(email: String, password: String, name: String, role: String) {
        viewModelScope.launch {
            _createResult.value = Resource.Loading()
            val result = repository.createUserWithEmailAndPassword(email, password, name, role)
            _createResult.value = result
        }
    }

    fun updateUser(userId: String, name: String, email: String, role: String) {
        viewModelScope.launch {
            _actionResult.value = Resource.Loading()
            val result = repository.updateUser(userId, name, email, role)
            _actionResult.value = result
        }
    }

    fun toggleUserActive(userId: String) {
        viewModelScope.launch {
            _actionResult.value = Resource.Loading()
            val result = repository.toggleUserActive(userId)
            _actionResult.value = result
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            _actionResult.value = Resource.Loading()
            val result = repository.deleteUser(userId)
            _actionResult.value = result
        }
    }

    fun resetActionResult() {
        _actionResult.value = null
    }

    fun resetCreateResult() {
        _createResult.value = null
    }
}
