package com.example.jarvis.core.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.jarvis.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class JarvisMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("JarvisMessaging", "From: ${remoteMessage.from}")

        // Handle data payload (e.g., for triggering actions)
        if (remoteMessage.data.isNotEmpty()) {
            val action = remoteMessage.data["action"]
            val command = remoteMessage.data["command"]

            if (action == "assistant_command" && command != null) {
                showNotification(
                    remoteMessage.notification?.title ?: "Jarvis Action",
                    remoteMessage.notification?.body ?: command,
                    command
                )
            }
        }

        // Handle notification payload
        remoteMessage.notification?.let {
            showNotification(it.title ?: "Jarvis", it.body ?: "", null)
        }
    }

    private fun showNotification(title: String, body: String, command: String?) {
        val channelId = "jarvis_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Jarvis Notifications",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (command != null) {
                putExtra("command", command)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("JarvisMessaging", "Refreshed token: $token")
    }
}
