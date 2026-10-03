package com.example.dynamicisland

import android.graphics.Bitmap

enum class IslandMode { IDLE, MEDIA, CALL, TIMER, BATTERY, RINGER, FLASHLIGHT, BLUETOOTH, SCREEN_RECORDING, HOTSPOT, MESSAGE }
enum class IslandPriority(val value: Int) { IDLE(0), MEDIA(10), TIMER(20), ALERT(30), CALL(40) }

data class IslandEvent(
    val mode: IslandMode,
    val title: String = "",
    val subtitle: String = "",
    val body: String = "",
    val progress: Float = 0f,
    val accent: Int = 0xFFFFFFFF.toInt(),
    val icon: Bitmap? = null,
    val sourcePackage: String? = null,
    val action: android.app.PendingIntent? = null,
    val replyAction: android.app.Notification.Action? = null,
    val expiresAt: Long = 0L,
    val secondary: IslandEvent? = null
)
