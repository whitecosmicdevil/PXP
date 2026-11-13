package com.pxp.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View

class PadView(ctx: Context, private val onKey: (Int, Boolean) -> Unit) : View(ctx) {
    private class Face(val code: Int, val kind: Int) {
        var x = 0f
        var y = 0f
    }

    private class Box(val code: Int, val label: String) {
        val rect = RectF()
    }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val pressed = HashSet<Int>()

    private val faces = listOf(
        Face(KeyEvent.KEYCODE_BUTTON_Y, 0),
        Face(KeyEvent.KEYCODE_BUTTON_B, 1),
        Face(KeyEvent.KEYCODE_BUTTON_A, 2),
        Face(KeyEvent.KEYCODE_BUTTON_X, 3)
    )
    private val boxes = listOf(
        Box(KeyEvent.KEYCODE_BUTTON_L1, "L"),
        Box(KeyEvent.KEYCODE_BUTTON_R1, "R"),
        Box(KeyEvent.KEYCODE_BUTTON_SELECT, "SELECT"),
        Box(KeyEvent.KEYCODE_BUTTON_START, "START")
    )

    private var dpX = 0f
    private var dpY = 0f
    private var dpR = 0f
    private var br = 0f

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val u = h.toFloat()
        val wf = w.toFloat()
        dpR = u * 0.2f
        dpX = u * 0.32f
        dpY = u * 0.66f
        br = u * 0.085f
        val fx = wf - u * 0.32f
        val fy = u * 0.66f
        val off = u * 0.15f
        faces[0].x = fx
        faces[0].y = fy - off
        faces[1].x = fx + off
        faces[1].y = fy
        faces[2].x = fx
        faces[2].y = fy + off
        faces[3].x = fx - off
        faces[3].y = fy

        boxes[0].rect.set(u * 0.1f, u * 0.04f, u * 0.1f + u * 0.3f, u * 0.04f + u * 0.13f)
        boxes[1].rect.set(wf - u * 0.1f - u * 0.3f, u * 0.04f, wf - u * 0.1f, u * 0.04f + u * 0.13f)
        val sw = u * 0.28f
        val sh = u * 0.1f
        val sy = u - u * 0.04f - sh
        boxes[2].rect.set(wf / 2f - u * 0.2f - sw / 2f, sy, wf / 2f - u * 0.2f + sw / 2f, sy + sh)
        boxes[3].rect.set(wf / 2f + u * 0.2f - sw / 2f, sy, wf / 2f + u * 0.2f + sw / 2f, sy + sh)
    }

    private fun hit(x: Float, y: Float, out: MutableSet<Int>) {
        val dx = x - dpX
        val dy = y - dpY
        if (dx * dx + dy * dy <= dpR * dpR * 1.44f) {
            val t = dpR * 0.3f
            if (dy < -t) out.add(KeyEvent.KEYCODE_DPAD_UP)
            if (dy > t) out.add(KeyEvent.KEYCODE_DPAD_DOWN)
            if (dx < -t) out.add(KeyEvent.KEYCODE_DPAD_LEFT)
            if (dx > t) out.add(KeyEvent.KEYCODE_DPAD_RIGHT)
        }
        for (f in faces) {
            val fx = x - f.x
            val fy = y - f.y
            if (fx * fx + fy * fy <= br * br * 1.6f) out.add(f.code)
        }
        for (b in boxes) {
            if (b.rect.contains(x, y)) out.add(b.code)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val now = HashSet<Int>()
        val action = e.actionMasked
        val ended = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL
        val skip = if (action == MotionEvent.ACTION_POINTER_UP) e.actionIndex else -1
        if (!ended) {
            for (i in 0 until e.pointerCount) {
                if (i == skip) continue
                hit(e.getX(i), e.getY(i), now)
            }
        }
        for (k in pressed) {
            if (k !in now) onKey(k, false)
        }
        for (k in now) {
            if (k !in pressed) onKey(k, true)
        }
        pressed.clear()
        pressed.addAll(now)
        invalidate()
        return true
    }

    private fun arm(c: Canvas, l: Float, t: Float, r: Float, b: Float, code: Int) {
        p.style = Paint.Style.FILL
        p.color = Color.WHITE
        p.alpha = if (code in pressed) 170 else 70
        c.drawRoundRect(RectF(l, t, r, b), dpR * 0.12f, dpR * 0.12f, p)
    }

    override fun onDraw(c: Canvas) {
        val t = dpR * 0.32f
        arm(c, dpX - t, dpY - dpR, dpX + t, dpY, KeyEvent.KEYCODE_DPAD_UP)
        arm(c, dpX - t, dpY, dpX + t, dpY + dpR, KeyEvent.KEYCODE_DPAD_DOWN)
        arm(c, dpX - dpR, dpY - t, dpX, dpY + t, KeyEvent.KEYCODE_DPAD_LEFT)
        arm(c, dpX, dpY - t, dpX + dpR, dpY + t, KeyEvent.KEYCODE_DPAD_RIGHT)

        for (f in faces) {
            p.style = Paint.Style.FILL
            p.color = Color.WHITE
            p.alpha = if (f.code in pressed) 170 else 60
            c.drawCircle(f.x, f.y, br, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = br * 0.08f
            p.alpha = 150
            c.drawCircle(f.x, f.y, br, p)
            p.strokeWidth = br * 0.14f
            when (f.kind) {
                0 -> {
                    p.color = Color.rgb(120, 230, 120)
                    path.reset()
                    path.moveTo(f.x, f.y - br * 0.5f)
                    path.lineTo(f.x - br * 0.5f, f.y + br * 0.38f)
                    path.lineTo(f.x + br * 0.5f, f.y + br * 0.38f)
                    path.close()
                    c.drawPath(path, p)
                }
                1 -> {
                    p.color = Color.rgb(255, 110, 110)
                    c.drawCircle(f.x, f.y, br * 0.45f, p)
                }
                2 -> {
                    p.color = Color.rgb(110, 170, 255)
                    c.drawLine(f.x - br * 0.4f, f.y - br * 0.4f, f.x + br * 0.4f, f.y + br * 0.4f, p)
                    c.drawLine(f.x - br * 0.4f, f.y + br * 0.4f, f.x + br * 0.4f, f.y - br * 0.4f, p)
                }
                else -> {
                    p.color = Color.rgb(255, 150, 200)
                    c.drawRect(f.x - br * 0.4f, f.y - br * 0.4f, f.x + br * 0.4f, f.y + br * 0.4f, p)
                }
            }
        }

        for (b in boxes) {
            p.style = Paint.Style.FILL
            p.color = Color.WHITE
            p.alpha = if (b.code in pressed) 150 else 50
            val rr = b.rect.height() * 0.3f
            c.drawRoundRect(b.rect, rr, rr, p)
            p.alpha = 220
            p.textAlign = Paint.Align.CENTER
            p.textSize = b.rect.height() * 0.45f
            c.drawText(b.label, b.rect.centerX(), b.rect.centerY() + p.textSize * 0.35f, p)
        }
        p.style = Paint.Style.FILL
    }
}
