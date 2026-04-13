package com.sonia.gatepass.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonia.gatepass.data.model.GatePass
import com.sonia.gatepass.data.repository.GatePassRepository
import com.sonia.gatepass.data.repository.NotificationRepository
import com.sonia.gatepass.util.Constants
import com.sonia.gatepass.util.DateUtil
import com.sonia.gatepass.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GatePassViewModel : ViewModel() {

    private val repository = GatePassRepository()
    private val notificationRepository = NotificationRepository()

    private val _gatePasses = MutableStateFlow<Resource<List<GatePass>>>(Resource.Loading())
    val gatePasses: StateFlow<Resource<List<GatePass>>> = _gatePasses.asStateFlow()

    private val _gatePass = MutableStateFlow<Resource<GatePass>?>(null)
    val gatePass: StateFlow<Resource<GatePass>?> = _gatePass.asStateFlow()

    private val _createResult = MutableStateFlow<Resource<String>?>(null)
    val createResult: StateFlow<Resource<String>?> = _createResult.asStateFlow()

    private val _actionResult = MutableStateFlow<Resource<Unit>?>(null)
    val actionResult: StateFlow<Resource<Unit>?> = _actionResult.asStateFlow()

    private val _nextGPID = MutableStateFlow<Resource<String>?>(null)
    val nextGPID: StateFlow<Resource<String>?> = _nextGPID.asStateFlow()

    fun observeAllGatePasses() {
        viewModelScope.launch {
            repository.observeAllGatePasses().collect { result ->
                _gatePasses.value = result
            }
        }
    }

    fun observeGatePassesByStatus(status: String) {
        viewModelScope.launch {
            repository.observeGatePassesByStatus(status).collect { result ->
                _gatePasses.value = result
            }
        }
    }

    fun observeGatePassesByCreatedBy(createdBy: String) {
        viewModelScope.launch {
            repository.observeGatePassesByCreatedBy(createdBy).collect { result ->
                _gatePasses.value = result
            }
        }
    }

    fun getGatePass(gpid: String) {
        viewModelScope.launch {
            _gatePass.value = Resource.Loading()
            val result = repository.getGatePass(gpid)
            _gatePass.value = result
        }
    }

    fun createGatePass(gatePass: GatePass) {
        viewModelScope.launch {
            _createResult.value = Resource.Loading()
            val result = repository.createGatePass(gatePass)
            
            if (result is Resource.Success) {
                // Notify admins about the new gate pass
                notificationRepository.sendNotificationToRole(
                    role = Constants.ROLE_ADMIN,
                    title = "New Gate Pass Created",
                    message = "${gatePass.createdByName} created gate pass ${result.data} for review",
                    gpid = result.data!!,
                    type = "CREATED"
                )
            }
            
            _createResult.value = result
        }
    }

    fun approveGatePass(gpid: String, approvedBy: String, approvedByName: String) {
        viewModelScope.launch {
            _actionResult.value = Resource.Loading()
            val result = repository.approveGatePass(gpid, approvedBy, approvedByName)
            
            if (result is Resource.Success) {
                // Send notification to the creator
                notificationRepository.sendNotificationToRole(
                    role = Constants.ROLE_USER,
                    title = "Gate Pass Approved",
                    message = "Your gate pass $gpid has been approved by $approvedByName",
                    gpid = gpid,
                    type = "APPROVED"
                )
            }
            
            _actionResult.value = result
        }
    }

    fun rejectGatePass(gpid: String, rejectedBy: String, rejectedByName: String) {
        viewModelScope.launch {
            _actionResult.value = Resource.Loading()
            val result = repository.rejectGatePass(gpid, rejectedBy, rejectedByName)
            
            if (result is Resource.Success) {
                // Send notification to the creator
                notificationRepository.sendNotificationToRole(
                    role = Constants.ROLE_USER,
                    title = "Gate Pass Rejected",
                    message = "Your gate pass $gpid was rejected by $rejectedByName",
                    gpid = gpid,
                    type = "REJECTED"
                )
            }
            
            _actionResult.value = result
        }
    }

    fun markCompleted(gpid: String) {
        viewModelScope.launch {
            _actionResult.value = Resource.Loading()
            val result = repository.markCompleted(gpid)
            
            if (result is Resource.Success) {
                // Send notification to admins about the completed gate pass
                notificationRepository.sendNotificationToRole(
                    role = Constants.ROLE_ADMIN,
                    title = "Gate Pass Completed",
                    message = "Gate pass $gpid has been marked as completed",
                    gpid = gpid,
                    type = "COMPLETED"
                )
            }
            
            _actionResult.value = result
        }
    }

    fun reopenGatePass(gpid: String, reopenedBy: String, reopenedByName: String) {
        viewModelScope.launch {
            _actionResult.value = Resource.Loading()
            val result = repository.reopenGatePass(gpid, reopenedBy, reopenedByName)
            
            if (result is Resource.Success) {
                // Send notification to admins about the reopened gate pass
                notificationRepository.sendNotificationToRole(
                    role = Constants.ROLE_ADMIN,
                    title = "Gate Pass Reopened",
                    message = "$reopenedByName reopened gate pass $gpid for further processing",
                    gpid = gpid,
                    type = "REOPENED"
                )
            }
            
            _actionResult.value = result
        }
    }

    fun deleteGatePass(gpid: String) {
        viewModelScope.launch {
            _actionResult.value = Resource.Loading()
            val result = repository.deleteGatePass(gpid)
            _actionResult.value = result
        }
    }

    fun updateGatePassStatus(gpid: String, status: String, updatedBy: String, updatedByName: String) {
        viewModelScope.launch {
            _actionResult.value = Resource.Loading()
            val result = repository.updateGatePassStatus(gpid, status, updatedBy, updatedByName)
            _actionResult.value = result
        }
    }

    fun getNextGPID() {
        viewModelScope.launch {
            _nextGPID.value = Resource.Loading()
            val result = repository.getNextGPID()
            _nextGPID.value = result
        }
    }

    fun resetCreateResult() {
        _createResult.value = null
    }

    fun resetActionResult() {
        _actionResult.value = null
    }

    fun getGatePassesByStyle(styleNo: String): List<GatePass> {
        val result = (_gatePasses.value as? Resource.Success)?.data ?: emptyList()
        return result.filter { it.styleNo == styleNo }
    }

    fun getStyleAggregates(styleNo: String): Map<String, Int> {
        val gatePasses = getGatePassesByStyle(styleNo)
        val totalSent = gatePasses.sumOf { it.totalSent }
        val totalReturned = gatePasses.sumOf { it.totalReturned }
        val totalRedispatched = gatePasses.sumOf { it.totalRedispatched }
        val balance = totalSent - totalReturned + totalRedispatched

        return mapOf(
            "totalSent" to totalSent,
            "totalReturned" to totalReturned,
            "totalRedispatched" to totalRedispatched,
            "balance" to balance
        )
    }
}
