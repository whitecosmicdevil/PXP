package com.pxp.player

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.RectF

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
    val alphaInt = (alpha * 255).toInt()
    
    // टेक्स्ट सेटिंग्स (परसेंटेज और टाइम)
    paint.textSize = h * 0.035f
    paint.textAlign = Paint.Align.RIGHT
    paint.color = Color.WHITE
    paint.alpha = alphaInt
    
    val text = "${battery.level}%  $timeStr"
    c.drawText(text, w * 0.95f, h * 0.06f, paint)

    // बैटरी आइकन की पोजीशन और साइज सेट करें
    paint.textSize = h * 0.035f
    val textWidth = paint.measureText(text)
    
    val batW = h * 0.07f
    val batH = h * 0.032f
    val batX = w * 0.95f - textWidth - batW - h * 0.02f
    val batY = h * 0.033f

    // बैटरी की बाहरी बॉडी (Outline)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 2f
    paint.color = Color.WHITE
    paint.alpha = alphaInt
    val batteryRect = RectF(batX, batY, batX + batW, batY + batH)
    c.drawRoundRect(batteryRect, 4f, 4f, paint)

    // बैटरी का छोटा टिप (आगे का हिस्सा)
    val tipW = h * 0.008f
    val tipH = h * 0.015f
    val tipRect = RectF(batX + batW, batY + (batH - tipH) / 2, batX + batW + tipW, batY + (batH + tipH) / 2)
    paint.style = Paint.Style.FILL
    c.drawRect(tipRect, paint)

    // बैटरी के अंदर का लेवल (जितना चार्ज है)
    val fillMargin = 3f
    val innerW = batW - (fillMargin * 2)
    val innerH = batH - (fillMargin * 2)
    val fillW = innerW * (battery.level.coerceIn(0, 100) / 100f)
    if (fillW > 0f) {
        val fillRect = RectF(batX + fillMargin, batY + fillMargin, batX + fillMargin + fillW, batY + fillMargin + innerH)
        c.drawRect(fillRect, paint)
    }
}
