package com.pxp.player

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        setContentView(PxpView(this))
    }
}

object Sfx {
    private const val SR = 22050

    fun play(notes: List<Pair<Double, Int>>, vol: Double = 0.25) {
        Thread {
            try {
                var total = 0
                for (n in notes) total += SR * n.second / 1000
                if (total <= 0) return@Thread
                val buf = ShortArray(total)
                var pos = 0
                for ((f, ms) in notes) {
                    val len = SR * ms / 1000
                    for (i in 0 until len) {
                        val t = i.toDouble() / SR
                        val env = min(1.0, min(i / 200.0, (len - i) / (len * 0.6)))
                        buf[pos + i] = (sin(2 * PI * f * t) * env * vol * 32767).toInt().toShort()
                    }
                    pos += len
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SR)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buf.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(buf, 0, buf.size)
                track.play()
                Thread.sleep(total * 1000L / SR + 200)
                track.release()
            } catch (e: Exception) {
            }
        }.start()
    }
}

class PxpView(ctx: Context) : View(ctx) {
    private val start = System.nanoTime()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wave = Path()
    private val clockFmt = SimpleDateFormat("dd/MM  HH:mm", Locale.getDefault())
    private val uiFace = Typeface.create("sans-serif-light", Typeface.NORMAL)
    private val logoFace = Typeface.create("sans-serif", Typeface.BOLD)
    private var bg: Shader? = null

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

        // Background glow and waves
        val bgA = ((t - 1f) / 1.5f).coerceIn(0f, 1f)
        if (bgA > 0f) {
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

        // Boot logo
        val logoA = when {
            t < 1.5f -> 0f
            t < 2.5f -> t - 1.5f
            t < 4f -> 1f
            else -> (5f - t).coerceIn(0f, 1f)
        }
        if (logoA > 0f) {
            if (!chimed) {
                chimed = true
                Sfx.play(listOf(523.0 to 220, 659.0 to 220, 784.0 to 220, 1047.0 to 800))
            }
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = logoFace
            paint.textSize = h * 0.24f
            paint.setShadowLayer(h * 0.06f, 0f, 0f, Color.WHITE)
            col(Color.WHITE, logoA)
            c.drawText("PXP", w / 2, h * 0.55f, paint)
            paint.clearShadowLayer()
            paint.typeface = uiFace
            paint.textSize = h * 0.045f
            col(Color.WHITE, logoA * 0.7f)
            c.drawText("PORTABLE EXPERIENCE PLAYER", w / 2, h * 0.66f, paint)
        }

        // XMB menu
        val xA = ((t - 4.8f) / 1f).coerceIn(0f, 1f)
        if (xA > 0f) {
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
                col(Color.WHITE, (0.45f + 0.55f * sel) * xA)
                c.drawRoundRect(
                    RectF(x - size / 2, cy - size / 2, x + size / 2, cy + size / 2),
                    size * 0.22f, size * 0.22f, paint
                )
                drawIcon(c, i, x, cy, size, (0.45f + 0.55f * sel) * xA)
                paint.textAlign = Paint.Align.CENTER
                if (sel > 0.5f) {
                    col(Color.WHITE, xA)
                    paint.textSize = h * 0.045f
                    c.drawText(cats[i], x, cy + size * 0.8f, paint)
                }
            }

            val selX = baseX + (cat - catAnim) * gap
            val list = items[cat]
            for (j in list.indices) {
                val d = j - itemAnim
                val y = cy + h * 0.27f + d * h * 0.14f
                val s
