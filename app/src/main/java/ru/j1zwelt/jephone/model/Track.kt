package ru.j1zwelt.jephone.model

import android.net.Uri

data class Track(
    val id: Long,
    val uri: Uri,
    val cover: Uri,
    val number: Int,
    val year: Int,
    val genre: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long
)
