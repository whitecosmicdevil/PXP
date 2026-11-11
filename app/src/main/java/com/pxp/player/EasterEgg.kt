package com.pxp.player

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.sin
import kotlin.math.hypot

const val EGG_TEXT = "Built on a phone. Powered by stubbornness."
var eggClicks = 0
var miniGameMode = false
var playerX = 0f
var playerY = 0f
var dotX = 0f
var dotY = 0f
var score = 0

fun resetMiniGame(w: Float, h: Float) {
    playerX = w / 2f
    playerY = h * 0.75f
    dotX = (Math.random() * w * 0.8f + w * 0.1f).toFloat()
    dotY = h * 0.4f
    score = 0
}

fun drawEgg(c: Canvas, p: Paint, w: Float, h: Float, t: Float) {
    p.style = Paint.Style.FILL
    p.color = Color.BLACK
    p.alpha = 235
    c.drawRect(0f, 0f, w, h, p)

    if (!miniGameMode) {
        val path = Path()
        for (k in 0..6) {
            path.reset()
            path.moveTo(0f, h)
            var x = 0f
            while (x <= w) {
                val y = h * (0.4f + 0.08f * k) + sin(x / w * 5f + t * 1.5f + k * 0.6f) * h * 0.06f
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
        p.alpha = 255
        p.textAlign = Paint.Align.CENTER
        p.textSize = h * 0.09f
        p.letterSpacing = 0.15f
        c.drawText("DEVELOPER MODE", w / 2, h * 0.2f, p)
        p.letterSpacing = 0f
        p.textSize = h * 0.05f
        c.drawText(EGG_TEXT, w / 2, h * 0.3f, p)

        // Animated "Made by Tanmay"
        val scaleAnim = 1f + 0.08f * sin(t * 6f)
        c.save()
        c.scale(scaleAnim, scaleAnim, w / 2, h * 0.52f)
        p.textSize = h * 0.06f
        p.color = Color.rgb(255, 215, 0)
        c.drawText("Made by Tanmay", w / 2, h * 0.52f, p)
        c.restore()

        p.color = Color.WHITE
        p.alpha = 170
        p.textSize = h * 0.035f
        c.drawText("Tap 'Made by Tanmay' 5 times for Retro Game", w / 2, h * 0.68f, p)
        c.drawText("Swipe right to exit", w / 2, h * 0.92f, p)
    } else {
        // Namco-style Retro Mini-Game
        p.color = Color.WHITE
        p.textSize = h * 0.04f
        p.textAlign = Paint.Align.LEFT
        c.drawText("Score: $score", w * 0.05f, h * 0.1f, p)
        p.textAlign = Paint.Align.RIGHT
        c.drawText("Touch to move | Back to exit", w * 0.95f, h * 0.1f, p)

        p.color = Color.YELLOW
        c.drawCircle(playerX, playerY, h * 0.04f, p)

        p.color = Color.RED
        c.drawCircle(dotX, dotY, h * 0.03f, p)

        dotY += h * 0.005f
        if (dotY > h) {
            dotY = h * 0.4f
            dotX = (Math.random() * w * 0.8f + w * 0.1f).toFloat()
        }

        val dist = hypot((playerX - dotX).toDouble(), (playerY - dotY).toDouble())
        if (dist < h * 0.07f) {
            score++
            Sfx.ok()
            dotY = h * 0.4f
            dotX = (Math.random() * w * 0.8f + w * 0.1f).toFloat()
        }
    }
}

fun handleEggTouch(x: Float, y: Float, w: Float, h: Float): Boolean {
    if (miniGameMode) {
        playerX = x
        return true
    } else {
        if (y in (h * 0.42f)..(h * 0.62f) && x in (w * 0.25f)..(w * 0.75f)) {
            eggClicks++
            Sfx.click()
            if (eggClicks >= 5) {
                miniGameMode = true
                resetMiniGame(w, h)
                Sfx.egg()
            }
            return true
        }
    }
    return false
}
