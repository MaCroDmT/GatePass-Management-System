package com.sonia.gatepass.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonia.gatepass.data.model.Notification
import com.sonia.gatepass.data.repository.NotificationRepository
import com.sonia.gatepass.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {
    
    private val repository = NotificationRepository()
    
    private val _notifications = MutableStateFlow<Resource<List<Notification>>>(Resource.Loading())
    val notifications: StateFlow<Resource<List<Notification>>> = _notifications.asStateFlow()
    
    private val _unreadCount = MutableStateFlow<Resource<Int>?>(null)
    val unreadCount: StateFlow<Resource<Int>?> = _unreadCount.asStateFlow()
    
    private val _actionResult = MutableStateFlow<Resource<Unit>?>(null)
    val actionResult: StateFlow<Resource<Unit>?> = _actionResult.asStateFlow()
    
    fun observeNotifications(userId: String) {
        viewModelScope.launch {
            repository.observeNotificationsByUserId(userId).collect { result ->
                _notifications.value = result
            }
        }
    }
    
    fun getUnreadCount(userId: String) {
        viewModelScope.launch {
            _unreadCount.value = Resource.Loading()
            val result = repository.getUnreadCount(userId)
            _unreadCount.value = result
        }
    }
    
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            _actionResult.value = Resource.Loading()
            val result = repository.markAsRead(notificationId)
            _actionResult.value = result
        }
    }
    
    fun markAllAsRead(userId: String) {
        viewModelScope.launch {
            _actionResult.value = Resource.Loading()
            val result = repository.markAllAsRead(userId)
            _actionResult.value = result
        }
    }
    
    fun resetActionResult() {
        _actionResult.value = null
    }
}
