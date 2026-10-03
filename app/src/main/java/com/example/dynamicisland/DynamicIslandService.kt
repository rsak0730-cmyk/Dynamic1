package com.example.dynamicisland

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.Rect
import android.os.*
import android.provider.Settings
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import java.util.concurrent.atomic.AtomicBoolean

class DynamicIslandService : Service() {
    companion object { var instance: DynamicIslandService? = null }
    private lateinit var wm: WindowManager
    private lateinit var root: FrameLayout
    private lateinit var island: IslandOverlayView
    private lateinit var manager: IslandStateManager
    private lateinit var media: MediaSessionMonitor
    private lateinit var haptic: HapticFeedbackHelper
    private val started = AtomicBoolean(false)
    private val notifications = mutableMapOf<String,String>()
    private var replyEdit: EditText? = null
    private var receiverRegistered = false

    override fun onCreate() {
        super.onCreate(); instance=this; haptic=HapticFeedbackHelper(this); wm=getSystemService(WINDOW_SERVICE); manager=IslandStateManager{p,s->runOnMain{island.setState(p,s); updateReplyInput(p)}}; media=MediaSessionMonitor(this){e->if(e!=null)manager.publish("media",e) else manager.remove("media")};
        createChannel(); startForeground(44, buildNotification()); buildOverlay(); registerReceiver(); media.start(); started.set(true)
    }
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int { intent?.let{ if(it.action=="SYSTEM_EVENT") SystemStateReceiver.event(this,it)?.let{e->manager.publish("system",e)} }; return START_STICKY }
    override fun onBind(intent:Intent?)=null
    override fun onDestroy(){if(receiverRegistered)try{unregisterReceiver(receiver)}catch(_:Exception){};media.stop();removeOverlay();instance=null;super.onDestroy()}

    private val receiver=object:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){SystemStateReceiver.event(c,i)?.let{manager.publish("system",it)}}}
    private fun registerReceiver(){val f=IntentFilter().apply{addAction(Intent.ACTION_BATTERY_CHANGED);addAction(Intent.ACTION_POWER_CONNECTED);addAction(Intent.ACTION_POWER_DISCONNECTED);addAction(android.media.AudioManager.RINGER_MODE_CHANGED_ACTION);addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED);addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED)};registerReceiver(receiver,f);receiverRegistered=true}

    private fun buildOverlay(){root=FrameLayout(this).apply{setBackgroundColor(Color.TRANSPARENT);clipChildren=true;clipToPadding=true};island=IslandOverlayView(this);root.addView(island,FrameLayout.LayoutParams(dp(390),dp(310)).apply{gravity=Gravity.TOP or Gravity.CENTER_HORIZONTAL;topMargin=0});val lp=WindowManager.LayoutParams(dp(390),dp(330),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,android.graphics.PixelFormat.TRANSLUCENT);val pref=getSharedPreferences("calibration",MODE_PRIVATE);lp.gravity=Gravity.TOP or Gravity.CENTER_HORIZONTAL;lp.x=pref.getInt("x",0);lp.y=pref.getInt("y",0);wm.addView(root,lp)}
    private fun updateReplyInput(e:IslandEvent){if(e.mode!=IslandMode.MESSAGE||e.replyAction==null||e.title.isBlank()){removeReplyInput();return};if(replyEdit==null){replyEdit=EditText(this).apply{hint="Reply…";setSingleLine(true);setTextColor(Color.WHITE);setHintTextColor(0xFF777777.toInt());setBackgroundColor(0xFF202020.toInt());setPadding(dp(14),0,dp(14),0);setOnEditorActionListener{_,_,_->sendReply(e.replyAction);true}};root.addView(replyEdit,FrameLayout.LayoutParams(dp(300),dp(44)).apply{gravity=Gravity.TOP or Gravity.CENTER_HORIZONTAL;topMargin=dp(235)});}
        val p=wmParams();p.flags=p.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv();wm.updateViewLayout(root,p);replyEdit?.requestFocus();(getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(replyEdit,InputMethodManager.SHOW_IMPLICIT)
    }
    private fun removeReplyInput(){replyEdit?.let{(getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(it.windowToken,0);root.removeView(it)};replyEdit=null;if(::root.isInitialized&&root.isAttachedToWindow){val p=wmParams();p.flags=p.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;wm.updateViewLayout(root,p)}}
    private fun sendReply(action:Notification.Action){val text=replyEdit?.text?.toString().orEmpty();val inputs=action.remoteInputs ?: return;if(text.isBlank())return;val intent=Intent();val results=Bundle();inputs.forEach{results.putCharSequence(it.resultKey,text)};android.app.RemoteInput.addResultsToIntent(inputs,intent,results);try{action.actionIntent.send(this,0,intent);removeReplyInput()}catch(_:PendingIntent.CanceledException){}}
    private fun wmParams()=root.layoutParams as WindowManager.LayoutParams
    private fun removeOverlay(){if(::root.isInitialized&&root.isAttachedToWindow)try{wm.removeView(root)}catch(_:Exception){}}
    private fun runOnMain(block:()->Unit){if(Looper.myLooper()==Looper.getMainLooper())block()else Handler(Looper.getMainLooper()).post(block)}
    private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
    private fun createChannel(){if(Build.VERSION.SDK_INT>=26)getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("dynamic_island","Dynamic Island",NotificationManager.IMPORTANCE_LOW))}
    private fun buildNotification()=NotificationCompat.Builder(this,"dynamic_island").setSmallIcon(R.drawable.ic_stat_island).setContentTitle("Dynamic Island active").setContentText("Overlay is running").setOngoing(true).setCategory(NotificationCompat.CATEGORY_SERVICE).build()

    fun publishNotification(key:String,event:IslandEvent){notifications[key]=key;manager.publish("notification:$key",event)}
    fun removeNotification(key:String){manager.remove("notification:$key")}
    fun showTest(event:IslandEvent){manager.publish("test",event)}
    fun showTimer(){var end=System.currentTimeMillis()+60000;val h=Handler(Looper.getMainLooper());val r=object:Runnable{override fun run(){val remain=(end-System.currentTimeMillis()).coerceAtLeast(0);manager.publish("timer",IslandEvent(IslandMode.TIMER,String.format("%02d:%02d",remain/60000,(remain/1000)%60),"Timer",progress=1f-remain/60000f,accent=0xFFFF9F0A.toInt()));if(remain>0)h.postDelayed(this,250)else manager.remove("timer")}};h.post(r)}
}
