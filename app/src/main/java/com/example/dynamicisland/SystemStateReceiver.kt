package com.example.dynamicisland

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.bluetooth.BluetoothDevice

class SystemStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val service = Intent(context, DynamicIslandService::class.java).apply { action = "SYSTEM_EVENT"; putExtras(intent) }
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(service) else context.startService(service)
    }
    private fun getDeviceName(intent: Intent): String {
        val device = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) else @Suppress("DEPRECATION") intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        return device?.name ?: "Bluetooth device"
    }
    companion object {
        fun event(context: Context, intent: Intent): IslandEvent? = when(intent.action) {
            Intent.ACTION_BATTERY_CHANGED -> { val level=intent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1); val scale=intent.getIntExtra(BatteryManager.EXTRA_SCALE,100); val p=(level.toFloat()/scale).coerceIn(0f,1f); val charging=intent.getIntExtra(BatteryManager.EXTRA_STATUS,0)==BatteryManager.BATTERY_STATUS_CHARGING; IslandEvent(IslandMode.BATTERY,"${(p*100).toInt()}%",if(charging)"Charging" else "Battery",progress=p,accent=0xFF30D158.toInt(),expiresAt=if(charging)0 else System.currentTimeMillis()+3000) }
            AudioManager.RINGER_MODE_CHANGED_ACTION -> { val rm=intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE,AudioManager.RINGER_MODE_NORMAL); IslandEvent(IslandMode.RINGER,if(rm==AudioManager.RINGER_MODE_SILENT)"Silent" else "Ringer",accent=if(rm==AudioManager.RINGER_MODE_SILENT)0xFFFF453A.toInt() else 0xFFB0B0B0.toInt(),expiresAt=System.currentTimeMillis()+2500) }
            BluetoothDevice.ACTION_ACL_CONNECTED -> IslandEvent(IslandMode.BLUETOOTH,getDeviceName(intent) ?: "Bluetooth connected",accent=0xFF0A84FF.toInt(),expiresAt=System.currentTimeMillis()+3000)
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> IslandEvent(IslandMode.BLUETOOTH,"Bluetooth disconnected",accent=0xFF0A84FF.toInt(),expiresAt=System.currentTimeMillis()+2500)
            Intent.ACTION_POWER_CONNECTED -> IslandEvent(IslandMode.BATTERY,"Charging","Power connected",accent=0xFF30D158.toInt(),expiresAt=System.currentTimeMillis()+3000)
            Intent.ACTION_POWER_DISCONNECTED -> IslandEvent(IslandMode.BATTERY,"Power disconnected",accent=0xFFFF9F0A.toInt(),expiresAt=System.currentTimeMillis()+2500)
            else -> null
        }
    }
}
