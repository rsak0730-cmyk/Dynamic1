package com.example.dynamicisland

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Handler
import android.os.Looper
import androidx.palette.graphics.Palette

class MediaSessionMonitor(private val context: Context, private val listener: (IslandEvent?) -> Unit) {
    private val manager = context.getSystemService(MediaSessionManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val component = ComponentName(context, NotificationMonitorService::class.java)
    private var lastPackage: String? = null
    private val callbacks = mutableMapOf<String, MediaController.Callback>()

    fun start() { refresh(); handler.post(refreshRunnable) }
    fun stop() { handler.removeCallbacks(refreshRunnable); callbacks.clear() }
    private val refreshRunnable = object : Runnable { override fun run() { refresh(); handler.postDelayed(this, 1000) } }

    private fun refresh() {
        val sessions = try { manager.getActiveSessions(component) } catch (_: SecurityException) { emptyList() }
        val playing = sessions.firstOrNull { it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING }
        if (playing == null) { if (lastPackage != null) { lastPackage = null; listener(null) }; return }
        val pkg = playing.packageName
        if (callbacks[pkg] == null) { val cb = object : MediaController.Callback(){ override fun onMetadataChanged(metadata: MediaMetadata?) { emit(playing) }; override fun onPlaybackStateChanged(state: android.media.session.PlaybackState?) { emit(playing) } }; callbacks[pkg]=cb; playing.registerCallback(cb) }
        emit(playing)
    }

    private fun emit(controller: MediaController) {
        val meta = controller.metadata ?: return
        val art = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val title = meta.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        val artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
        val duration = meta.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(1)
        val pos = controller.playbackState?.position ?: 0L
        val accent = dominantColor(art)
        listener(IslandEvent(IslandMode.MEDIA,title,artist,progress=(pos.toFloat()/duration).coerceIn(0f,1f),accent=accent,icon=art,sourcePackage=controller.packageName,action=null))
        lastPackage = controller.packageName
    }
    private fun dominantColor(bitmap: Bitmap?): Int { if (bitmap == null) return 0xFFFFFFFF.toInt(); return Palette.from(bitmap).generate().getVibrantColor(Palette.from(bitmap).generate().getDominantColor(0xFFFFFFFF.toInt())) }
}
