package com.sonia.gatepass.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sonia.gatepass.R
import com.sonia.gatepass.ui.main.MainActivity

class FCMService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Handle notification message
        message.notification?.let {
            showNotification(it.title ?: "Gate Pass", it.body ?: "")
        }

        // Handle data message (for topic-based notifications)
        if (message.data.isNotEmpty()) {
            val title = message.data["title"] ?: "Gate Pass Update"
            val body = message.data["message"] ?: message.data["body"] ?: ""
            if (body.isNotEmpty()) {
                showNotification(title, body)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token refreshed - can be sent to server if needed
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "gate_pass_channel"
        val channelName = "Gate Pass Notifications"

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

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_notifications", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
