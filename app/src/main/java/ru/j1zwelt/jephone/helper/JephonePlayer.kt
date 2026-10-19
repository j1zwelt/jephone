package ru.j1zwelt.jephone.helper

import android.app.Application
import android.content.ContentUris
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.j1zwelt.jephone.model.Track
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri

class JephonePlayer(application: Application) : AndroidViewModel(application) {
    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(application).build()
    private var timerJob: Job? = null

    val trackList = mutableStateListOf<Track>()
    var currentTrack by mutableStateOf<Track?>(null)
    var currentMillis by mutableLongStateOf(0L)
    var isPlaying by mutableStateOf(false)
    var shuffleModeEnabled by mutableStateOf(false)
    var repeatMode by mutableIntStateOf(0)

    init {
        loadLocalMusic()

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                this@JephonePlayer.isPlaying = isPlaying

                stopChronometerJob()
                startChronometerJob()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                val currentIndex = exoPlayer.currentMediaItemIndex
                if (currentIndex in trackList.indices) {
                    this@JephonePlayer.currentTrack = trackList[currentIndex]
                    this@JephonePlayer.currentMillis = 0L
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                super.onShuffleModeEnabledChanged(shuffleModeEnabled)
                this@JephonePlayer.shuffleModeEnabled = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                super.onRepeatModeChanged(repeatMode)
                this@JephonePlayer.repeatMode = repeatMode
            }
        })
    }

    fun stopChronometerJob() {
        timerJob?.cancel()
    }

    fun startChronometerJob() {
        if (isPlaying) {
            timerJob = viewModelScope.launch {
                while (exoPlayer.isPlaying) {
                    currentMillis = exoPlayer.currentPosition
                    delay(1000L)
                }
            }
        }
    }

    fun loadLocalMusic() {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DISC_NUMBER,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )

        application.contentResolver?.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?",
            arrayOf("Music/%"),
            "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val numberColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISC_NUMBER)
            val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val albumId = cursor.getLong(albumIdColumn)

                val trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                val artworkUri = ContentUris.withAppendedId(
                    "content://media/external/audio/albumart".toUri(),
                    albumId
                )

                val music = Track(
                    id = id,
                    uri = trackUri,
                    cover = artworkUri,
                    number = cursor.getInt(numberColumn),
                    year = cursor.getInt(yearColumn),
                    genre = "",
                    title = cursor.getString(titleColumn),
                    artist = cursor.getString(artistColumn),
                    album = cursor.getString(albumColumn),
                    duration = cursor.getLong(durationColumn)
                )

                trackList.add(music)
            }
        }

        val mediaItems = trackList.map {
            MediaItem.Builder().setMediaId(it.uri.toString()).setUri(it.uri).setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(it.title)
                    .setArtist(it.artist).build()
            ).build()
        }

        Log.i("Attempt", trackList[1].toString())

        exoPlayer.setMediaItems(mediaItems, 0, 0)
        exoPlayer.prepare()
    }

    fun playTrack(track: Track) {
        val index = trackList.indexOf(track)
        if (index != -1) {
            exoPlayer.seekTo(index, 0L)
            exoPlayer.play()
        }
    }

    fun playOrStop() {
        if (exoPlayer.isPlaying) exoPlayer.pause()
        else exoPlayer.play()
    }

    fun seekToNext() {
        if (exoPlayer.hasNextMediaItem()) exoPlayer.seekToNext()
    }

    fun seekToPrevious() {
        if (exoPlayer.hasPreviousMediaItem()) exoPlayer.seekToPrevious()
    }

    fun seekTo(millis: Long) {
        exoPlayer.seekTo(millis)
    }

    fun toggleShuffleMode() {
        exoPlayer.shuffleModeEnabled = !exoPlayer.shuffleModeEnabled
    }

    fun toggleRepeatMode() {
        exoPlayer.repeatMode = when (exoPlayer.repeatMode) {
            ExoPlayer.REPEAT_MODE_OFF -> ExoPlayer.REPEAT_MODE_ONE
            ExoPlayer.REPEAT_MODE_ONE -> ExoPlayer.REPEAT_MODE_ALL
            else -> ExoPlayer.REPEAT_MODE_OFF
        }
    }

    companion object {
        fun Long.toTime(): String {
            val hours = TimeUnit.MILLISECONDS.toHours(this)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(this) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60

            val mm = minutes.toString().padStart(2, '0')
            val ss = seconds.toString().padStart(2, '0')

            return if (hours > 0) "$hours:$mm:$ss" else "$mm:$ss"
        }
    }

    override fun onCleared() {
        exoPlayer.release()
        timerJob?.cancel()
    }
}