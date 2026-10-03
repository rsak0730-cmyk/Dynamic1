package com.example.dynamicisland

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sign

class IslandOverlayView(context: Context) : View(context) {
    private val haptic = HapticFeedbackHelper(context)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create("sans", Typeface.NORMAL) }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; typeface = Typeface.create("sans", Typeface.NORMAL) }
    private val path = Path()
    private val clipPath = Path()
    private var primary = IslandEvent(IslandMode.IDLE)
    private var secondary: IslandEvent? = null
    private var expanded = false
    private var expansion = 0f
    private var targetExpansion = 0f
    private var downY = 0f
    private var downX = 0f
    private var lastY = 0f
    private var velocityTracker: VelocityTracker? = null
    private var sourceAction: android.app.PendingIntent? = null
    private var quickReplyVisible = false
    private val expansionSpring = SpringAnimation(this, DynamicAnimation.ROTATION).apply {
        spring = SpringForce(0f).setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY).setStiffness(SpringForce.STIFFNESS_LOW)
    }

    init { setLayerType(View.LAYER_TYPE_SOFTWARE, null); isClickable = true }

    fun setState(event: IslandEvent, second: IslandEvent?) {
        primary = event; secondary = second; sourceAction = event.action
        invalidate()
    }
    fun setQuickReplyFocus(active: Boolean) { quickReplyVisible = active; expanded = active; targetExpansion = if (active) 1f else 0f; animateExpansion(); invalidate() }
    fun toggleExpanded() { expanded = !expanded; targetExpansion = if (expanded) 1f else 0f; haptic.click(); animateExpansion() }
    private fun animateExpansion() {
        val spring = SpringAnimation(this, EXPANSION_PROPERTY)
        spring.spring = SpringForce(targetExpansion).setDampingRatio(0.5f).setStiffness(SpringForce.STIFFNESS_LOW)
        spring.addUpdateListener { _, value, _ -> expansion = value; invalidate() }
        spring.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val baseH = 36f * density
        val compactW = 112f * density
        val expandedW = minOf(width - 16f * density, 365f * density)
        val expandedH = minOf(height - 20f * density, 280f * density)
        val w = lerp(compactW, expandedW, expansion)
        val h = lerp(baseH, expandedH, expansion)
        val cx = width / 2f
        val top = 4f * density
        val cy = top + h / 2f
        val left = cx - w / 2f
        val right = cx + w / 2f
        val radius = lerp(h / 2f, 28f * density, expansion)
        buildSuperellipse(clipPath, RectF(left, top, right, top + h), radius, 4.0)
        canvas.save()
        canvas.clipPath(clipPath)
        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
        canvas.drawPath(clipPath, paint)
        drawContent(canvas, RectF(left, top, right, top + h), expansion, density)
        canvas.restore()
        if (secondary != null && expansion < 0.5f) drawSecondary(canvas, cx + w / 2f + 8f * density, cy, 26f * density, secondary!!, density)
        if (primary.mode == IslandMode.IDLE && expansion < 0.1f) {
            paint.color = Color.rgb(10,10,10); canvas.drawCircle(cx - w/2f + 22f*density, cy, 4f*density, paint)
            canvas.drawCircle(cx + w/2f - 22f*density, cy, 4f*density, paint)
        }
    }

    private fun drawContent(c: Canvas, r: RectF, e: Float, d: Float) {
        val iconAlpha = ((1f - e / 0.2f).coerceIn(0f, 1f))
        val expandedAlpha = ((e - 0.7f) / 0.3f).coerceIn(0f, 1f)
        sourceAction = primary.action
        when (primary.mode) {
            IslandMode.IDLE -> { if (e < 0.5f) drawText(c, "", r.centerX(), r.centerY(), 1f, 12f*d) }
            IslandMode.MEDIA -> {
                if (iconAlpha > 0) { primary.icon?.let { b -> c.drawBitmap(b, null, RectF(r.left+8*d,r.top+5*d,r.left+26*d,r.top+23*d), paintWithAlpha(iconAlpha)) }; drawEqualizer(c, r.right-42*d, r.centerY(), primary.accent, iconAlpha, d) }
                if (expandedAlpha > 0) { primary.icon?.let { b -> c.drawBitmap(b, null, RectF(r.left+14*d,r.top+14*d,r.left+112*d,r.top+112*d), paintWithAlpha(expandedAlpha)) }; drawText(c, primary.title.ifBlank { "Now Playing" }, r.left+126*d, r.top+38*d, expandedAlpha, 19*d, true); drawText(c, primary.subtitle, r.left+126*d, r.top+62*d, expandedAlpha, 13*d); drawProgress(c,r.left+126*d,r.top+84*d,r.right-18*d,primary.progress,primary.accent,expandedAlpha,d); drawMediaControls(c,r.centerX(),r.bottom-52*d,expandedAlpha,d) }
            }
            IslandMode.CALL -> {
                drawCircleAvatar(c, primary.icon, r.left+24*d,r.centerY(),9*d,iconAlpha,d); drawText(c, if(primary.title.isBlank()) "Call" else primary.title, r.left+40*d,r.centerY()+5*d,iconAlpha,13*d,true); if(expandedAlpha>0){ drawText(c, primary.title,r.left+20*d,r.top+32*d,expandedAlpha,22*d,true); drawText(c,primary.subtitle,r.left+20*d,r.top+58*d,expandedAlpha,15*d); drawButton(c,r.left+20*d,r.bottom-56*d,r.left+105*d,r.bottom-18*d,"Decline",Color.rgb(255,69,58),expandedAlpha,d); drawButton(c,r.right-105*d,r.bottom-56*d,r.right-20*d,r.bottom-18*d,"Accept",Color.rgb(48,209,88),expandedAlpha,d) }
            }
            IslandMode.TIMER -> { drawText(c,"◷",r.left+14*d,r.centerY()+6*d,iconAlpha,18*d); drawText(c,primary.title,r.left+38*d,r.centerY()+5*d,iconAlpha,14*d,true); if(expandedAlpha>0){ drawRing(c,r.centerX(),r.top+95*d,58*d,primary.progress,Color.rgb(255,159,10),expandedAlpha,d); drawText(c,primary.title,r.centerX(),r.top+102*d,expandedAlpha,22*d,true,Paint.Align.CENTER); drawButton(c,r.left+24*d,r.bottom-52*d,r.centerX()-8*d,r.bottom-16*d,"Pause",Color.rgb(255,159,10),expandedAlpha,d); drawButton(c,r.centerX()+8*d,r.bottom-52*d,r.right-24*d,r.bottom-16*d,"Cancel",Color.DKGRAY,expandedAlpha,d) } }
            IslandMode.BATTERY -> { drawBattery(c,r.left+12*d,r.centerY(),primary.progress,primary.accent,iconAlpha,d); drawText(c,primary.title,r.left+38*d,r.centerY()+5*d,iconAlpha,13*d,true); if(expandedAlpha>0){drawText(c,primary.subtitle,r.left+20*d,r.top+45*d,expandedAlpha,17*d,true);drawRing(c,r.centerX(),r.centerY()+12*d,62*d,primary.progress,Color.rgb(48,209,88),expandedAlpha,d)}}
            IslandMode.RINGER, IslandMode.FLASHLIGHT, IslandMode.BLUETOOTH, IslandMode.SCREEN_RECORDING, IslandMode.HOTSPOT, IslandMode.MESSAGE -> { drawText(c,iconFor(primary.mode),r.left+14*d,r.centerY()+6*d,iconAlpha,16*d); drawText(c,primary.title,r.left+40*d,r.centerY()+5*d,iconAlpha,13*d,true); if(expandedAlpha>0){drawText(c,primary.subtitle,r.left+20*d,r.top+44*d,expandedAlpha,14*d);drawText(c,primary.body,r.left+20*d,r.top+72*d,expandedAlpha,13*d); if(primary.mode==IslandMode.MESSAGE) drawButton(c,r.left+20*d,r.bottom-48*d,r.right-20*d,r.bottom-14*d,"Mark as Read",primary.accent,expandedAlpha,d)}}
        }
    }

    private fun drawSecondary(c: Canvas,x:Float,y:Float,radius:Float,e:IslandEvent,d:Float){ paint.color=Color.BLACK; c.drawCircle(x,y,radius,paint); drawText(c,if(e.mode==IslandMode.TIMER) "◷" else "•",x,y+5*d,1f,15*d,true,Paint.Align.CENTER) }
    private fun drawEqualizer(c:Canvas,x:Float,y:Float,color:Int,a:Float,d:Float){ paint.color=color;paint.alpha=(255*a).toInt(); for(i in 0..3){val bh=(6+5*((System.nanoTime()/100_000_000L+i)%3))*d;c.drawRoundRect(x+i*5*d,y-bh/2,x+3*d+i*5*d,y+bh/2,2*d,2*d,paint)};paint.alpha=255 }
    private fun drawProgress(c:Canvas,l:Float,y:Float,r:Float,p:Float,color:Int,a:Float,d:Float){paint.color=Color.DKGRAY;paint.alpha=(255*a).toInt();c.drawRoundRect(l,y,r,y+3*d,2*d,2*d,paint);paint.color=color;c.drawRoundRect(l,y,l+(r-l)*p.coerceIn(0f,1f),y+3*d,2*d,2*d,paint);paint.alpha=255}
    private fun drawMediaControls(c:Canvas,x:Float,y:Float,a:Float,d:Float){drawText(c,"‹",x-56*d,y+7*d,a,30*d,true,Paint.Align.CENTER);drawText(c,"▶",x,y+5*d,a,22*d,true,Paint.Align.CENTER);drawText(c,"›",x+56*d,y+7*d,a,30*d,true,Paint.Align.CENTER)}
    private fun drawCircleAvatar(c:Canvas,b:Bitmap?,x:Float,y:Float,r:Float,a:Float,d:Float){b?.let{c.drawBitmap(it,null,RectF(x-r,y-r,x+r,y+r),paintWithAlpha(a))}?:run{paint.color=Color.DKGRAY;paint.alpha=(255*a).toInt();c.drawCircle(x,y,r,paint);paint.alpha=255}}
    private fun drawBattery(c:Canvas,x:Float,y:Float,p:Float,color:Int,a:Float,d:Float){paint.color=color;paint.alpha=(255*a).toInt();c.drawRoundRect(x,y-6*d,x+22*d,y+6*d,3*d,3*d,paint);paint.alpha=255}
    private fun drawRing(c:Canvas,x:Float,y:Float,r:Float,p:Float,color:Int,a:Float,d:Float){paint.style=Paint.Style.STROKE;paint.strokeWidth=7*d;paint.strokeCap=Paint.Cap.ROUND;paint.color=Color.DKGRAY;paint.alpha=(150*a).toInt();c.drawCircle(x,y,r,paint);paint.color=color;paint.alpha=(255*a).toInt();c.drawArc(x-r,y-r,x+r,y+r,-90f,360f*p.coerceIn(0f,1f),false,paint);paint.alpha=255;paint.style=Paint.Style.FILL}
    private fun drawButton(c:Canvas,l:Float,t:Float,r:Float,b:Float,label:String,color:Int,a:Float,d:Float){paint.color=color;paint.alpha=(255*a).toInt();c.drawRoundRect(l,t,r,b,18*d,18*d,paint);paint.alpha=255;drawText(c,label,(l+r)/2,(t+b)/2+5*d,a,13*d,true,Paint.Align.CENTER)}
    private fun drawText(c:Canvas,s:String,x:Float,y:Float,a:Float,size:Float,bold:Boolean=false,align:Paint.Align=Paint.Align.LEFT){textPaint.textSize=size;textPaint.alpha=(255*a).toInt();textPaint.typeface=Typeface.create("sans",if(bold)Typeface.BOLD else Typeface.NORMAL);textPaint.textAlign=align;c.drawText(s,x,y,textPaint);textPaint.alpha=255}
    private fun paintWithAlpha(a:Float)=Paint(paint).apply{alpha=(255*a).toInt()}
    private fun iconFor(m:IslandMode)=when(m){IslandMode.RINGER->"◉";IslandMode.FLASHLIGHT->"✦";IslandMode.BLUETOOTH->"ᛒ";IslandMode.SCREEN_RECORDING->"●";IslandMode.HOTSPOT->"⌁";IslandMode.MESSAGE->"●";else->"•"}
    private fun lerp(a:Float,b:Float,t:Float)=a+(b-a)*t
    private fun buildSuperellipse(out:Path,r:RectF,radius:Float,n:Double){out.reset();val points=64;val cx=r.centerX();val cy=r.centerY();val rx=r.width()/2;val ry=r.height()/2;for(i in 0..points){val th=2*Math.PI*i/points;val ct=cos(th);val st=sin(th);val x=cx+rx*sign(ct)*abs(ct).pow(2.0/n);val y=cy+ry*sign(st)*abs(st).pow(2.0/n);if(i==0)out.moveTo(x.toFloat(),y.toFloat())else out.lineTo(x.toFloat(),y.toFloat())};out.close()}

    override fun onTouchEvent(ev:MotionEvent):Boolean{
        when(ev.actionMasked){MotionEvent.ACTION_DOWN->{downX=ev.x;downY=ev.y;lastY=ev.y;velocityTracker=VelocityTracker.obtain().also{it.addMovement(ev)};haptic.tick();return true}
            MotionEvent.ACTION_MOVE->{velocityTracker?.addMovement(ev);val dy=ev.y-downY;if(abs(dy)>10){expansion=(targetExpansion+(dy/220f)*0.4f).coerceIn(0f,1.2f);invalidate()};lastY=ev.y;return true}
            MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{velocityTracker?.addMovement(ev);velocityTracker?.computeCurrentVelocity(1000);val vy=velocityTracker?.yVelocity?:0f;val dy=ev.y-downY;if(abs(dy)>45||abs(vy)>700){targetExpansion=if(dy>0||vy>0)1f else 0f;expanded=targetExpansion>0.5f;haptic.click()}else if(System.currentTimeMillis()-downTime<450){if(expanded)toggleExpanded() else try { sourceAction?.send() } catch (_: android.app.PendingIntent.CanceledException) { }} else {targetExpansion=if(expansion>0.5f)1f else 0f};animateExpansion();velocityTracker?.recycle();velocityTracker=null;return true}}
        return true
    }
    private var downTime:Long=0L
    override fun dispatchTouchEvent(event: MotionEvent): Boolean { if(event.actionMasked==MotionEvent.ACTION_DOWN) downTime=System.currentTimeMillis(); return super.dispatchTouchEvent(event) }

    companion object {
        private val EXPANSION_PROPERTY = object : androidx.dynamicanimation.animation.FloatPropertyCompat<IslandOverlayView>("expansion") {
            override fun getValue(view: IslandOverlayView): Float = view.expansion
            override fun setValue(view: IslandOverlayView, value: Float) { view.expansion = value }
        }
    }


}
