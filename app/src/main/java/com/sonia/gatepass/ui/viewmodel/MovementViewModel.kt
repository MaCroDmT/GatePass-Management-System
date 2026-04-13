package com.sonia.gatepass.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonia.gatepass.data.model.Movement
import com.sonia.gatepass.data.repository.MovementRepository
import com.sonia.gatepass.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MovementViewModel : ViewModel() {
    
    private val repository = MovementRepository()
    
    private val _movements = MutableStateFlow<Resource<List<Movement>>>(Resource.Loading())
    val movements: StateFlow<Resource<List<Movement>>> = _movements.asStateFlow()
    
    private val _recordResult = MutableStateFlow<Resource<String>?>(null)
    val recordResult: StateFlow<Resource<String>?> = _recordResult.asStateFlow()
    
    fun observeMovements(gpid: String) {
        viewModelScope.launch {
            repository.observeMovementsByGPID(gpid).collect { result ->
                _movements.value = result
            }
        }
    }
    
    fun recordMovement(movement: Movement) {
        viewModelScope.launch {
            _recordResult.value = Resource.Loading()
            val result = repository.recordMovement(movement)
            _recordResult.value = result
        }
    }
    
    fun getMovements(gpid: String) {
        viewModelScope.launch {
            _movements.value = Resource.Loading()
            val result = repository.getMovementsByGPID(gpid)
            _movements.value = result
        }
    }
    
    fun resetRecordResult() {
        _recordResult.value = null
    }
}
