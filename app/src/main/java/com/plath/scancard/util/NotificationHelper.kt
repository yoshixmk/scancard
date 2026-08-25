package com.plath.scancard.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri

class NotificationHelper(private val context: Context) {
    companion object {
        private const val CHANNEL_ID = "extraction_channel"
        private const val CHANNEL_NAME = "Extraction Progress"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showCompletionNotification(deckId: Long) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("scancard://deck/$deckId")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // 明示package付与: カスタムscheme scancard:// は package に依らないが、
            // com.plath 移行後に他アプリが同schemeを横取りするのを防ぐ
            `package` = context.packageName
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            deckId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Extraction Complete")
            .setContentText("Flashcards have been extracted successfully.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            manager.notify(deckId.toInt(), notification)
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHelper", "Missing notification permission", e)
        }
    }

    fun showErrorNotification(deckId: Long, message: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("scancard://deck/$deckId")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            `package` = context.packageName
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            deckId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Extraction Failed")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            manager.notify(deckId.toInt(), notification)
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHelper", "Missing notification permission", e)
        }
    }

    fun createForegroundNotification(deckId: Long): android.app.Notification {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("scancard://deck/$deckId")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            `package` = context.packageName
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            deckId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Extracting flashcards")
            .setContentText("Processing deck $deckId…")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }
}
