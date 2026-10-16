package com.example.jarvis.core.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.jarvis.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class VoiceAssistantService : Service() {

    @Inject
    lateinit var voiceManager: VoiceManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())

        // Start listening for wake word "Jarvis"
        // In a real implementation, this would use a small local model like Porcupine or Snowboy
        voiceManager.startListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "CHANNEL_ID")
            .setContentTitle("Jarvis is listening")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "CHANNEL_ID",
            "Voice Assistant Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}