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
    private val soundNames = listOf("Classic", "Digital")

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

        battery.update()
        paint.typeface = uiFace
        drawStatusBar(c, paint, w, h, xA, battery, clockFmt.format(Date()))
    }

    private fun drawList(c: Canvas, w: Float, h: Float) {
        col(Color.BLACK, 0.6f)
        c.drawRect(0f, 0f, w, h, paint)
        paint.typeface = uiFace
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = h * 0.06f
        col(Color.WHITE, 1f)
        c.drawText(listTitle, w * 0.08f, h * 0.15f, paint)
        val first = (listSel - 2).coerceAtLeast(0)
        for (r in 0 until 6) {
            val idx = first + r
            if (idx >= listEntries.size) break
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
            val full = listEntries[idx].name
            val name = if (full.length > 40) full.substring(0, 40) + "..." else full
            c.drawText(name, w * 0.1f, y, paint)
        }
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = h * 0.04f
        col(Color.WHITE, 0.8f)
        c.drawText("${listSel + 1} / ${listEntries.size}", w * 0.94f, h * 0.15f, paint)
    }

    private fun drawPhoto(c: Canvas, w: Float, h: Float) {
        c.drawColor(Color.BLACK)
        val b = photoBmp
        paint.typeface = uiFace
        if (b != null) {
            val sc = minOf(w / b.width, h / b.height)
            val dw = b.width * sc
            val dh = b.height * sc
            col(Color.WHITE, 1f)
            c.drawBitmap(
                b, null,
                RectF((w - dw) / 2, (h - dh) / 2, (w + dw) / 2, (h + dh) / 2),
                paint
            )
        } else {
            col(Color.WHITE, 0.8f)
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = h * 0.05f
            c.drawText(if (photoFail) "Cannot open this photo." else "Loading...", w / 2, h * 0.5f, paint)
        }
        col(Color.BLACK, 0.55f)
        c.drawRect(0f, h * 0.92f, w, h, paint)
        col(Color.WHITE, 1f)
        paint.textSize = h * 0.04f
        paint.textAlign = Paint.Align.LEFT
        val nm = photos[photoIdx].name
        c.drawText(if (nm.length > 36) nm.substring(0, 36) + "..." else nm, w * 0.04f, h * 0.975f, paint)
        paint.textAlign = Paint.Align.RIGHT
        c.drawText("${photoIdx + 1} / ${photos.size}", w * 0.96f, h * 0.975f, paint)
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

    private fun decode(uri: Uri): Bitmap? {
        try {
            val cr = context.contentResolver
            val o1 = BitmapFactory.Options()
            o1.inJustDecodeBounds = true
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o1) }
            var s = 1
            while (o1.outWidth / s > 2400 || o1.outHeight / s > 2400) s *= 2
            val o2 = BitmapFactory.Options()
            o2.inSampleSize = s
            val raw = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o2) }
                ?: return null
            var rot = 0
            try {
                cr.openInputStream(uri)?.use {
                    val ex = ExifInterface(it)
                    rot = when (ex.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                }
            } catch (e: Exception) {
            }
            if (rot == 0) return raw
            val m = Matrix()
            m.postRotate(rot.toFloat())
            return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
        } catch (e: Exception) {
            return null
        }
    }

    private fun loadPhoto() {
        val uri = photos[photoIdx].uri
        val token = ++loadToken
        photoBmp = null
        photoFail = false
        Thread {
            val bmp = decode(uri)
            post {
                if (token == loadToken) {
                    photoBmp = bmp
                    photoFail = bmp == null
                }
            }
        }.start()
    }

    private fun openList(type: Int, title: String) {
        if (!cardSet) {
            showMsg("Memory card not found.\n(PXP-8001)")
            return
        }
        val l = of(type)
        if (l.isEmpty()) {
            showMsg("No files found.\n(PXP-8002)")
            return
        }
        listEntries = l
        listTitle = title
        listSel = 0
        mode = 1
    }

    private fun openPhotos() {
        if (!cardSet) {
            showMsg("Memory card not found.\n(PXP-8001)")
            return
        }
        val l = of(T_PHOTO)
        if (l.isEmpty()) {
            showMsg("No photos found.\n(PXP-8002)")
            return
        }
        photos = l
        photoIdx = 0
        mode = 2
        loadPhoto()
    }

    private fun moveH(d: Int) {
        when (mode) {
            0 -> {
                val n = cat + d
                if (n in cats.indices) {
                    cat = n
                    item = 0
                    itemAnim = 0f
                    Sfx.click()
                }
            }
            1, 3, 4, 5 -> if (d < 0) {
                mode = 0
                Sfx.click()
            }
            2 -> {
                val n = photoIdx + d
                if (n in photos.indices) {
                    photoIdx = n
                    Sfx.click()
                    loadPhoto()
                }
            }
        }
    }

    private fun moveV(d: Int) {
        when (mode) {
            0 -> {
                val n = item + d
                if (n in items[cat].indices) {
                    item = n
                    Sfx.click()
                }
            }
            1 -> {
                val n = listSel + d
                if (n in listEntries.indices) {
                    listSel = n
                    Sfx.click()
                }
            }
            2 -> if (d < 0) {
                mode = 0
                photoBmp = null
                Sfx.click()
            }
        }
    }

    private fun confirm() {
        when (mode) {
            1 -> {
                Sfx.ok()
                val e = listEntries[listSel]
                if (e.type == T_GAME) showMsg("Now loading...")
                onOpen?.invoke(e)
            }
            2, 4 -> {}
            3 -> {
                Sfx.ok()
                if (updater.state == 2) onOpenWeb?.invoke(RELEASES_URL) else updater.check()
            }
            5 -> {
                eggTaps++
                eggTapNs = System.nanoTime()
                Sfx.click()
                if (eggTaps >= 5) {
                    eggTaps = 0
                    game.reset()
                    mode = 6
                    Sfx.egg()
                }
            }
            6 -> {}
            else -> {
                val label = items[cat][item]
                if (!label.startsWith("Sound")) Sfx.ok()
                when {
                    label == "Memory Stick" -> onPickFolder?.invoke()
                    label == "PXP Games" -> openList(T_GAME, "PXP Games")
                    label == "Video Player" -> openList(T_VIDEO, "Videos")
                    label == "Music Player" -> openList(T_MUSIC, "Music")
                    label == "Photo Viewer" -> openPhotos()
                    label == "Network Update" -> {
                        mode = 3
                        updater.check()
                    }
                    label == "Browser" -> onOpenWeb?.invoke("https://duckduckgo.com")
                    label == "System Settings" -> {
                        sysInfo = buildSystemInfo(
                            context, battery, library.count { it.type == T_GAME }, cardSet
                        )
                        mode = 4
                    }
                    label == "Developer Mode" -> openCredits()
                    label.startsWith("Theme") -> cycleTheme()
                    label.startsWith("Sound") -> cycleSound()
                    label.startsWith("Time Zone") -> cycleZone()
                    else -> showMsg("This feature is not ready yet.")
                }
            }
        }
    }

    fun goBack(): Boolean {
        if (mode == 0) return false
        mode = if (mode == 6) 5 else 0
        photoBmp = null
        Sfx.click()
        return true
    }

    fun onPadKey(code: Int): Boolean {
        if (!ready()) return false
        if (mode == 6) {
            when (code) {
                KeyEvent.KEYCODE_DPAD_LEFT -> game.nudge(-0.06f)
                KeyEvent.KEYCODE_DPAD_RIGHT -> game.nudge(0.06f)
                KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> game.action()
                KeyEvent.KEYCODE_BUTTON_B -> return goBack()
                else -> return false
            }
            return true
        }
        return when (code) {
            KeyEvent.KEYCODE_DPAD_LEFT -> { if (!trackEgg('L')) moveH(-1); true }
            KeyEvent.KEYCODE_DPAD_RIGHT -> { if (!trackEgg('R')) moveH(1); true }
            KeyEvent.KEYCODE_DPAD_UP -> { if (!trackEgg('U')) moveV(-1); true }
            KeyEvent.KEYCODE_DPAD_DOWN -> { if (!trackEgg('D')) moveV(1); true }
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> { confirm(); true }
            KeyEvent.KEYCODE_BUTTON_B -> goBack()
            else -> false
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (!ready()) return true
        if (mode == 6) {
            game.onTouch(e, width.toFloat(), height.toFloat())
            return true
        }
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
                    if (!trackEgg(if (dx < 0) 'L' else 'R')) moveH(if (dx < 0) 1 else -1)
                } else if (abs(dy) > th) {
                    if (!trackEgg(if (dy < 0) 'U' else 'D')) moveV(if (dy < 0) 1 else -1)
                } else {
                    confirm()
                }
            }
        }
        return true
    }
}
