package com.pxp.player

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.sin

const val CREDIT_TEXT = "MADE BY TANMAY"

fun drawEgg(c: Canvas, p: Paint, w: Float, h: Float, t: Float, pulse: Float) {
    p.style = Paint.Style.FILL
    p.clearShadowLayer()
    p.letterSpacing = 0f
    p.color = Color.BLACK
    p.alpha = 235
    c.drawRect(0f, 0f, w, h, p)

    val path = Path()
    for (k in 0..6) {
        path.reset()
        path.moveTo(0f, h)
        var x = 0f
        while (x <= w) {
            val y = h * (0.62f + 0.05f * k) + sin(x / w * 5f + t * 1.5f + k * 0.6f) * h * 0.05f
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
    p.alpha = 170
    p.textAlign = Paint.Align.CENTER
    p.textSize = h * 0.045f
    p.letterSpacing = 0.3f
    c.drawText("DEVELOPER MODE", w / 2, h * 0.16f, p)
    p.letterSpacing = 0f

    val size = h * 0.13f * (1f + 0.12f * pulse)
    p.textSize = size
    p.textAlign = Paint.Align.LEFT
    val total = p.measureText(CREDIT_TEXT)
    var cx = w / 2 - total / 2
    for (i in CREDIT_TEXT.indices) {
        val ch = CREDIT_TEXT[i].toString()
        val appear = ((t - i * 0.07f) / 0.35f).coerceIn(0f, 1f)
        val drop = (1f - appear) * size * 0.8f
        val wob = sin(t * 3f + i * 0.5f) * size * 0.06f
        val hue = (t * 120f + i * 22f) % 360f
        p.color = Color.HSVToColor(255, floatArrayOf(hue, 0.55f, 1f))
        p.alpha = (appear * 255).toInt()
        p.setShadowLayer(size * 0.2f, 0f, 0f, Color.argb(160, 255, 255, 255))
        c.drawText(ch, cx, h * 0.5f - drop + wob, p)
        p.clearShadowLayer()
        cx += p.measureText(ch)
    }

    p.color = Color.WHITE
    p.alpha = 170
    p.textAlign = Paint.Align.CENTER
    p.textSize = h * 0.04f
    c.drawText("Swipe right to exit", w / 2, h * 0.95f, p)
}
