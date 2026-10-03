package com.example.dynamicisland

import android.os.Handler
import android.os.Looper
import java.util.concurrent.ConcurrentHashMap

class IslandStateManager(private val onState: (IslandEvent, IslandEvent?) -> Unit) {
    private val events = ConcurrentHashMap<String, IslandEvent>()
    private val handler = Handler(Looper.getMainLooper())

    fun publish(key: String, event: IslandEvent) {
        events[key] = event
        emit()
        if (event.expiresAt > 0) handler.postDelayed({ if (events[key] == event) { events.remove(key); emit() } }, (event.expiresAt - System.currentTimeMillis()).coerceAtLeast(1))
    }
    fun remove(key: String) { events.remove(key); emit() }
    fun clear() { events.clear(); emit() }
    fun current(): Pair<IslandEvent, IslandEvent?> {
        val sorted = events.values.sortedByDescending { priority(it.mode).value }
        return (sorted.firstOrNull() ?: IslandEvent(IslandMode.IDLE)) to sorted.getOrNull(1)
    }
    private fun emit() { val (p, s) = current(); onState(p, s) }
    private fun priority(mode: IslandMode) = when(mode) { IslandMode.CALL -> IslandPriority.CALL; IslandMode.MESSAGE, IslandMode.BATTERY, IslandMode.RINGER, IslandMode.FLASHLIGHT, IslandMode.BLUETOOTH, IslandMode.SCREEN_RECORDING, IslandMode.HOTSPOT -> IslandPriority.ALERT; IslandMode.TIMER -> IslandPriority.TIMER; IslandMode.MEDIA -> IslandPriority.MEDIA; IslandMode.IDLE -> IslandPriority.IDLE }
}
