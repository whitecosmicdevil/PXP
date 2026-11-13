package com.pxp.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.View
import kotlin.math.sin

class LoadingView(ctx: Context, private val title: String) : View(ctx) {
    var progress = -1f
    var label = "Now loading..."

    private val start = System.nanoTime()
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wave = Path()
    private val face = Typeface.create("sans-serif-light", Typeface.NORMAL)
    private val logoFace = Typeface.create("sans-serif-thin", Typeface.NORMAL)
    private var bg: Shader? = null

    private val top = intArrayOf(
        Color.rgb(8, 30, 95), Color.rgb(90, 60, 5),
        Color.rgb(35, 38, 45), Color.rgb(80, 5, 15)
    )
    private val bottom = intArrayOf(
        Color.rgb(70, 160, 215), Color.rgb(235, 190, 70),
        Color.rgb(150, 155, 165), Color.rgb(225, 70, 70)
    )
    private val idx = ctx.getSharedPreferences("pxp", Context.MODE_PRIVATE)
        .getInt("theme", 0).coerceIn(0, 3)

    private val tips = listOf(
        "Hold the >> button to fast-forward through cutscenes.",
        "Plug in a Bluetooth controller for the full PSP feel.",
        "Press Back to return to the PXP menu.",
        "You can change the PXP theme in Settings."
    )

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        bg = LinearGradient(
            0f, 0f, 0f, h.toFloat(), top[idx], bottom[idx], Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(c: Canvas) {
        val t = (System.nanoTime() - start) / 1e9f
        val w = width.toFloat()
        val h = height.toFloat()

        p.style = Paint.Style.FILL
        p.clearShadowLayer()
        p.letterSpacing = 0f
        p.shader = bg
        p.alpha = 255
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null

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
            p.color = Color.WHITE
            p.alpha = 28
            c.drawPath(wave, p)
        }

        p.textAlign = Paint.Align.CENTER
        p.typeface = logoFace
        p.textSize = h * 0.2f
        p.letterSpacing = 0.4f
        p.setShadowLayer(h * 0.05f, 0f, 0f, Color.WHITE)
        p.color = Color.WHITE
        p.alpha = 255
        c.drawText("PXP", w / 2, h * 0.3f, p)
        p.clearShadowLayer()
        p.letterSpacing = 0f

        p.typeface = face
        p.textSize = h * 0.06f
        val base = title.substringBeforeLast('.')
        val shown = if (base.length > 38) base.substring(0, 38) + "..." else base
        c.drawText(shown, w / 2, h * 0.48f, p)

        val bw = w * 0.5f
        val bx = (w - bw) / 2f
        val by = h * 0.66f
        val bh = h * 0.018f

        p.alpha = 60
        c.drawRoundRect(RectF(bx, by, bx + bw, by + bh), bh, bh, p)
        p.alpha = 235
        if (progress >= 0f) {
            val fw = (bw * progress).coerceIn(bh, bw)
            c.drawRoundRect(RectF(bx, by, bx + fw, by + bh), bh, bh, p)
        } else {
            val seg = bw * 0.25f
            val pos = (sin(t * 2f) + 1f) / 2f
            val sx = bx + pos * (bw - seg)
            c.drawRoundRect(RectF(sx, by, sx + seg, by + bh), bh, bh, p)
        }

        p.textSize = h * 0.04f
        p.alpha = 210
        p.textAlign = Paint.Align.LEFT
        c.drawText(label.substringBefore('\n'), bx, by - h * 0.025f, p)
        if (progress >= 0f) {
            p.textAlign = Paint.Align.RIGHT
            c.drawText("${(progress * 100).toInt()}%", bx + bw, by - h * 0.025f, p)
        }

        val r = h * 0.05f
        val dcx = bx - h * 0.11f
        val dcy = by + bh / 2f
        val angle = (t * 220f) % 360f
        p.style = Paint.Style.FILL
        p.color = Color.WHITE
        p.alpha = 50
        c.drawCircle(dcx, dcy, r, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = r * 0.12f
        p.alpha = 150
        c.drawCircle(dcx, dcy, r, p)
        val oval = RectF(dcx - r * 0.7f, dcy - r * 0.7f, dcx + r * 0.7f, dcy + r * 0.7f)
        p.alpha = 230
        c.drawArc(oval, angle, 70f, false, p)
        c.drawArc(oval, angle + 180f, 70f, false, p)
        p.style = Paint.Style.FILL
        p.color = Color.BLACK
        p.alpha = 150
        c.drawCircle(dcx, dcy, r * 0.18f, p)

        val tipTime = 3.5f
        val tipIdx = (t / tipTime).toInt() % tips.size
        val phase = (t % tipTime) / tipTime
        val fade = minOf(1f, minOf(phase / 0.15f, (1f - phase) / 0.15f))
        p.color = Color.WHITE
        p.alpha = (fade.coerceIn(0f, 1f) * 200).toInt()
        p.textAlign = Paint.Align.CENTER
        p.textSize = h * 0.04f
        c.drawText(tips[tipIdx], w / 2, h * 0.9f, p)

        postInvalidateOnAnimation()
    }
}
