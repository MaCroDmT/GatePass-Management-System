package com.sonia.gatepass.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.sonia.gatepass.GatePassApplication
import com.sonia.gatepass.data.model.User
import com.sonia.gatepass.util.Constants
import com.sonia.gatepass.util.DateUtil
import com.sonia.gatepass.util.Resource
import com.sonia.gatepass.util.toUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun login(email: String, password: String): Resource<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: return Resource.Error("User ID not found")

            // First try: document ID matches Firebase Auth UID
            var userDoc = firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            var user = userDoc.toUser()

            // Second try: if not found, search by email
            if (user == null || !user.isActive) {
                val emailQuery = firestore.collection(Constants.COLLECTION_USERS)
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .await()

                if (!emailQuery.isEmpty) {
                    user = emailQuery.documents[0].toUser()
                }
            }

            if (user != null && user.isActive) {
                Resource.Success(user)
            } else {
                Resource.Error("User not found in database. Please create a Users document with your email: '$email'")
            }
        } catch (e: Exception) {
            Resource.Error("Login failed: ${e.message}")
        }
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun getUserData(userId: String): Resource<User> {
        return try {
            // First try: document ID matches Firebase Auth UID
            var userDoc = firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            var user = userDoc.toUser()

            // Fallback: if document ID doesn't match, search by userId field
            if (user == null) {
                val query = firestore.collection(Constants.COLLECTION_USERS)
                    .whereEqualTo("userId", userId)
                    .limit(1)
                    .get()
                    .await()

                if (!query.isEmpty) {
                    user = query.documents[0].toUser()
                }
            }

            // Second fallback: search by email (for legacy setups)
            if (user == null) {
                val email = auth.currentUser?.email
                if (email != null) {
                    val emailQuery = firestore.collection(Constants.COLLECTION_USERS)
                        .whereEqualTo("email", email)
                        .limit(1)
                        .get()
                        .await()

                    if (!emailQuery.isEmpty) {
                        user = emailQuery.documents[0].toUser()
                    }
                }
            }

            if (user != null) {
                Resource.Success(user)
            } else {
                Resource.Error("User not found in Firestore")
            }
        } catch (e: Exception) {
            Resource.Error("Failed to get user data: ${e.message}")
        }
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun observeUserChanges(): Flow<Resource<User>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            trySend(Resource.Error("User not logged in"))
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to listen to changes"))
                    return@addSnapshotListener
                }

                val user = snapshot?.toUser()
                if (user != null) {
                    trySend(Resource.Success(user))
                } else {
                    trySend(Resource.Error("User not found"))
                }
            }

        awaitClose { listener.remove() }
    }

    // ==================== User Management (Super Admin) ====================

    fun observeAllUsers(): Flow<Resource<List<User>>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_USERS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to listen"))
                    return@addSnapshotListener
                }

                val users = snapshot?.documents?.mapNotNull {
                    it.toUser()
                }?.sortedBy { it.name.lowercase() } ?: emptyList()
                trySend(Resource.Success(users))
            }

        awaitClose { listener.remove() }
    }

    suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        name: String,
        role: String
    ): Resource<String> {
        return try {
            // Use a SECONDARY Firebase Auth instance to avoid logging out the current user
            val primaryApp = FirebaseApp.getInstance()
            val secondaryApp = FirebaseApp.initializeApp(
                GatePassApplication.instance,
                FirebaseOptions.Builder(primaryApp.options).build(),
                "SecondaryAuth"
            )

            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)

            // Create user in Firebase Authentication using secondary instance
            val authResult = secondaryAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUid = authResult.user?.uid
                ?: return Resource.Error("Failed to create authentication user")

            // Use the PRIMARY Firebase instance to create the Firestore document
            val user = User(
                userId = firebaseUid,
                name = name,
                role = role,
                email = email,
                createdAt = DateUtil.getCurrentDate(),
                isActive = true
            )

            firestore.collection(Constants.COLLECTION_USERS)
                .document(firebaseUid)
                .set(user)
                .await()

            // Clean up the secondary auth instance
            secondaryApp.delete()

            Resource.Success(firebaseUid)
        } catch (e: Exception) {
            cleanupSecondaryAuth()
            Resource.Error("Failed to create user: ${e.message}")
        }
    }

    private fun cleanupSecondaryAuth() {
        try {
            FirebaseApp.getInstance("SecondaryAuth")?.delete()
        } catch (_: Exception) {}
    }

    suspend fun updateUser(userId: String, name: String, email: String, role: String): Resource<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "name" to name,
                "email" to email,
                "role" to role
            )

            firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .await()

            // Also update email in Firebase Auth if it changed
            val currentUser = auth.currentUser
            if (currentUser != null && currentUser.uid == userId) {
                // Can't update own email while logged in easily — skip for now
            } else {
                // For other users, we'd need Admin SDK — skip for client-side
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to update user: ${e.message}")
        }
    }

    suspend fun toggleUserActive(userId: String): Resource<Unit> {
        return try {
            val userDoc = firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            val currentUser = userDoc.toUser()
                ?: return Resource.Error("User not found")

            firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update("isActive", !currentUser.isActive)
                .await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to toggle user status: ${e.message}")
        }
    }

    suspend fun deleteUser(userId: String): Resource<Unit> {
        return try {
            // Prevent self-deletion
            val currentUserId = getCurrentUserId()
            if (currentUserId == userId) {
                return Resource.Error("You cannot delete your own account")
            }

            // Delete Firestore document
            firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .delete()
                .await()

            // Delete from Firebase Auth
            val authUser = auth.currentUser
            if (authUser != null && authUser.uid == userId) {
                // Can't delete own auth while logged in
            } else {
                // For other users — client SDK can only delete current user
                // Full deletion requires Firebase Admin SDK (server-side)
                // For now, we just deactivate by removing Firestore doc
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to delete user: ${e.message}")
        }
    }
}
