package com.pxp.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import android.view.KeyEvent
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
    var onOpen: ((Entry) -> Unit)? = null
    var onOpenWeb: ((String) -> Unit)? = null

    private val start = System.nanoTime()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wave = Path()
    private val prefs = ctx.getSharedPreferences("pxp", Context.MODE_PRIVATE)
    private val battery = Battery(ctx)
    private val updater = UpdateChecker(ctx)
    private val game = MiniGame()
    private val clockFmt = SimpleDateFormat("dd/MM  hh:mm a", Locale.getDefault())
    private val uiFace = Typeface.create("sans-serif-light", Typeface.NORMAL)
    private val logoFace = Typeface.create("sans-serif-thin", Typeface.NORMAL)
    private var bg: Shader? = null
    private val menuStart = 6.2f

    private val themeNames = listOf("Blue", "Gold", "Grey", "Red")
    private val themeTop = intArrayOf(
        Color.rgb(8, 30, 95), Color.rgb(90, 60, 5),
        Color.rgb(35, 38, 45), Color.rgb(80, 5, 15)
    )
    private val themeBottom = intArrayOf(
        Color.rgb(70, 160, 215), Color.rgb(235, 190, 70),
        Color.rgb(150, 155, 165), Color.rgb(225, 70, 70)
    )
    private var themeIdx = 0
    private val soundNames = listOf("Classic", "Digital", "Droplet", "Relax")

    private val zones = listOf(
        "Device" to "",
        "India" to "Asia/Kolkata",
        "UTC" to "UTC",
        "London" to "Europe/London",
        "Dubai" to "Asia/Dubai",
        "Singapore" to "Asia/Singapore",
        "Tokyo" to "Asia/Tokyo",
        "Sydney" to "Australia/Sydney",
        "New York" to "America/New_York",
        "Los Angeles" to "America/Los_Angeles"
    )
    private var zi = 0

    private val cats = listOf("Settings", "Photo", "Music", "Video", "Game", "Network")
    private val items = listOf<MutableList<String>>(
        mutableListOf("Time Zone", "System Settings", "Theme", "Sound"),
        mutableListOf("Memory Stick", "Photo Viewer"),
        mutableListOf("Memory Stick", "Music Player"),
        mutableListOf("Memory Stick", "Video Player"),
        mutableListOf("Memory Stick", "PXP Games"),
        mutableListOf("Network Update", "Browser")
    )

    private var devUnlocked = false

    init {
        paint.isFilterBitmap = true
        zi = prefs.getInt("zone", 0).coerceIn(0, zones.size - 1)
        themeIdx = prefs.getInt("theme", 0).coerceIn(0, themeNames.size - 1)
        Sfx.style = prefs.getInt("sound", 0).coerceIn(0, soundNames.size - 1)
        devUnlocked = prefs.getBoolean("dev", false)
        applyZone()
        applyLabels()
        applyDev()
    }

    private var cat = 4
    private var item = 0
    private var catAnim = 4f
    private var itemAnim = 0f
    private var chimed = false
    private var downX = 0f
    private var downY = 0f
    private var msg: String? = null
    private var msgUntil = 0L

    private var library: List<Entry> = emptyList()
    private var cardSet = false
    private var mode = 0
    private var listEntries: List<Entry> = emptyList()
    private var listTitle = ""
    private var listSel = 0
    private var photos: List<Entry> = emptyList()
    private var photoIdx = 0
    private var photoBmp: Bitmap? = null
    private var photoFail = false
    private var loadToken = 0
    private var sysInfo: List<Pair<String, String>> = emptyList()
    private var eggBuf = ""
    private var eggStart = 0L
    private var eggTaps = 0
    private var eggTapNs = 0L

    private fun applyZone() {
        val id = zones[zi].second
        clockFmt.timeZone = if (id.isEmpty()) TimeZone.getDefault() else TimeZone.getTimeZone(id)
        items[0][0] = "Time Zone: " + zones[zi].first
    }

    private fun applyLabels() {
        items[0][2] = "Theme: " + themeNames[themeIdx]
        items[0][3] = "Sound: " + soundNames[Sfx.style]
    }

    private fun applyDev() {
        if (devUnlocked && !items[0].contains("Developer Mode")) {
            items[0].add("Developer Mode")
        }
    }

    private fun buildBg() {
        val h = height.toFloat()
        if (h <= 0f) return
        bg = LinearGradient(
            0f, 0f, 0f, h, themeTop[themeIdx], themeBottom[themeIdx],
            Shader.TileMode.CLAMP
        )
    }

    private fun cycleZone() {
        zi = (zi + 1) % zones.size
        prefs.edit().putInt("zone", zi).apply()
        applyZone()
    }

    private fun cycleTheme() {
        themeIdx = (themeIdx + 1) % themeNames.size
        prefs.edit().putInt("theme", themeIdx).apply()
        buildBg()
        applyLabels()
    }

    private fun cycleSound() {
        Sfx.style = (Sfx.style + 1) % soundNames.size
        prefs.edit().putInt("sound", Sfx.style).apply()
        applyLabels()
        Sfx.click()
    }

    private fun openCredits() {
        mode = 5
        eggStart = System.nanoTime()
        eggTaps = 0
        Sfx.egg()
    }

    private fun trackEgg(ch: Char): Boolean {
        if (mode != 0) return false
        eggBuf = (eggBuf + ch).takeLast(8)
        if (eggBuf != "UUDDLRLR") return false
        eggBuf = ""
        devUnlocked = true
        prefs.edit().putBoolean("dev", true).apply()
        applyDev()
        openCredits()
        return true
    }

    fun setLibrary(list: List<Entry>) {
        library = list
        cardSet = true
        val t = (System.nanoTime() - start) / 1e9f
        if (t > menuStart) {
            if (list.isEmpty()) {
                showMsg("Nothing found in that folder.\n(PXP-8002)")
            } else {
                val g = list.count { it.type == T_GAME }
                val p = list.count { it.type == T_PHOTO }
                val v = list.count { it.type == T_VIDEO }
                val m = list.count { it.type == T_MUSIC }
                showMsg("$g games, $p photos\n$v videos, $m songs")
            }
        }
    }

    fun showMsg(m: String) {
        msg = m
        msgUntil = System.currentTimeMillis() + 2600
    }

    private fun of(type: Int): List<Entry> =
        library.filter { it.type == type }.sortedBy { it.name.lowercase() }

    private fun ready() = (System.nanoTime() - start) / 1e9f >= menuStart + 0.4f

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        buildBg()
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
        if (mode == 1) drawList(c, w, h)
        if (mode == 2 && photos.isNotEmpty()) drawPhoto(c, w, h)
        if (mode == 3) {
            paint.typeface = uiFace
            drawUpdateScreen(c, paint, w, h, updater)
        }
        if (mode == 4) {
            paint.typeface = uiFace
            drawSystemScreen(c, paint, w, h, sysInfo)
        }
        if (mode == 5) {
            paint.typeface = uiFace
            val et = (System.nanoTime() - eggStart) / 1e9f
            val pulse = (1f - (System.nanoTime() - eggTapNs) / 250_000_000f).coerceIn(0f, 1f)
            drawEgg(c, paint, w, h, et, pulse)
        }
        if (mode == 6) {
            paint.typeface = uiFace
            game.draw(c, paint, w, h)
        }
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
