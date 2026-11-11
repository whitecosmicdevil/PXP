package com.pxp.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

class PxpView(ctx: Context) : View(ctx) {
    private val start = System.nanoTime()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wave = Path()
    private val clockFmt = SimpleDateFormat("dd/MM  HH:mm", Locale.getDefault())
    private val uiFace = Typeface.create("sans-serif-light", Typeface.NORMAL)
    private val logoFace = Typeface.create("sans-serif-thin", Typeface.NORMAL)
    private var bg: Shader? = null
    private val menuStart = 6.2f

    private val cats = listOf("Settings", "Photo", "Music", "Video", "Game", "Network")
    private val items = listOf(
        listOf("System Settings", "Theme Settings", "Sound Settings"),
        listOf("Memory Stick", "Photo Viewer"),
        listOf("Memory Stick", "Music Player"),
        listOf("Memory Stick", "Video Player"),
        listOf("Memory Stick", "PXP Games"),
        listOf("Network Update", "Browser")
    )

    private var cat = 4
    private var item = 0
    private var catAnim = 4f
    private var itemAnim = 0f
    private var chimed = false
    private var downX = 0f
    private var downY = 0f
    private var msg: String? = null
    private var msgUntil = 0L

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        bg = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            Color.rgb(8, 30, 95), Color.rgb(70, 160, 215),
            Shader.TileMode.CLAMP
        )
    }

    private fun col(color: Int, a: Float) {
        paint.color = color
        paint.alpha = (a.coerceIn(0f, 1f) * 255).toInt()
    }

    override fun onDraw(c: Canvas) {
        val t = (System.nanoTime() - start) / 1e9f
        val w = width.toFloat()
        val h = height.toFloat()
        c.drawColor(Color.BLACK)
        paint.style = Paint.Style.FILL
        paint.clearShadowLayer()
        paint.letterSpacing = 0f
        drawBackground(c, t, w, h)
        drawBoot(c, t, w, h)
        if (t > menuStart) drawMenu(c, t, w, h)
        drawMessage(c, w, h)
        postInvalidateOnAnimation()
    }

    private fun drawBackground(c: Canvas, t: Float, w: Float, h: Float) {
        val bgA = ((t - 2.5f) / 1.5f).coerceIn(0f, 1f)
        if (bgA <= 0f) return
        paint.shader = bg
        paint.alpha = (bgA * 255).toInt()
        c.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        for (k in 0..3) {
            wave.reset()
            wave.moveTo(0f, h)
            var x = 0f
            while (x <= w) {
                val y = h * (0.58f + 0.06f * k) +
                    sin(x / w * 6f + t * (0.6f + 0.2f * k) + k) * h * 0.05f
                wave.lineTo(x, y)
                x += 14f
            }
            wave.lineTo(w, h)
            wave.close()
            col(Color.WHITE, bgA * 0.11f)
            c.drawPath(wave, paint)
        }
    }

    private fun drawBoot(c: Canvas, t: Float, w: Float, h: Float) {
        val entA = when {
            t < 0.6f -> 0f
            t < 1.2f -> (t - 0.6f) / 0.6f
            t < 2.0f -> 1f
            t < 2.5f -> (2.5f - t) / 0.5f
            else -> 0f
        }
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = uiFace
        if (entA > 0f) {
            paint.textSize = h * 0.06f
            paint.letterSpacing = 0.3f
            col(Color.WHITE, entA)
            c.drawText("PXP ENTERTAINMENT", w / 2, h * 0.53f, paint)
            paint.letterSpacing = 0f
        }
        if (t >= 2.6f && !chimed) {
            chimed = true
            Sfx.boot()
        }
        val logoA = when {
            t < 3.0f -> 0f
            t < 4.0f -> t - 3.0f
            t < 5.6f -> 1f
            else -> ((6.4f - t) / 0.8f).coerceIn(0f, 1f)
        }
        if (logoA > 0f) {
            val p = ((t - 3.0f) / 1.8f).coerceIn(0f, 1f)
            paint.typeface = logoFace
            paint.textSize = h * 0.3f
            paint.letterSpacing = 0.9f - 0.65f * p
            paint.setShadowLayer(h * 0.05f, 0f, 0f, Color.WHITE)
            col(Color.WHITE, logoA)
            c.drawText("PXP", w / 2, h * 0.55f, paint)
            paint.clearShadowLayer()
            paint.letterSpacing = 0f
            paint.typeface = uiFace
            val half = w * 0.3f * p
            paint.strokeWidth = 2f
            col(Color.WHITE, logoA * 0.8f)
            c.drawLine(w / 2 - half, h * 0.63f, w / 2 + half, h * 0.63f, paint)
            paint.textSize = h * 0.04f
            paint.letterSpacing = 0.25f
            col(Color.WHITE, logoA * p * 0.8f)
            c.drawText("PORTABLE EXPERIENCE PLAYER", w / 2, h * 0.72f, paint)
            paint.letterSpacing = 0f
        }
    }

    private fun drawMenu(c: Canvas, t: Float, w: Float, h: Float) {
        val xA = ((t - menuStart) / 1f).coerceIn(0f, 1f)
        catAnim += (cat - catAnim) * 0.2f
        itemAnim += (item - itemAnim) * 0.2f
        paint.typeface = uiFace
        val baseX = w * 0.28f
        val gap = w * 0.15f
        val cy = h * 0.32f

        for (i in cats.indices) {
            val d = i - catAnim
            val x = baseX + d * gap
            val sel = (1f - abs(d)).coerceIn(0f, 1f)
            val size = h * 0.12f * (1f + 0.5f * sel)
            if (sel > 0.05f) {
                paint.shader = RadialGradient(
                    x, cy, size * 1.1f,
                    Color.argb((90 * sel * xA).toInt(), 255, 255, 255),
                    Color.argb(0, 255, 255, 255),
                    Shader.TileMode.CLAMP
                )
                c.drawCircle(x, cy, size * 1.1f, paint)
                paint.shader = null
            }
            col(Color.WHITE, (0.5f + 0.5f * sel) * xA)
            drawXmbIcon(c, paint, i, x, cy, size)
            paint.textAlign = Paint.Align.CENTER
            if (sel > 0.5f) {
                col(Color.WHITE, xA)
                paint.textSize = h * 0.045f
                c.drawText(cats[i], x, cy + size * 0.85f, paint)
            }
        }

        val selX = baseX + (cat - catAnim) * gap
        val list = items[cat]
        for (j in list.indices) {
            val d = j - itemAnim
            val y = cy + h * 0.27f + d * h * 0.14f
            val sel = (1f - abs(d)).coerceIn(0f, 1f)
            val s = h * 0.075f * (1f + 0.25f * sel)
            col(Color.WHITE, (0.35f + 0.65f * sel) * xA)
            c.drawRoundRect(
                RectF(selX - s / 2, y - s / 2, selX + s / 2, y + s / 2),
                s * 0.2f, s * 0.2f, paint
            )
            paint.textAlign = Paint.Align.LEFT
            paint.textSize = h * (0.05f + 0.012f * sel)
            col(Color.WHITE, (0.5f + 0.5f * sel) * xA)
            c.drawText(list[j], selX + s * 0.8f, y + h * 0.018f, paint)
        }

        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = h * 0.05f
        col(Color.WHITE, xA)
        c.drawText(clockFmt.format(Date()), w - w * 0.04f, h * 0.09f, paint)
    }

    private fun drawMessage(c: Canvas, w: Float, h: Float) {
        val m = msg
        if (m == null || System.currentTimeMillis() >= msgUntil) return
        col(Color.BLACK, 0.8f)
        c.drawRoundRect(
            RectF(w * 0.2f, h * 0.38f, w * 0.8f, h * 0.62f),
            h * 0.03f, h * 0.03f, paint
        )
        col(Color.WHITE, 1f)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = h * 0.055f
        c.drawText(m, w / 2, h * 0.53f, paint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val t = (System.nanoTime() - start) / 1e9f
        if (t < menuStart + 0.4f) return true
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x
                downY = e.y
            }
            MotionEvent.ACTION_UP -> {
                val dx = e.x - downX
                val dy = e.y - downY
                val th = width * 0.04f
                if (abs(dx) > th && abs(dx) > abs(dy)) {
                    val n = if (dx < 0) cat + 1 else cat - 1
                    if (n in cats.indices) {
                        cat = n
                        item = 0
                        itemAnim = 0f
                        Sfx.click()
                    }
                } else if (abs(dy) > th) {
                    val n = if (dy < 0) item + 1 else item - 1
                    if (n in items[cat].indices) {
                        item = n
                        Sfx.click()
                    }
                } else {
                    Sfx.ok()
                    msg = if (items[cat][item] == "PXP Games")
                        "No memory card found."
                    else
                        "This feature is not ready yet."
                    msgUntil = System.currentTimeMillis() + 2200
                }
            }
        }
        return true
    }
}
