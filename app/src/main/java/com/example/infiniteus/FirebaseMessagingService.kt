package com.example.infiniteus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle data message
        if (remoteMessage.data.isNotEmpty()) {
            val messageType = remoteMessage.data["type"]
            
            when (messageType) {
                "heartbeat" -> handleHeartbeat()
                "eclipse" -> handleEclipse()
                "presence" -> handlePresence(remoteMessage.data)
                else -> handleGenericMessage(remoteMessage.data)
            }
        }

        // Handle notification message
        remoteMessage.notification?.let {
            sendNotification(it.title ?: "The Infinite Us", it.body ?: "Your partner sent you a message")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send token to server or store locally
        // This token should be saved to the user's profile in Firebase
        saveTokenToDatabase(token)
    }

    private fun handleHeartbeat() {
        // Trigger haptic feedback for Ghost Heartbeat
        triggerGhostHeartbeat()
        
        // Optionally show a notification
        sendNotification("Your Partner's Heartbeat", "Feel the connection...")
    }

    private fun handleEclipse() {
        // Trigger a more intense haptic feedback for Eclipse event
        triggerEclipseHaptic()
        
        // Show notification
        sendNotification("Eclipse Moment", "You and your partner are in sync!")
    }

    private fun handlePresence(data: Map<String, String>) {
        val partnerName = data["partnerName"] ?: "Your Partner"
        val status = data["status"] ?: "online"
        
        sendNotification("$partnerName is $status", "Stay connected...")
    }

    private fun handleGenericMessage(data: Map<String, String>) {
        val title = data["title"] ?: "The Infinite Us"
        val body = data["body"] ?: "You have a new message"
        
        sendNotification(title, body)
    }

    private fun triggerGhostHeartbeat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            
            // "Da-Dum" pattern: [delay, vibrate, delay, vibrate]
            val pattern = longArrayOf(0, 40, 60, 100)
            val effect = VibrationEffect.createWaveform(pattern, -1)
            vibrator.vibrate(effect)
        }
    }

    private fun triggerEclipseHaptic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            
            // Intense pulse pattern for Eclipse
            val pattern = longArrayOf(0, 100, 50, 100, 50, 100)
            val effect = VibrationEffect.createWaveform(pattern, -1)
            vibrator.vibrate(effect)
        }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "The Infinite Us",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for The Infinite Us"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun saveTokenToDatabase(token: String) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val database = com.google.firebase.database.FirebaseDatabase.getInstance()
            database.getReference("users").child(currentUser.uid).child("fcmToken").setValue(token)
        }
    }

    companion object {
        private const val CHANNEL_ID = "infinite_us_channel"
        private const val NOTIFICATION_ID = 1
    }
}
