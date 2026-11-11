package com.pxp.player

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

data class Note(val freq: Double, val startMs: Int, val durMs: Int, val vol: Double = 1.0)

object Sfx {
    private const val SR = 44100
    var style = 0

    fun click() {
        if (style == 0) {
            play(listOf(Note(1500.0, 0, 60)), 0.15)
        } else {
            play(listOf(Note(2200.0, 0, 40), Note(1760.0, 45, 60)), 0.14)
        }
    }

    fun ok() = play(listOf(Note(880.0, 0, 120), Note(1320.0, 70, 200)), 0.2)

    fun boot() = play(
        listOf(
            Note(130.8, 0, 3200, 0.7),
            Note(261.6, 0, 3000, 0.5),
            Note(523.3, 150, 2600, 0.6),
            Note(659.3, 450, 2400, 0.5),
            Note(784.0, 750, 2200, 0.5),
            Note(1046.5, 1050, 2200, 0.6),
            Note(1568.0, 1350, 1800, 0.3)
        ),
        0.35
    )

    private fun play(notes: List<Note>, master: Double) {
        Thread {
            try {
                var endMs = 0
                for (n in notes) endMs = maxOf(endMs, n.startMs + n.durMs)
                val total = SR * (endMs + 100) / 1000
                val mix = DoubleArray(total)
                for (n in notes) {
                    val s0 = SR * n.startMs / 1000
                    val len = SR * n.durMs / 1000
                    val attack = maxOf(1, minOf(SR * 25 / 1000, len / 4))
                    val release = maxOf(1, minOf(SR * 80 / 1000, len / 3))
                    for (i in 0 until len) {
                        val idx = s0 + i
                        if (idx >= total) break
                        val ph = 2 * PI * n.freq * i / SR
                        val tone = sin(ph) + 0.25 * sin(2 * ph) + 0.1 * sin(3 * ph)
                        val a = minOf(1.0, i.toDouble() / attack)
                        val r = minOf(1.0, (len - i).toDouble() / release)
                        val d = exp(-3.0 * i / len)
                        mix[idx] += tone * a * r * d * n.vol
                    }
                }
                var peak = 0.0
                for (v in mix) peak = maxOf(peak, abs(v))
                val scale = if (peak > 0.0) master / peak else 0.0
                val buf = ShortArray(total)
                for (i in 0 until total) {
                    buf[i] = (mix[i] * scale * 32767).toInt().toShort()
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SR)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buf.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(buf, 0, buf.size)
                track.play()
                Thread.sleep(total * 1000L / SR + 200)
                track.release()
            } catch (e: Exception) {
            }
        }.start()
    }
}
