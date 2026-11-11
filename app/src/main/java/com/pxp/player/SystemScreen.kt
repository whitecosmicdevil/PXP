package com.pxp.player

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.util.Locale

@Suppress("UNUSED_PARAMETER")
fun buildSystemInfo(
    ctx: Context, bat: Battery, games: Int, cardSet: Boolean
): List<Pair<String, String>> {
    val ver = try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"
    } catch (e: Exception) {
        "?"
    }
    val free = try {
        val st = StatFs(Environment.getDataDirectory().path)
        String.format(Locale.US, "%.1f GB", st.availableBytes / 1073741824.0)
    } catch (e: Exception) {
        "?"
    }
    val batIntent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = batIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val status = batIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val pct = if (level >= 0 && scale > 0) level * 100 / scale else -1
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    val batText = (if (pct >= 0) "$pct%" else "?") + if (charging) " (charging)" else ""
    return listOf(
        "PXP version" to ver,
        "Android" to Build.VERSION.RELEASE,
        "Device" to (Build.MANUFACTURER + " " + Build.MODEL),
        "Battery" to batText,
        "Free storage" to free,
        "Memory Stick" to (if (cardSet) "Connected" else "Not set"),
        "Games found" to games.toString()
    )
}

fun drawSystemScreen(c: Canvas, p: Paint, w: Float, h: Float, info: List<Pair<String, String>>) {
    p.style = Paint.Style.FILL
    p.color = Color.BLACK
    p.alpha = 200
    c.drawRect(0f, 0f, w, h, p)
    p.color = Color.WHITE
    p.alpha = 255
    p.textAlign = Paint.Align.LEFT
    p.textSize = h * 0.07f
    c.drawText("System Settings", w * 0.08f, h * 0.14f, p)
    p.strokeWidth = 2f
    c.drawLine(w * 0.08f, h * 0.18f, w * 0.92f, h * 0.18f, p)
    p.textSize = h * 0.05f
    for (i in info.indices) {
        val y = h * 0.30f + i * h * 0.095f
        p.textAlign = Paint.Align.LEFT
        p.alpha = 190
        c.drawText(info[i].first, w * 0.08f, y, p)
        p.textAlign = Paint.Align.RIGHT
        p.alpha = 255
        c.drawText(info[i].second, w * 0.92f, y, p)
    }
    p.textAlign = Paint.Align.CENTER
    p.alpha = 160
    p.textSize = h * 0.04f
    c.drawText("Swipe right to go back", w / 2, h * 0.96f, p)
}
