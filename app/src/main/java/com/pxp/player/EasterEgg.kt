package com.pxp.player

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.sin

const val EGG_TEXT = "Built on a phone. Powered by stubbornness."

fun drawEgg(c: Canvas, p: Paint, w: Float, h: Float, t: Float) {
    p.style = Paint.Style.FILL
    p.color = Color.BLACK
    p.alpha = 235
    c.drawRect(0f, 0f, w, h, p)
    val path = Path()
    for (k in 0..6) {
        path.reset()
        path.moveTo(0f, h)
        var x = 0f
        while (x <= w) {
            val y = h * (0.4f + 0.08f * k) + sin(x / w * 5f + t * 1.5f + k * 0.6f) * h * 0.06f
            path.lineTo(x, y)
            x += 16f
        }
        path.lineTo(w, h)
        path.close()
        val hue = (k * 50f + t * 60f) % 360f
        p.color = Color.HSVToColor(140, floatArrayOf(hue, 0.85f, 1f))
        c.drawPath(path, p)
    }
    p.color = Color.WHITE
    p.alpha = 255
    p.textAlign = Paint.Align.CENTER
    p.textSize = h * 0.09f
    p.letterSpacing = 0.15f
    c.drawText("DEVELOPER MODE", w / 2, h * 0.2f, p)
    p.letterSpacing = 0f
    p.textSize = h * 0.05f
    c.drawText(EGG_TEXT, w / 2, h * 0.3f, p)
    p.alpha = 170
    p.textSize = h * 0.04f
    c.drawText("Swipe right to exit", w / 2, h * 0.95f, p)
}
