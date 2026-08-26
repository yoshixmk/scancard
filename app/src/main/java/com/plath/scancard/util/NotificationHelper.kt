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

        // アプリ管理通知（progress/completion/error）のベースid。
        // WorkManagerのFGS通知は deckId をそのまま使うため、衝突を避けてオフセットする。
        // 同一IDでのraw notifyはWMがsetProgress毎に元FGS通知を再投稿して上書きされるため機能しない
        // （実測 2026-08-26）。別IDなら常にシェードに表示される。
        private const val APP_NOTIF_ID_OFFSET = 100_000
        fun appNotificationId(deckId: Long): Int = APP_NOTIF_ID_OFFSET + deckId.toInt()
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
            manager.notify(appNotificationId(deckId), notification)
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
            manager.notify(appNotificationId(deckId), notification)
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHelper", "Missing notification permission", e)
        }
    }

    fun showProgressNotification(deckId: Long, current: Int, total: Int) {
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
        val safeTotal = total.coerceAtLeast(1)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Extracting flashcards")
            .setContentText("Page $current of $safeTotal")
            .setProgress(safeTotal, current.coerceIn(0, safeTotal), false)
            .setOngoing(true)
            // 進捗更新のたびに通知音/ヘッドアップを鳴らさない
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            manager.notify(appNotificationId(deckId), notification)
            android.util.Log.d("NotificationHelper", "Progress notif posted deck=$deckId $current/$safeTotal")
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
