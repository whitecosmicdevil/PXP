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

private fun glyphClock(c: Canvas, p: Paint, x: Float, y: Float, u: Float) {
    p.style = Paint.Style.STROKE
    p.strokeWidth = u * 0.8f
    p.strokeCap = Paint.Cap.ROUND
    c.drawCircle(x, y, u * 3.8f, p)
    c.drawLine(x, y, x, y - u * 2.4f, p)
    c.drawLine(x, y, x + u * 1.9f, y + u * 1.1f, p)
    p.strokeCap = Paint.Cap.BUTT
    p.style = Paint.Style.FILL
}

private fun glyphDrop(c: Canvas, p: Paint, x: Float, y: Float, u: Float) {
    val d = Path()
    d.moveTo(x, y - u * 4.2f)
    d.cubicTo(x + u * 4.6f, y, x + u * 3.4f, y + u * 3.8f, x, y + u * 3.8f)
    d.cubicTo(x - u * 3.4f, y + u * 3.8f, x - u * 4.6f, y, x, y - u * 4.2f)
    d.close()
    p.style = Paint.Style.FILL
    c.drawPath(d, p)
}

private fun glyphSpeaker(c: Canvas, p: Paint, x: Float, y: Float, u: Float) {
    val s = Path()
    s.moveTo(x - u * 3.8f, y - u * 1.4f)
    s.lineTo(x - u * 1.6f, y - u * 1.4f)
    s.lineTo(x + u * 0.8f, y - u * 3.4f)
    s.lineTo(x + u * 0.8f, y + u * 3.4f)
    s.lineTo(x - u * 1.6f, y + u * 1.4f)
    s.lineTo(x - u * 3.8f, y + u * 1.4f)
    s.close()
    p.style = Paint.Style.FILL
    c.drawPath(s, p)
    p.style = Paint.Style.STROKE
    p.strokeWidth = u * 0.7f
    p.strokeCap = Paint.Cap.ROUND
    c.drawArc(RectF(x - u * 0.6f, y - u * 2.4f, x + u * 3.0f, y + u * 2.4f), -45f, 90f, false, p)
    c.drawArc(RectF(x - u * 1.0f, y - u * 3.8f, x + u * 4.2f, y + u * 3.8f), -45f, 90f, false, p)
    p.strokeCap = Paint.Cap.BUTT
    p.style = Paint.Style.FILL
}

private fun glyphCode(c: Canvas, p: Paint, x: Float, y: Float, u: Float) {
    p.style = Paint.Style.STROKE
    p.strokeWidth = u * 0.8f
    p.strokeCap = Paint.Cap.ROUND
    val l = Path()
    l.moveTo(x - u * 1.4f, y - u * 2.6f)
    l.lineTo(x - u * 4f, y)
    l.lineTo(x - u * 1.4f, y + u * 2.6f)
    c.drawPath(l, p)
    val r = Path()
    r.moveTo(x + u * 1.4f, y - u * 2.6f)
    r.lineTo(x + u * 4f, y)
    r.lineTo(x + u * 1.4f, y + u * 2.6f)
    c.drawPath(r, p)
    c.drawLine(x + u * 0.9f, y - u * 3.2f, x - u * 0.9f, y + u * 3.2f, p)
    p.strokeCap = Paint.Cap.BUTT
    p.style = Paint.Style.FILL
}

private fun glyphCard(c: Canvas, p: Paint, x: Float, y: Float, u: Float) {
    val g = Path()
    g.fillType = Path.FillType.EVEN_ODD
    g.moveTo(x - u * 3f, y - u * 4f)
    g.lineTo(x + u * 1.4f, y - u * 4f)
    g.lineTo(x + u * 3f, y - u * 2.4f)
    g.lineTo(x + u * 3f, y + u * 4f)
    g.lineTo(x - u * 3f, y + u * 4f)
    g.close()
    for (i in 0..2) {
        val sx = x - u * 2.1f + i * u * 1.5f
        g.addRect(sx, y - u * 3.2f, sx + u * 0.8f, y - u * 1.2f, Path.Direction.CW)
    }
    p.style = Paint.Style.FILL
    c.drawPath(g, p)
}

private fun glyphDownload(c: Canvas, p: Paint, x: Float, y: Float, u: Float) {
    p.style = Paint.Style.FILL
    c.drawRect(x - u * 0.9f, y - u * 4.2f, x + u * 0.9f, y + u * 0.4f, p)
    val h = Path()
    h.moveTo(x - u * 3f, y + u * 0.1f)
    h.lineTo(x + u * 3f, y + u * 0.1f)
    h.lineTo(x, y + u * 3.1f)
    h.close()
    c.drawPath(h, p)
    p.style = Paint.Style.STROKE
    p.strokeWidth = u * 0.9f
    c.drawLine(x - u * 3.6f, y + u * 4.3f, x + u * 3.6f, y + u * 4.3f, p)
    p.style = Paint.Style.FILL
}

fun drawItemIcon(c: Canvas, p: Paint, label: String, x: Float, y: Float, size: Float) {
    val u = size / 10f
    when {
        label.startsWith("Time Zone") -> glyphClock(c, p, x, y, u)
        label == "System Settings" -> drawXmbIcon(c, p, 0, x, y, size)
        label.startsWith("Theme") -> glyphDrop(c, p, x, y, u)
        label.startsWith("Sound") -> glyphSpeaker(c, p, x, y, u)
        label == "Developer Mode" -> glyphCode(c, p, x, y, u)
        label == "Memory Stick" -> glyphCard(c, p, x, y, u)
        label == "Photo Viewer" -> drawXmbIcon(c, p, 1, x, y, size)
        label == "Music Player" -> drawXmbIcon(c, p, 2, x, y, size)
        label == "Video Player" -> drawXmbIcon(c, p, 3, x, y, size)
        label == "PXP Games" -> drawXmbIcon(c, p, 4, x, y, size)
        label == "Network Update" -> glyphDownload(c, p, x, y, u)
        label == "Browser" -> drawXmbIcon(c, p, 5, x, y, size)
        else -> {
            p.style = Paint.Style.FILL
            c.drawRoundRect(
                RectF(x - size / 2, y - size / 2, x + size / 2, y + size / 2),
                size * 0.2f, size * 0.2f, p
            )
        }
    }
    p.style = Paint.Style.FILL
}
