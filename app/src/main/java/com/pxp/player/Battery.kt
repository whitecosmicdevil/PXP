package com.pxp.player

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color

class Battery(private val context: Context) {
    var level: Int = 100
    var isCharging: Boolean = false

    fun update() {
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val status = context.registerReceiver(null, filter)
            if (status != null) {
                val levelVal = status.getIntExtra("level", -1)
                val scale = status.getIntExtra("scale", -1)
                if (levelVal >= 0 && scale > 0) {
                    level = (levelVal * 100 / scale)
                }
                val plugged = status.getIntExtra("plugged", -1)
                isCharging = plugged > 0
            }
        } catch (e: Exception) {
            level = 100
        }
    }
}

fun drawStatusBar(c: Canvas, paint: Paint, w: Float, h: Float, alpha: Float, battery: Battery, timeStr: String) {
    paint.textSize = h * 0.035f
    paint.textAlign = Paint.Align.RIGHT
    paint.color = Color.WHITE
    paint.alpha = (alpha * 255).toInt()
    
    val text = "${battery.level}%  $timeStr"
    c.drawText(text, w * 0.95f, h * 0.06f, paint)
}
