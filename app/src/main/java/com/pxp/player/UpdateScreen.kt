package com.pxp.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import java.net.HttpURLConnection
import java.net.URL

const val UPDATE_URL = "https://raw.githubusercontent.com/whitecosmicdevil/PXP/main/version.txt"
const val RELEASES_URL = "https://github.com/whitecosmicdevil/PXP/releases"

class UpdateChecker(private val ctx: Context) {
    var state = 0
    var latest = ""
    val current: String = try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"
    } catch (e: Exception) {
        "?"
    }
    private var token = 0
    private val ui = Handler(Looper.getMainLooper())

    fun check() {
        state = 0
        latest = ""
        val my = ++token
        Thread {
            var text: String? = null
            try {
                val conn = URL(UPDATE_URL).openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                text = conn.inputStream.bufferedReader().use { it.readText() }.trim()
                conn.disconnect()
            } catch (e: Exception) {
            }
            val res = text
            ui.post {
                if (my == token) {
                    if (res == null || res.isEmpty() || res.length > 20) {
                        state = 3
                    } else {
                        latest = res
                        state = if (isNewer(res, current)) 2 else 1
                    }
                }
            }
        }.start()
    }

    private fun isNewer(a: String, b: String): Boolean {
        val x = a.split(".").map { it.trim().toIntOrNull() ?: 0 }
        val y = b.split(".").map { it.trim().toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val p = x.getOrElse(i) { 0 }
            val q = y.getOrElse(i) { 0 }
            if (p != q) return p > q
        }
        return false
    }
}

fun drawUpdateScreen(c: Canvas, p: Paint, w: Float, h: Float, u: UpdateChecker) {
    p.style = Paint.Style.FILL
    p.color = Color.BLACK
    p.alpha = 200
    c.drawRect(0f, 0f, w, h, p)
    p.color = Color.WHITE
    p.alpha = 255
    p.textAlign = Paint.Align.LEFT
    p.textSize = h * 0.07f
    c.drawText("Network Update", w * 0.08f, h * 0.16f, p)
    p.strokeWidth = 2f
    c.drawLine(w * 0.08f, h * 0.2f, w * 0.92f, h * 0.2f, p)

    p.textSize = h * 0.055f
    c.drawText("Current version", w * 0.08f, h * 0.36f, p)
    c.drawText("Latest version", w * 0.08f, h * 0.48f, p)
    p.textAlign = Paint.Align.RIGHT
    c.drawText(u.current, w * 0.92f, h * 0.36f, p)
    val latestText = if (u.state == 0) "..." else if (u.latest.isEmpty()) "-" else u.latest
    c.drawText(latestText, w * 0.92f, h * 0.48f, p)

    val status = when (u.state) {
        0 -> "Checking for updates..."
        1 -> "Your PXP is up to date."
        2 -> "A new version is available."
        else -> "Could not connect.\n(PXP-9101)"
    }
    p.color = when (u.state) {
        1 -> Color.rgb(140, 255, 160)
        2 -> Color.rgb(255, 210, 100)
        3 -> Color.rgb(255, 120, 120)
        else -> Color.WHITE
    }
    p.alpha = 255
    p.textAlign = Paint.Align.CENTER
    p.textSize = h * 0.06f
    val lines = status.split("\n")
    c.drawText(lines[0], w / 2, h * 0.68f, p)
    if (lines.size > 1) {
        p.textSize = h * 0.04f
        c.drawText(lines[1], w / 2, h * 0.75f, p)
    }

    p.color = Color.WHITE
    p.alpha = 190
    p.textSize = h * 0.04f
    val hint = if (u.state == 2) "Tap to open the download page" else "Tap to check again"
    c.drawText(hint, w / 2, h * 0.9f, p)
}
