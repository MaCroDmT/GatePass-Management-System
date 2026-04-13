package com.sonia.gatepass.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.sonia.gatepass.data.model.AuditLogEntry
import com.sonia.gatepass.data.model.GatePass
import com.sonia.gatepass.util.Constants
import com.sonia.gatepass.util.DateUtil
import com.sonia.gatepass.util.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class GatePassRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val gatePassCollection = firestore.collection(Constants.COLLECTION_GATE_PASS)

    private val dateTimeFormat = SimpleDateFormat(Constants.DATE_TIME_FORMAT, Locale.getDefault())

    private fun List<GatePass>.sortByCreatedAtDesc(): List<GatePass> {
        return sortedByDescending { gp ->
            try {
                dateTimeFormat.parse(gp.createdAt)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }
    
    suspend fun getNextGPID(): Resource<String> {
        return try {
            val snapshot = gatePassCollection
                .orderBy("gpid", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            
            val nextId = if (snapshot.isEmpty) {
                1
            } else {
                val lastDoc = snapshot.documents[0]
                val lastGPID = lastDoc.getString("gpid") ?: "GP0"
                val lastNumber = lastGPID.replace(Constants.GPID_PREFIX, "").toIntOrNull() ?: 0
                lastNumber + 1
            }
            
            Resource.Success("${Constants.GPID_PREFIX}$nextId")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to generate GPID")
        }
    }
    
    suspend fun createGatePass(gatePass: GatePass): Resource<String> {
        return try {
            val gpid = gatePass.gpid.ifBlank {
                val result = getNextGPID()
                if (result.data != null) result.data!! else UUID.randomUUID().toString()
            }
            
            val newGatePass = gatePass.copy(
                gpid = gpid,
                createdAt = DateUtil.getCurrentDateTime(),
                updatedAt = DateUtil.getCurrentDateTime()
            )
            
            gatePassCollection.document(gpid).set(newGatePass).await()
            Resource.Success(gpid)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create gate pass")
        }
    }
    
    suspend fun getGatePass(gpid: String): Resource<GatePass> {
        return try {
            val doc = gatePassCollection.document(gpid).get().await()
            val gatePass = doc.toObject(GatePass::class.java)
            if (gatePass != null) {
                Resource.Success(gatePass)
            } else {
                Resource.Error("Gate Pass not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get gate pass")
        }
    }
    
    fun observeAllGatePasses(): Flow<Resource<List<GatePass>>> = callbackFlow {
        val listener = gatePassCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to listen"))
                    return@addSnapshotListener
                }
                
                val gatePasses = snapshot?.documents?.mapNotNull { 
                    it.toObject(GatePass::class.java) 
                } ?: emptyList()
                trySend(Resource.Success(gatePasses))
            }
        
        awaitClose { listener.remove() }
    }
    
    fun observeGatePassesByStatus(status: String): Flow<Resource<List<GatePass>>> = callbackFlow {
        val listener = gatePassCollection
            .whereEqualTo("status", status)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to listen"))
                    return@addSnapshotListener
                }

                val gatePasses = snapshot?.documents?.mapNotNull {
                    it.toObject(GatePass::class.java)
                }?.sortByCreatedAtDesc() ?: emptyList()
                trySend(Resource.Success(gatePasses))
            }

        awaitClose { listener.remove() }
    }
    
    fun observeGatePassesByCreatedBy(createdBy: String): Flow<Resource<List<GatePass>>> = callbackFlow {
        val listener = gatePassCollection
            .whereEqualTo("createdBy", createdBy)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to listen"))
                    return@addSnapshotListener
                }

                val gatePasses = snapshot?.documents?.mapNotNull {
                    it.toObject(GatePass::class.java)
                }?.sortByCreatedAtDesc() ?: emptyList()
                trySend(Resource.Success(gatePasses))
            }

        awaitClose { listener.remove() }
    }
    
    suspend fun updateGatePassStatus(
        gpid: String, 
        status: String, 
        updatedBy: String, 
        updatedByName: String
    ): Resource<Unit> {
        return try {
            val auditLog = AuditLogEntry(
                action = "Status changed to $status",
                performedBy = updatedBy,
                performedByName = updatedByName,
                timestamp = DateUtil.getCurrentDateTime(),
                details = "Gate pass status updated"
            )
            
            gatePassCollection.document(gpid).update(
                mapOf(
                    "status" to status,
                    "updatedAt" to DateUtil.getCurrentDateTime(),
                    "auditLog" to FieldValue.arrayUnion(auditLog)
                )
            ).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update gate pass status")
        }
    }
    
    suspend fun approveGatePass(
        gpid: String, 
        approvedBy: String, 
        approvedByName: String
    ): Resource<Unit> {
        return try {
            val auditLog = AuditLogEntry(
                action = "Approved",
                performedBy = approvedBy,
                performedByName = approvedByName,
                timestamp = DateUtil.getCurrentDateTime(),
                details = "Gate pass approved"
            )
            
            gatePassCollection.document(gpid).update(
                mapOf(
                    "status" to Constants.STATUS_APPROVED,
                    "approvedBy" to approvedBy,
                    "approvedByName" to approvedByName,
                    "updatedAt" to DateUtil.getCurrentDateTime(),
                    "auditLog" to FieldValue.arrayUnion(auditLog)
                )
            ).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to approve gate pass")
        }
    }
    
    suspend fun rejectGatePass(
        gpid: String, 
        rejectedBy: String, 
        rejectedByName: String
    ): Resource<Unit> {
        return try {
            val auditLog = AuditLogEntry(
                action = "Rejected",
                performedBy = rejectedBy,
                performedByName = rejectedByName,
                timestamp = DateUtil.getCurrentDateTime(),
                details = "Gate pass rejected"
            )
            
            gatePassCollection.document(gpid).update(
                mapOf(
                    "status" to Constants.STATUS_REJECTED,
                    "updatedAt" to DateUtil.getCurrentDateTime(),
                    "auditLog" to FieldValue.arrayUnion(auditLog)
                )
            ).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to reject gate pass")
        }
    }
    
    suspend fun markCompleted(gpid: String): Resource<Unit> {
        return try {
            val auditLog = AuditLogEntry(
                action = "Completed",
                performedBy = "System",
                performedByName = "System",
                timestamp = DateUtil.getCurrentDateTime(),
                details = "Gate pass marked as completed"
            )
            
            gatePassCollection.document(gpid).update(
                mapOf(
                    "status" to Constants.STATUS_COMPLETED,
                    "completedAt" to DateUtil.getCurrentDateTime(),
                    "updatedAt" to DateUtil.getCurrentDateTime(),
                    "auditLog" to FieldValue.arrayUnion(auditLog)
                )
            ).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to complete gate pass")
        }
    }
    
    suspend fun reopenGatePass(
        gpid: String, 
        reopenedBy: String, 
        reopenedByName: String
    ): Resource<Unit> {
        return try {
            val gatePass = getGatePass(gpid)
            if (gatePass.data == null) {
                return Resource.Error("Gate Pass not found")
            }
            
            val currentReopeningCount = gatePass.data!!.reopeningCount
            val auditLog = AuditLogEntry(
                action = "Reopened",
                performedBy = reopenedBy,
                performedByName = reopenedByName,
                timestamp = DateUtil.getCurrentDateTime(),
                details = "Gate pass reopened - Reopening #${currentReopeningCount + 1}"
            )
            
            gatePassCollection.document(gpid).update(
                mapOf(
                    "status" to Constants.STATUS_REOPENED,
                    "reopeningCount" to currentReopeningCount + 1,
                    "updatedAt" to DateUtil.getCurrentDateTime(),
                    "auditLog" to FieldValue.arrayUnion(auditLog)
                )
            ).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to reopen gate pass")
        }
    }
    
    suspend fun updateQuantities(
        gpid: String,
        totalReturned: Int,
        totalRedispatched: Int
    ): Resource<Unit> {
        return try {
            gatePassCollection.document(gpid).update(
                mapOf(
                    "totalReturned" to totalReturned,
                    "totalRedispatched" to totalRedispatched,
                    "updatedAt" to DateUtil.getCurrentDateTime()
                )
            ).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update quantities")
        }
    }

    suspend fun deleteGatePass(gpid: String): Resource<Unit> {
        return try {
            // Delete related movements first
            val movementsSnapshot = firestore.collection(Constants.COLLECTION_MOVEMENTS)
                .whereEqualTo("gpid", gpid)
                .get()
                .await()

            val batch = firestore.batch()
            for (doc in movementsSnapshot.documents) {
                batch.delete(doc.reference)
            }

            // Delete the gate pass document
            batch.delete(gatePassCollection.document(gpid))
            batch.commit().await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete gate pass")
        }
    }

    fun observeGatePassesByStyle(styleNo: String): Flow<Resource<List<GatePass>>> = callbackFlow {
        val listener = gatePassCollection
            .whereEqualTo("styleNo", styleNo)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to listen"))
                    return@addSnapshotListener
                }

                val gatePasses = snapshot?.documents?.mapNotNull {
                    it.toObject(GatePass::class.java)
                }?.sortByCreatedAtDesc() ?: emptyList()
                trySend(Resource.Success(gatePasses))
            }

        awaitClose { listener.remove() }
    }
    
    suspend fun getAllGatePasses(): Resource<List<GatePass>> {
        return try {
            val snapshot = gatePassCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val gatePasses = snapshot.documents.mapNotNull { 
                it.toObject(GatePass::class.java) 
            }
            Resource.Success(gatePasses)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get gate passes")
        }
    }
}
