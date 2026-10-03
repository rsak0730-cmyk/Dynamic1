package com.example.dynamicisland

import android.app.Notification
import android.app.PendingIntent
import android.graphics.Bitmap
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import androidx.core.app.NotificationCompat

class NotificationMonitorService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) { publish(sbn) }
    override fun onNotificationRemoved(sbn: StatusBarNotification) { DynamicIslandService.instance?.removeNotification(sbn.key) }

    private fun publish(sbn: StatusBarNotification) {
        val n = sbn.notification ?: return
        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return
        val category = n.category
        val isCall = category == Notification.CATEGORY_CALL || title.contains("call", true)
        val large = if (Build.VERSION.SDK_INT >= 23) extras.getParcelable(Notification.EXTRA_LARGE_ICON, Bitmap::class.java) else @Suppress("DEPRECATION") extras.getParcelable(Notification.EXTRA_LARGE_ICON)
        val reply = n.actions?.firstOrNull { a -> a.remoteInputs?.any { it.allowFreeFormInput } == true }
        val event = IslandEvent(
            mode = if (isCall) IslandMode.CALL else IslandMode.MESSAGE,
            title = title.ifBlank { sbn.packageName.substringAfterLast('.') },
            subtitle = if (isCall) "Incoming call" else sbn.packageName.substringAfterLast('.'),
            body = TextUtils.ellipsize(text, android.text.TextPaint().apply{textSize=14f}, 230f, TextUtils.TruncateAt.END).toString(),
            accent = if (isCall) 0xFF30D158.toInt() else 0xFF0A84FF.toInt(),
            icon = large,
            sourcePackage = sbn.packageName,
            action = n.contentIntent,
            replyAction = reply,
            expiresAt = if (isCall) 0L else System.currentTimeMillis()+8000
        )
        DynamicIslandService.instance?.publishNotification(sbn.key,event)
    }
}
