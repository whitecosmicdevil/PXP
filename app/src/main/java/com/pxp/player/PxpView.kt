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

    private val start = System.nanoTime()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wave = Path()
    private val prefs = ctx.getSharedPreferences("pxp", Context.MODE_PRIVATE)
    private val battery = Battery(ctx)
    private val clockFmt = SimpleDateFormat("dd/MM  hh:mm a", Locale.getDefault())
    private val uiFace = Typeface.create("sans-serif-light", Typeface.NORMAL)
    private val logoFace = Typeface.create("sans-serif-thin", Typeface.NORMAL)
    private var bg: Shader? = null
    private val menuStart = 6.2f

    // Easter Egg States
    private var systemSettingsClicks = 0
    private var themeSettingsClicks = 0
    private var pxpGamesClicks = 0
    private var isGoldenTheme = false

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
        mutableListOf("Time Zone", "System Settings", "Theme Settings", "Sound Settings"),
        mutableListOf("Memory Stick", "Photo Viewer"),
        mutableListOf("Memory Stick", "Music Player"),
        mutableListOf("Memory Stick", "Video Player"),
        mutableListOf("Memory Stick", "PXP Games"),
        mutableListOf("Network Update", "Browser")
    )

    init {
        paint.isFilterBitmap = true
        zi = prefs.getInt("zone", 0).coerceIn(0, zones.size - 1)
        isGoldenTheme = prefs.getBoolean("golden", false)
        applyZone()
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

    private fun applyZone() {
        val id = zones[zi].second
        clockFmt.timeZone = if (id.isEmpty()) TimeZone.getDefault() else TimeZone.getTimeZone(id)
        items[0][0] = "Time Zone: " + zones[zi].first
    }

    private fun cycleZone() {
        zi = (zi + 1) % zones.size
        prefs.
