package com.sonia.gatepass.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.sonia.gatepass.data.model.GatePass
import com.sonia.gatepass.data.model.Movement
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

class MovementRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val movementCollection = firestore.collection(Constants.COLLECTION_MOVEMENTS)
    private val gatePassCollection = firestore.collection(Constants.COLLECTION_GATE_PASS)

    private val dateTimeFormat = SimpleDateFormat(Constants.DATE_TIME_FORMAT, Locale.getDefault())

    private fun List<Movement>.sortByCreatedAtDesc(): List<Movement> {
        return sortedByDescending { m ->
            try {
                dateTimeFormat.parse(m.createdAt)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }
    
    suspend fun recordMovement(movement: Movement): Resource<String> {
        return try {
            val movementId = UUID.randomUUID().toString()
            val newMovement = movement.copy(
                movementId = movementId,
                createdAt = DateUtil.getCurrentDateTime()
            )
            
            movementCollection.document(movementId).set(newMovement).await()
            
            // Update gate pass quantities
            updateGatePassQuantities(movement.gpid, movement.type, movement.quantity)
            
            Resource.Success(movementId)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to record movement")
        }
    }
    
    private suspend fun updateGatePassQuantities(gpid: String, type: String, quantity: Int) {
        val gatePassDoc = gatePassCollection.document(gpid).get().await()
        val gatePass = gatePassDoc.toObject(GatePass::class.java) ?: return
        
        val updatedReturned = if (type == Constants.MOVEMENT_INWARD) {
            gatePass.totalReturned + quantity
        } else {
            gatePass.totalReturned
        }
        
        val updatedRedispatched = if (type == Constants.MOVEMENT_RE_DISPATCH) {
            gatePass.totalRedispatched + quantity
        } else {
            gatePass.totalRedispatched
        }
        
        val updatedSent = if (type == Constants.MOVEMENT_OUTWARD) {
            gatePass.totalSent + quantity
        } else {
            gatePass.totalSent
        }
        
        val balance = updatedSent - updatedReturned + updatedRedispatched
        
        gatePassCollection.document(gpid).update(
            mapOf(
                "totalSent" to updatedSent,
                "totalReturned" to updatedReturned,
                "totalRedispatched" to updatedRedispatched,
                "balanceQuantity" to balance
            )
        ).await()
    }
    
    fun observeMovementsByGPID(gpid: String): Flow<Resource<List<Movement>>> = callbackFlow {
        val listener = movementCollection
            .whereEqualTo("gpid", gpid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to listen"))
                    return@addSnapshotListener
                }

                val movements = snapshot?.documents?.mapNotNull {
                    it.toObject(Movement::class.java)
                }?.sortByCreatedAtDesc() ?: emptyList()
                trySend(Resource.Success(movements))
            }

        awaitClose { listener.remove() }
    }
    
    suspend fun getMovementsByGPID(gpid: String): Resource<List<Movement>> {
        return try {
            val snapshot = movementCollection
                .whereEqualTo("gpid", gpid)
                .get()
                .await()

            val movements = snapshot.documents.mapNotNull {
                it.toObject(Movement::class.java)
            }.sortByCreatedAtDesc()
            Resource.Success(movements)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get movements")
        }
    }
}
