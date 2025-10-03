package com.novaprompt.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.novaprompt.app.R
import com.novaprompt.app.activity.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import kotlin.random.Random
import java.net.URL
import kotlin.concurrent.thread
import kotlin.math.min

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "🔥 Refreshed FCM token: $token")
        TokenManager.sendTokenToServer(this, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "📨 Message received from: ${remoteMessage.from}")

        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FCM", "📊 Message data: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        remoteMessage.notification?.let { notification ->
            Log.d("FCM", "🔔 Notification: ${notification.body}")
            showNotification(
                notification.title ?: "NovaPrompt",
                notification.body ?: "",
                remoteMessage.data
            )
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val title = data["title"] ?: "NovaPrompt"
        val message = data["message"] ?: data["body"] ?: "New notification"
        val imageUrl = data["image"] ?: data["image_url"]

        showNotification(title, message, data, imageUrl)
    }

    private fun showNotification(
        title: String,
        message: String,
        data: Map<String, String>,
        imageUrl: String? = null
    ) {
        val channelId = "novaprompt_channel"
        val notificationId = Random.nextInt(1000, 9999)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager, channelId)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notification_data", data.toString())

            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_close) // Use your own notification icon
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(message)
            .setBigContentTitle(title)

        notificationBuilder.setStyle(bigTextStyle)

        if (!imageUrl.isNullOrEmpty()) {
            loadAndShowImageNotification(notificationBuilder, imageUrl, notificationId, notificationManager, title, message)
        } else {
            notificationManager.notify(notificationId, notificationBuilder.build())
            Log.d("FCM", "✅ Notification displayed: $title")
        }
    }

    private fun loadAndShowImageNotification(
        builder: NotificationCompat.Builder,
        imageUrl: String,
        notificationId: Int,
        notificationManager: NotificationManager,
        title: String,
        message: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("FCM", "🖼️ Loading image from: $imageUrl")

                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.doInput = true

                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) NovaPrompt-App")

                connection.instanceFollowRedirects = true

                connection.connect()

                if (connection.responseCode in 200..299) {
                    val inputStream = connection.inputStream
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    connection.disconnect()

                    if (bitmap != null) {
                        val scaledBitmap = scaleBitmapForNotification(bitmap)

                        withContext(Dispatchers.Main) {
                            val bigPictureStyle = NotificationCompat.BigPictureStyle()
                                .bigPicture(scaledBitmap)
                                .setBigContentTitle(title)
                                .setSummaryText(message)

                            builder.setStyle(bigPictureStyle)
                            notificationManager.notify(notificationId, builder.build())
                            Log.d("FCM", "✅ Image notification displayed successfully")
                            scaledBitmap?.recycle()
                        }
                        bitmap.recycle()
                    } else {
                        throw IOException("Failed to decode bitmap - null result")
                    }
                } else {
                    throw IOException("HTTP error: ${connection.responseCode} - ${connection.responseMessage}")
                }
            } catch (e: Exception) {
                Log.e("FCM", "❌ Failed to load notification image: ${e.message}")
                showFallbackNotification(notificationManager, builder, notificationId, title, message)
            }
        }
    }

    private fun scaleBitmapForNotification(bitmap: Bitmap): Bitmap? {
        return try {
            val maxWidth = 1024
            val maxHeight = 512

            if (bitmap.width <= maxWidth && bitmap.height <= maxHeight) {
                return bitmap
            }

            val scale = min(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height)
            val scaledWidth = (bitmap.width * scale).toInt()
            val scaledHeight = (bitmap.height * scale).toInt()

            Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        } catch (e: Exception) {
            Log.e("FCM", "❌ Error scaling bitmap: ${e.message}")
            null
        }
    }

    private fun showFallbackNotification(
        notificationManager: NotificationManager,
        builder: NotificationCompat.Builder,
        notificationId: Int,
        title: String,
        message: String
    ) {
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(message)
            .setBigContentTitle(title)

        builder.setStyle(bigTextStyle)
        notificationManager.notify(notificationId, builder.build())
        Log.d("FCM", "✅ Fallback text notification displayed")
    }

    private fun createNotificationChannel(notificationManager: NotificationManager, channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "NovaPrompt Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Receive updates and new prompts from NovaPrompt"
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}