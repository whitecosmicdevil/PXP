package com.pxp.player

import android.net.Uri

const val T_GAME = 0
const val T_PHOTO = 1
const val T_VIDEO = 2
const val T_MUSIC = 3

data class Entry(val name: String, val uri: Uri, val type: Int)

fun typeOf(name: String): Int {
    val l = name.lowercase()
    val games = listOf(".iso", ".cso", ".pbp", ".chd")
    val photos = listOf(".jpg", ".jpeg", ".png", ".webp", ".bmp")
    val videos = listOf(".mp4", ".mkv", ".3gp", ".webm", ".avi")
    val music = listOf(".mp3", ".m4a", ".aac", ".wav", ".ogg", ".flac")
    return when {
        games.any { l.endsWith(it) } -> T_GAME
        photos.any { l.endsWith(it) } -> T_PHOTO
        videos.any { l.endsWith(it) } -> T_VIDEO
        music.any { l.endsWith(it) } -> T_MUSIC
        else -> -1
    }
}
