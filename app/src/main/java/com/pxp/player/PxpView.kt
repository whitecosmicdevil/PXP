package com.pxp.player

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.BatteryManager
import android.view.MotionEvent
import android.view.View
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.sin

class PxpView(ctx: Context) : View(ctx) {
    var onPickFolder: (() -> Unit)? = null

    private val start = System.nanoTime()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wave = Path()
    private val clockFmt = SimpleDateFormat("dd/MM  hh:mm a", Locale.ENGLISH)
    private val uiFace = Typeface.create("sans-serif-light", Typeface.NORMAL)
    private val logoFace = Typeface.create("sans-serif-thin", Typeface.NORMAL)
    private var bg: Shader? = null
    private val menuStart = 6.2f

    private val zoneIds = listOf(
        "", "Asia/Kolkata", "UTC", "Europe/London", "Europe/Paris", "Asia/Dubai",
        "Asia/Singapore", "Asia/Tokyo", "Australia/Sydney",
        "America/New_York", "America/Chicago", "America/Los_Angeles"
    )
    private val zoneNames = listOf(
        "Device", "India (IST)", "UTC", "London", "Paris", "Dubai",
        "Singapore", "Tokyo", "Sydney", "New York", "Chicago", "Los Angeles"
    )
    private var zoneIdx = ctx.getSharedPreferences("pxp", Context.MODE_PRIVATE).getInt("zone", 0)

    private val cats = listOf("Settings", "Photo", "Music", "Video", "Game", "Network")
    private val items: List<MutableList<String>> = listOf(
        mutableListOf("Time Zone", "Theme Settings", "Sound Settings"),
        mutableListOf("Memory Stick", "Photo Viewer"),
        mutableListOf("Memory Stick", "Music Player"),
        mutableListOf("Memory Stick", "Video Player"),
        mutableListOf("Memory Stick", "PXP Games"),
        mutableListOf("Network Update", "Browser")
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

    private var games: List<String> = emptyList()
    private var cardSet = false
    private var showList = false
    private var listSel = 0

    private var batPct = 100
    private var batCharging = false
    private var batCheck = 0L

    init {
        applyZone()
    }

    private fun applyZone() {
        clockFmt.timeZone = if (zoneIdx == 0) TimeZone.getDefault()
        else TimeZone.getTimeZone(zoneIds[zoneIdx])
        items[0][0] = "Time Zone: " + zoneNames[zoneIdx]
    }

    private fun updateBattery() {
        val now = System.currentTimeMillis()
        if (now - batCheck < 3000) return
        batCheck = now
        val i = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        if (level >= 0 && scale > 0) batPct = level * 100 / scale
        batCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }

    fun setGames(list: List<String>) {
        games = list.sorted()
        cardSet = true
        listSel = 0
        val t = (System.nanoTime() - start) / 1e9f
        if (list.isNotEmpty() && t > menuStart) {
            showMsg("${list.size} games found")
        }
    }

    private fun showMsg(m: String) {
        msg = m
        msgUntil = System.currentTimeMillis() + 2200
    }

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
        if (showList) drawList(c, w, h)
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

    private fun drawStatus(c: Canvas, w: Float, h: Float, a: Float) {
        updateBattery()
        paint.typeface = uiFace
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = h * 0.045f
        col(Color.WHITE, a)
        val clock = clockFmt.format(Date())
        val right = w * 0.96f
        val y = h * 0.09f
        c.drawText(clock, right, y, paint)
        val tw = paint.measureText(clock)
        val pctText = batPct.toString() + "%"
        val pctRight = right - tw - h * 0.05f
        c.drawText(pctText, pctRight, y, paint)
        val pw = paint.measureText(pctText)
        val tip = h * 0.012f
        val bodyRight = pctRight - pw - h * 0.025f - tip
        val bw = h * 0.1f
        val bh = h * 0.05f
        val cyy = y - h * 0.016f
        val color = when {
            batCharging -> Color.rgb(120, 255, 140)
            batPct <= 20 -> Color.rgb(255, 110, 110)
            else -> Color.WHITE
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = h * 0.006f
        col(color, a)
        c.drawRoundRect(
            RectF(bodyRight - bw, cyy - bh / 2, bodyRight, cyy + bh / 2),
            bh * 0.2f, bh * 0.2f, paint
        )
        paint.style = Paint.Style.FILL
        c.drawRect(bodyRight, cyy - bh * 0.2f, bodyRight + tip, cyy + bh * 0.2f, paint)
        val pad = h * 0.01f
        val fillW = (bw - pad * 2) * batPct / 100f
        c.drawRect(
            bodyRight - bw + pad, cyy - bh / 2 + pad,
            bodyRight - bw + pad + fillW, cyy + bh / 2 - pad, paint
        )
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

        drawStatus(c, w, h, xA)
    }

    private fun drawList(c: Canvas, w: Float, h: Float) {
        col(Color.BLACK, 0.6f)
        c.drawRect(0f, 0f, w, h, paint)
        paint.typeface = uiFace
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = h * 0.06f
        col(Color.WHITE, 1f)
        c.drawText("PXP Games", w * 0.08f, h * 0.15f, paint)
        val first = (listSel - 2).coerceAtLeast(0)
        for (r in 0 until 6) {
            val idx = first + r
            if (idx >= games.size) break
            val y = h * 0.28f + r * h * 0.11f
            if (idx == listSel) {
                col(Color.WHITE, 0.25f)
                c.drawRoundRect(
                    RectF(w * 0.06f, y - h * 0.07f, w * 0.94f, y + h * 0.035f),
                    h * 0.02f, h * 0.02f, paint
                )
            }
            col(Color.WHITE, if (idx == listSel) 1f else 0.6f)
            paint.textSize = h * 0.05f
            val full = games[idx]
            val name = if (full.length > 40) full.substring(0, 40) + "..." else full
            c.drawText(name, w * 0.1f, y, paint)
        }
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = h * 0.04f
        col(Color.WHITE, 0.8f)
        c.drawText("${listSel + 1} / ${games.size}", w * 0.94f, h * 0.15f, paint)
    }

    private fun drawMessage(c: Canvas, w: Float, h: Float) {
        val m = msg
        if (m == null || System.currentTimeMillis() >= msgUntil) return
        col(Color.BLACK, 0.8f)
        c.drawRoundRect(
            RectF(w * 0.2f, h * 0.36f, w * 0.8f, h * 0.64f),
            h * 0.03f, h * 0.03f, paint
        )
        col(Color.WHITE, 1f)
        paint.typeface = uiFace
        paint.textAlign = Paint.Align.CENTER
        val lines = m.split("\n")
        paint.textSize = h * 0.055f
        c.drawText(lines[0], w / 2, h * 0.49f, paint)
        if (lines.size > 1) {
            paint.textSize = h * 0.04f
            c.drawText(lines[1], w / 2, h * 0.57f, paint)
        }
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
                if (showList) {
                    if (dx > th && abs(dx) > abs(dy)) {
                        showList = false
                        Sfx.click()
                    } else if (abs(dy) > th) {
                        val n = if (dy < 0) listSel + 1 else listSel - 1
                        if (n in games.indices) {
                            listSel = n
                            Sfx.click()
                        }
                    } else {
                        Sfx.ok()
                        showMsg("Game launching comes in the next step.")
                    }
                    return true
                }
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
                    val label = items[cat][item]
                    when {
                        label == "Memory Stick" -> onPickFolder?.invoke()
                        label == "PXP Games" -> {
                            if (!cardSet) {
                                showMsg("Memory Stick not found.\n(PXP-8001)")
                            } else if (games.isEmpty()) {
                                showMsg("No games found.\n(PXP-8002)")
                            } else {
                                listSel = 0
                                showList = true
                            }
                        }
                        label.startsWith("Time Zone") -> {
                            zoneIdx = (zoneIdx + 1) % zoneNames.size
                            context.getSharedPreferences("pxp", Context.MODE_PRIVATE)
                                .edit().putInt("zone", zoneIdx).apply()
                            applyZone()
                            showMsg("Time zone:\n" + zoneNames[zoneIdx])
                        }
                        else -> showMsg("This feature is not ready yet.")
                    }
                }
            }
        }
        return true
    }
}
