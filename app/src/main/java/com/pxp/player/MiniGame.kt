package com.pxp.player

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class MiniGame {
    private class Enemy(var x: Float, var y: Float, val kind: Int)

    private val enemies = ArrayList<Enemy>()
    private val bullets = ArrayList<FloatArray>()
    private val shots = ArrayList<FloatArray>()
    private val path = Path()
    private var px = 0.5f
    private var targetX = 0.5f
    private var dir = 1f
    private var score = 0
    private var best = 0
    private var lives = 3
    private var level = 1
    private var over = false
    private var last = 0L
    private var fireCd = 0f
    private var enemyCd = 1.2f
    private var hurt = 0f

    init {
        reset()
    }

    fun reset() {
        score = 0
        lives = 3
        level = 1
        over = false
        px = 0.5f
        targetX = 0.5f
        bullets.clear()
        shots.clear()
        hurt = 0f
        last = 0L
        spawn()
    }

    private fun spawn() {
        enemies.clear()
        for (r in 0 until 4) {
            for (c in 0 until 7) {
                enemies.add(Enemy(0.14f + c * 0.12f, 0.12f + r * 0.09f, r))
            }
        }
        dir = 1f
    }

    fun nudge(d: Float) {
        targetX = (targetX + d).coerceIn(0.05f, 0.95f)
    }

    fun action() {
        if (over) reset()
    }

    fun onTouch(e: MotionEvent, w: Float, h: Float) {
        val pw = h * 0.9f
        val ox = (w - pw) / 2f
        val fx = ((e.x - ox) / pw).coerceIn(0.05f, 0.95f)
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> if (over) reset() else targetX = fx
            MotionEvent.ACTION_MOVE -> if (!over) targetX = fx
        }
    }

    private fun update(dt: Float) {
        if (over || dt <= 0f) return
        px += (targetX - px) * minOf(1f, dt * 12f)
        hurt = maxOf(0f, hurt - dt)
        fireCd -= dt
        if (fireCd <= 0f) {
            bullets.add(floatArrayOf(px, 0.86f))
            fireCd = 0.28f
        }
        val speed = 0.07f + 0.02f * level
        var edge = false
        for (e in enemies) {
            e.x += dir * speed * dt
            if (e.x < 0.06f || e.x > 0.94f) edge = true
        }
        if (edge) {
            dir = -dir
            for (e in enemies) {
                e.y += 0.035f
                e.x += dir * 0.01f
            }
        }
        val bi = bullets.iterator()
        while (bi.hasNext()) {
            val b = bi.next()
            b[1] -= 1.1f * dt
            val hit = enemies.firstOrNull {
                abs(b[0] - it.x) < 0.045f && abs(b[1] - it.y) < 0.04f
            }
            if (hit != null) {
                enemies.remove(hit)
                score += 40 - hit.kind * 10
                bi.remove()
            } else if (b[1] < 0f) {
                bi.remove()
            }
        }
        enemyCd -= dt
        if (enemyCd <= 0f && enemies.isNotEmpty()) {
            val e = enemies[Random.nextInt(enemies.size)]
            shots.add(floatArrayOf(e.x, e.y))
            enemyCd = maxOf(0.35f, 1.3f - 0.15f * level) * (0.6f + Random.nextFloat() * 0.8f)
        }
        val si = shots.iterator()
        while (si.hasNext()) {
            val b = si.next()
            b[1] += (0.45f + 0.04f * level) * dt
            if (hurt <= 0f && abs(b[0] - px) < 0.05f && abs(b[1] - 0.9f) < 0.04f) {
                lives--
                hurt = 1.2f
                si.remove()
                if (lives <= 0) {
                    over = true
                    best = maxOf(best, score)
                }
            } else if (b[1] > 1f) {
                si.remove()
            }
        }
        if (enemies.any { it.y > 0.82f }) {
            over = true
            best = maxOf(best, score)
        }
        if (enemies.isEmpty()) {
            level++
            spawn()
            shots.clear()
        }
    }

    fun draw(c: Canvas, p: Paint, w: Float, h: Float) {
        val now = System.nanoTime()
        val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
        last = now
        update(dt)
        val t = (now / 1_000_000L % 1_000_000L) / 1000f

        val pw = h * 0.9f
        val ox = (w - pw) / 2f
        c.drawColor(Color.BLACK)
        p.style = Paint.Style.FILL
        p.clearShadowLayer()
        p.letterSpacing = 0f

        for (i in 0 until 60) {
            val fx = ((i * 7919) % 1000) / 1000f
            val fy = ((i * 104729 + 13) % 1000) / 1000f
            val y = (fy + t * 0.03f * (1 + i % 3)) % 1f
            p.color = Color.WHITE
            p.alpha = 90 + (i % 4) * 40
            c.drawCircle(ox + fx * pw, y * h, 1.5f + (i % 3), p)
        }
        p.color = Color.WHITE
        p.alpha = 50
        c.drawRect(ox - 2f, 0f, ox, h, p)
        c.drawRect(ox + pw, 0f, ox + pw + 2f, h, p)

        val s = h * 0.055f
        val colors = intArrayOf(
            Color.rgb(255, 90, 90), Color.rgb(255, 180, 60),
            Color.rgb(120, 230, 120), Color.rgb(100, 180, 255)
        )
        for (e in enemies) {
            val ex = ox + e.x * pw
            val ey = e.y * h
            val wig = sin(t * 8f + e.x * 20f) * s * 0.15f
            p.color = colors[e.kind % 4]
            c.drawRoundRect(
                RectF(ex - s * 0.7f, ey - s * 0.4f, ex + s * 0.7f, ey + s * 0.4f),
                s * 0.25f, s * 0.25f, p
            )
            c.drawRect(ex - s * 0.95f, ey - s * 0.1f + wig, ex - s * 0.7f, ey + s * 0.25f + wig, p)
            c.drawRect(ex + s * 0.7f, ey - s * 0.1f - wig, ex + s * 0.95f, ey + s * 0.25f - wig, p)
            p.color = Color.BLACK
            c.drawRect(ex - s * 0.4f, ey - s * 0.15f, ex - s * 0.15f, ey + s * 0.1f, p)
            c.drawRect(ex + s * 0.15f, ey - s * 0.15f, ex + s * 0.4f, ey + s * 0.1f, p)
        }

        val blink = hurt > 0f && (now / 100_000_000L) % 2L == 0L
        if (!blink && !over) {
            val cx = ox + px * pw
            val py = 0.9f * h
            path.reset()
            path.moveTo(cx, py - s * 0.9f)
            path.lineTo(cx - s * 0.8f, py + s * 0.6f)
            path.lineTo(cx, py + s * 0.3f)
            path.lineTo(cx + s * 0.8f, py + s * 0.6f)
            path.close()
            p.color = Color.rgb(120, 230, 255)
            c.drawPath(path, p)
        }

        p.color = Color.rgb(255, 240, 150)
        for (b in bullets) {
            val bx = ox + b[0] * pw
            c.drawRect(bx - s * 0.07f, b[1] * h - s * 0.3f, bx + s * 0.07f, b[1] * h + s * 0.3f, p)
        }
        p.color = Color.rgb(255, 110, 160)
        for (b in shots) {
            val bx = ox + b[0] * pw
            c.drawRect(bx - s * 0.07f, b[1] * h - s * 0.3f, bx + s * 0.07f, b[1] * h + s * 0.3f, p)
        }

        p.color = Color.WHITE
        p.alpha = 255
        p.textSize = h * 0.045f
        p.textAlign = Paint.Align.LEFT
        c.drawText("SCORE $score", ox + pw * 0.03f, h * 0.07f, p)
        p.textAlign = Paint.Align.CENTER
        c.drawText("LV $level", ox + pw / 2, h * 0.07f, p)
        p.textAlign = Paint.Align.RIGHT
        c.drawText("LIVES $lives", ox + pw * 0.97f, h * 0.07f, p)

        if (over) {
            p.color = Color.BLACK
            p.alpha = 170
            c.drawRect(ox, 0f, ox + pw, h, p)
            p.color = Color.WHITE
            p.alpha = 255
            p.textAlign = Paint.Align.CENTER
            p.textSize = h * 0.1f
            c.drawText("GAME OVER", w / 2, h * 0.42f, p)
            p.textSize = h * 0.05f
            c.drawText("Score $score   Best $best", w / 2, h * 0.54f, p)
            c.drawText("Tap to play again", w / 2, h * 0.64f, p)
        }
        p.color = Color.WHITE
        p.alpha = 150
        p.textAlign = Paint.Align.CENTER
        p.textSize = h * 0.035f
        c.drawText("Back to exit", w / 2, h * 0.97f, p)
    }
}
