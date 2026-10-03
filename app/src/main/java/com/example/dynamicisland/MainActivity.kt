package com.example.dynamicisland

import android.Manifest
import android.app.*
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val prefs by lazy { getSharedPreferences("calibration", MODE_PRIVATE) }
    private lateinit var status: TextView
    private lateinit var container: LinearLayout
    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { refreshStatus() }

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); buildUi(); refreshStatus() }
    override fun onResume(){super.onResume();refreshStatus()}

    private fun buildUi(){
        container=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(24),dp(20),dp(30));setBackgroundColor(Color.BLACK)}
        val scroll=ScrollView(this).apply{addView(container)};setContentView(scroll)
        text("Dynamic Island",28,true);text("Android system overlay • calibration + test deck",14,false,0xFF9E9E9E)
        status=TextView(this).apply{setTextColor(Color.WHITE);setPadding(0,dp(16),0,dp(10));setTextSize(14f)};container.addView(status)
        button("1 · Allow display over other apps"){if(!Settings.canDrawOverlays(this))startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))else startOverlay()}
        button("2 · Allow notification access"){startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))}
        button("3 · Ignore battery optimizations"){if(Build.VERSION.SDK_INT>=23)startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,Uri.parse("package:$packageName")))}
        button("Start / restart Dynamic Island"){startOverlay()}
        text("Live notch calibration",20,true);slider("X offset",-100,100,prefs.getInt("x",0)){prefs.edit().putInt("x",it).apply();restartOverlayIfRunning()};slider("Y offset",0,120,prefs.getInt("y",0)){prefs.edit().putInt("y",it).apply();restartOverlayIfRunning()};slider("Width scale",80,140,prefs.getInt("w",100)){prefs.edit().putInt("w",it).apply();restartOverlayIfRunning()};slider("Height scale",80,150,prefs.getInt("h",100)){prefs.edit().putInt("h",it).apply();restartOverlayIfRunning()};slider("Curvature",10,60,prefs.getInt("radius",28)){prefs.edit().putInt("radius",it).apply();restartOverlayIfRunning()}
        text("Test deck",20,true);button("Music"){svc()?.showTest(IslandEvent(IslandMode.MEDIA,"Midnight Drive","Demo Artist",progress=.42f,accent=0xFF8E7CFF.toInt()))};button("Incoming Call"){svc()?.showTest(IslandEvent(IslandMode.CALL,"Alex","Incoming call",accent=0xFF30D158.toInt()))};button("60-second Timer"){svc()?.showTimer()};button("Battery 100% / Charging"){svc()?.showTest(IslandEvent(IslandMode.BATTERY,"100%","Charging",1f,0xFF30D158.toInt()))};button("WhatsApp-style Message"){svc()?.showTest(IslandEvent(IslandMode.MESSAGE,"Jamie","WhatsApp","Hey! This is a Dynamic Island test message.",accent=0xFF30D158.toInt()))};button("Split: Music + Timer"){svc()?.showTest(IslandEvent(IslandMode.MEDIA,"Midnight Drive","Demo Artist",.55f,0xFF8E7CFF.toInt(),secondary=IslandEvent(IslandMode.TIMER,"00:42")))};button("Clear test state"){svc()?.showTest(IslandEvent(IslandMode.IDLE))}
        text("Cutout",20,true);val cutout=window.decorView.rootWindowInsets?.displayCutout; text(if(cutout==null)"No display cutout reported by this device." else "Display cutout detected: ${cutout.boundingRects.size} region(s). Overlay uses centered calibration by default.",13,false,0xFFAAAAAA.toInt())
    }
    private fun startOverlay(){if(!Settings.canDrawOverlays(this)){Toast.makeText(this,"Overlay permission is required",Toast.LENGTH_SHORT).show();return};val i=Intent(this,DynamicIslandService::class.java);if(Build.VERSION.SDK_INT>=26)ContextCompat.startForegroundService(this,i)else startService(i);Toast.makeText(this,"Dynamic Island started",Toast.LENGTH_SHORT).show()}
    private fun restartOverlayIfRunning(){if(DynamicIslandService.instance!=null){DynamicIslandService.instance?.stopSelf();Handler(Looper.getMainLooper()).postDelayed({startOverlay()},300)}}
    private fun svc()=DynamicIslandService.instance ?: run{startOverlay();DynamicIslandService.instance}
    private fun refreshStatus(){val overlay=Settings.canDrawOverlays(this);val listener=android.provider.Settings.Secure.getString(contentResolver,"enabled_notification_listeners")?.contains(packageName)==true;status.text="Overlay: ${if(overlay)"ON" else "OFF"}   Notifications: ${if(listener)"ON" else "OFF"}"}
    private fun text(s:String,size:Float,bold:Boolean,color:Int=Color.WHITE){container.addView(TextView(this).apply{text=s;textSize=size;setTextColor(color);typeface=if(bold)android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT;setPadding(0,dp(12),0,dp(4))})}
    private fun button(label:String,on:()->Unit){container.addView(Button(this).apply{text=label;setOnClickListener{on()};isAllCaps=false})}
    private fun slider(label:String,min:Int,max:Int,value:Int,on:(Int)->Unit){text("$label: $value",14,false,0xFFBBBBBB.toInt());val bar=SeekBar(this).apply{this.max=max-min;progress=(value-min).coerceIn(0,this.max);setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:SeekBar?,p:Int,f:Boolean){val v=p+min; (parent as? LinearLayout)?.let{}; on(v)};override fun onStartTrackingTouch(s:SeekBar?){ };override fun onStopTrackingTouch(s:SeekBar?){}})};container.addView(bar)}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
