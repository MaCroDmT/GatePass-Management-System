package com.sonia.gatepass.data.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.sonia.gatepass.GatePassApplication
import com.sonia.gatepass.R
import com.sonia.gatepass.data.model.Notification
import com.sonia.gatepass.ui.main.MainActivity
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

class NotificationRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val notificationCollection = firestore.collection(Constants.COLLECTION_NOTIFICATIONS)

    private val dateTimeFormat = SimpleDateFormat(Constants.DATE_TIME_FORMAT, Locale.getDefault())

    private fun List<Notification>.sortByCreatedAtDesc(): List<Notification> {
        return sortedByDescending { notif ->
            try {
                dateTimeFormat.parse(notif.createdAt)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }

    suspend fun createNotification(notification: Notification): Resource<String> {
        return try {
            val notificationId = UUID.randomUUID().toString()
            val newNotification = notification.copy(
                notificationId = notificationId,
                createdAt = DateUtil.getCurrentDateTime()
            )

            notificationCollection.document(notificationId).set(newNotification).await()

            // Also show a local system notification
            showLocalSystemNotification(notification.title, notification.message)

            Resource.Success(notificationId)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create notification")
        }
    }

    fun observeNotificationsByUserId(userId: String): Flow<Resource<List<Notification>>> = callbackFlow {
        val listener = notificationCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Log the error but don't crash — return empty list on permission errors
                    android.util.Log.e("NotificationRepo", "Notification listen error: ${error.message}")
                    if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        trySend(Resource.Success(emptyList()))
                    } else {
                        trySend(Resource.Error(error.message ?: "Failed to listen"))
                    }
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents?.mapNotNull {
                    it.toObject(Notification::class.java)
                }?.sortByCreatedAtDesc() ?: emptyList()
                trySend(Resource.Success(notifications))
            }

        awaitClose { listener.remove() }
    }

    suspend fun markAsRead(notificationId: String): Resource<Unit> {
        return try {
            notificationCollection.document(notificationId)
                .update("status", Constants.NOTIFICATION_READ)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to mark notification as read")
        }
    }

    suspend fun markAllAsRead(userId: String): Resource<Unit> {
        return try {
            val batch = firestore.batch()
            val snapshot = notificationCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", Constants.NOTIFICATION_UNREAD)
                .get()
                .await()

            for (doc in snapshot.documents) {
                batch.update(doc.reference, "status", Constants.NOTIFICATION_READ)
            }

            batch.commit().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to mark all as read")
        }
    }

    suspend fun getUnreadCount(userId: String): Resource<Int> {
        return try {
            val snapshot = notificationCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", Constants.NOTIFICATION_UNREAD)
                .get()
                .await()

            Resource.Success(snapshot.size())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get unread count")
        }
    }

    suspend fun sendNotificationToRole(
        role: String,
        title: String,
        message: String,
        gpid: String = "",
        type: String = ""
    ): Resource<Unit> {
        return try {
            val usersSnapshot = FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_USERS)
                .whereEqualTo("role", role)
                .get()
                .await()

            val batch = firestore.batch()

            for (userDoc in usersSnapshot.documents) {
                // Use the user's actual Firebase Auth UID from the document field,
                // NOT the Firestore document ID (which may be auto-generated)
                val userId = userDoc.getString("userId") ?: userDoc.id
                val notification = Notification(
                    notificationId = UUID.randomUUID().toString(),
                    userId = userId,
                    title = title,
                    message = message,
                    gpid = gpid,
                    type = type,
                    status = Constants.NOTIFICATION_UNREAD,
                    createdAt = DateUtil.getCurrentDateTime()
                )

                val docRef = notificationCollection.document()
                batch.set(docRef, notification)
            }

            batch.commit().await()

            // Show local system notification
            showLocalSystemNotification(title, message)

            // Subscribe to role-based FCM topic
            subscribeToRoleTopic(role)

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send notification")
        }
    }

    /**
     * Subscribes the current device to FCM topics based on user role.
     * Call this after login to enable push notifications.
     */
    fun subscribeToRoleTopics(role: String) {
        val fcm = FirebaseMessaging.getInstance()
        // All users get general notifications
        fcm.subscribeToTopic("all_users")

        when (role) {
            Constants.ROLE_SUPER_ADMIN -> {
                fcm.subscribeToTopic("admin_notifications")
            }
            Constants.ROLE_ADMIN -> {
                fcm.subscribeToTopic("admin_notifications")
            }
            Constants.ROLE_USER -> {
                fcm.subscribeToTopic("user_notifications")
            }
        }
    }

    /**
     * Unsubscribes from all FCM topics on logout.
     */
    fun unsubscribeFromAllTopics() {
        val fcm = FirebaseMessaging.getInstance()
        fcm.unsubscribeFromTopic("all_users")
        fcm.unsubscribeFromTopic("admin_notifications")
        fcm.unsubscribeFromTopic("user_notifications")
    }

    private fun subscribeToRoleTopic(role: String) {
        val fcm = FirebaseMessaging.getInstance()
        when (role) {
            Constants.ROLE_SUPER_ADMIN, Constants.ROLE_ADMIN -> {
                fcm.subscribeToTopic("admin_notifications")
            }
            Constants.ROLE_USER -> {
                fcm.subscribeToTopic("user_notifications")
            }
            else -> {
                fcm.subscribeToTopic("all_users")
            }
        }
    }

    /**
     * Shows a local system notification when a notification is created.
     * This works even without FCM push server setup.
     */
    private fun showLocalSystemNotification(title: String, message: String) {
        val context = GatePassApplication.instance
        val channelId = "gate_pass_channel"
        val channelName = "Gate Pass Notifications"

        // Create notification channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for gate pass updates"
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, com.sonia.gatepass.ui.notifications.NotificationsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
