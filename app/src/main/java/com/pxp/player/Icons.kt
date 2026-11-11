package com.pxp.player

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

fun drawXmbIcon(c: Canvas, p: Paint, i: Int, x: Float, y: Float, size: Float) {
    val u = size / 10f
    p.style = Paint.Style.FILL
    when (i) {
        0 -> {
            for (k in 0 until 8) {
                c.save()
                c.rotate(k * 45f, x, y)
                c.drawRoundRect(
                    RectF(x - u * 0.9f, y - u * 4.4f, x + u * 0.9f, y - u * 2.6f),
                    u * 0.3f, u * 0.3f, p
                )
                c.restore()
            }
            val g = Path()
            g.fillType = Path.FillType.EVEN_ODD
            g.addCircle(x, y, u * 3.3f, Path.Direction.CW)
            g.addCircle(x, y, u * 1.4f, Path.Direction.CW)
            c.drawPath(g, p)
        }
        1 -> {
            p.style = Paint.Style.STROKE
            p.strokeWidth = u * 0.8f
            c.drawRoundRect(
                RectF(x - u * 4.2f, y - u * 3.2f, x + u * 4.2f, y + u * 3.2f),
                u * 0.9f, u * 0.9f, p
            )
            p.style = Paint.Style.FILL
            val m = Path()
            m.moveTo(x - u * 3.2f, y + u * 2.2f)
            m.lineTo(x - u * 0.9f, y - u * 0.9f)
            m.lineTo(x + u * 0.6f, y + u * 1.0f)
            m.lineTo(x + u * 1.5f, y - u * 0.1f)
            m.lineTo(x + u * 3.2f, y + u * 2.2f)
            m.close()
            c.drawPath(m, p)
            c.drawCircle(x + u * 2.2f, y - u * 1.5f, u * 0.9f, p)
        }
        2 -> {
            c.drawOval(RectF(x - u * 3.2f, y + u * 1.2f, x - u * 0.8f, y + u * 3.0f), p)
            c.drawOval(RectF(x + u * 0.8f, y + u * 0.4f, x + u * 3.2f, y + u * 2.2f), p)
            c.drawRect(x - u * 1.1f, y - u * 3.4f, x - u * 0.5f, y + u * 2.2f, p)
            c.drawRect(x + u * 2.9f, y - u * 4.2f, x + u * 3.5f, y + u * 1.4f, p)
            val b = Path()
            b.moveTo(x - u * 1.1f, y - u * 3.4f)
            b.lineTo(x + u * 3.5f, y - u * 4.4f)
            b.lineTo(x + u * 3.5f, y - u * 2.8f)
            b.lineTo(x - u * 1.1f, y - u * 1.8f)
            b.close()
            c.drawPath(b, p)
        }
        3 -> {
            val v = Path()
            v.fillType = Path.FillType.EVEN_ODD
            v.addRoundRect(
                RectF(x - u * 4.2f, y - u * 3f, x + u * 4.2f, y + u * 3f),
                u * 0.9f, u * 0.9f, Path.Direction.CW
            )
            v.moveTo(x - u * 1.2f, y - u * 1.6f)
            v.lineTo(x + u * 2f, y)
            v.lineTo(x - u * 1.2f, y + u * 1.6f)
            v.close()
            c.drawPath(v, p)
        }
        4 -> {
            val g = Path()
            g.fillType = Path.FillType.EVEN_ODD
            g.addRoundRect(
                RectF(x - u * 4.6f, y - u * 2.6f, x + u * 4.6f, y + u * 2.6f),
                u * 2f, u * 2f, Path.Direction.CW
            )
            val cx = x - u * 2.4f
            val a = u * 0.5f
            val hh = u * 1.5f
            g.moveTo(cx - a, y - hh)
            g.lineTo(cx + a, y - hh)
            g.lineTo(cx + a, y - a)
            g.lineTo(cx + hh, y - a)
            g.lineTo(cx + hh, y + a)
            g.lineTo(cx + a, y + a)
            g.lineTo(cx + a, y + hh)
            g.lineTo(cx - a, y + hh)
            g.lineTo(cx - a, y + a)
            g.lineTo(cx - hh, y + a)
            g.lineTo(cx - hh, y - a)
            g.lineTo(cx - a, y - a)
            g.close()
            g.addCircle(x + u * 2.0f, y + u * 0.7f, u * 0.55f, Path.Direction.CW)
            g.addCircle(x + u * 3.1f, y - u * 0.5f, u * 0.55f, Path.Direction.CW)
            c.drawPath(g, p)
        }
        else -> {
            p.style = Paint.Style.STROKE
            p.strokeWidth = u * 0.7f
            c.drawCircle(x, y, u * 3.6f, p)
            c.drawOval(RectF(x - u * 1.6f, y - u * 3.6f, x + u * 1.6f, y + u * 3.6f), p)
            c.drawLine(x - u * 3.6f, y, x + u * 3.6f, y, p)
            c.drawLine(x - u * 3.0f, y - u * 1.8f, x + u * 3.0f, y - u * 1.8f, p)
            c.drawLine(x - u * 3.0f, y + u * 1.8f, x + u * 3.0f, y + u * 1.8f, p)
        }
    }
    p.style = Paint.Style.FILL
}
