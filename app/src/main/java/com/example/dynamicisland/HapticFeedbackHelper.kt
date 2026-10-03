package com.example.dynamicisland

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticFeedbackHelper(context: Context) {
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java).defaultVibrator
    } else context.getSystemService(Vibrator::class.java)

    fun tick() = vibratePrimitive(0.4f, 10)
    fun click() = vibratePrimitive(0.8f, 18)
    fun thud() = if (Build.VERSION.SDK_INT >= 29) vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)) else vibrator.vibrate(24)
    private fun vibratePrimitive(scale: Float, fallbackMs: Long) {
        if (Build.VERSION.SDK_INT >= 30) {
            val primitive = VibrationEffect.Composition.PRIMITIVE_TICK
            vibrator.vibrate(VibrationEffect.startComposition().addPrimitive(primitive, scale).compose())
        } else vibrator.vibrate(VibrationEffect.createOneShot(fallbackMs, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
