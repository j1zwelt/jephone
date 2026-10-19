package ru.j1zwelt.jephone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.RepeatOneOn
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ShuffleOn
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.j1zwelt.jephone.helper.JephonePlayer
import ru.j1zwelt.jephone.helper.JephonePlayer.Companion.toTime
import ru.j1zwelt.jephone.model.Track
import ru.j1zwelt.jephone.ui.theme.JephoneTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JephoneTheme {
                val player: JephonePlayer = viewModel()
                val scaffoldState = rememberBottomSheetScaffoldState()
                val scope = rememberCoroutineScope()
                val listState = rememberLazyListState()

                LaunchedEffect(player.currentTrack) {
                    if (player.currentTrack != null) scope.launch {
                        scaffoldState.bottomSheetState.expand()
                    }
                }

                LaunchedEffect(listState.isScrollInProgress) {
                    if (listState.isScrollInProgress) scope.launch {
                        scaffoldState.bottomSheetState.partialExpand()
                    }
                }

                BottomSheetScaffold(
                    sheetContent = {
                        val track = player.currentTrack
                        if (track != null) Player(player, track)
                    }, topBar = {
                        TopAppBar({ Text(stringResource(R.string.app_name)) })
                    }, scaffoldState = scaffoldState
                ) { innerPadding ->
                    LazyColumn(
                        Modifier.padding(innerPadding), listState
                    ) {
                        items(player.trackList) {
                            Track(it, {
                                player.playTrack(it)
                            }, {

                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Player(player: JephonePlayer, track: Track) {
    Column(
        Modifier.padding(
            start = 56.dp, end = 56.dp, top = 24.dp, bottom = 48.dp
        ), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            track.cover,
            stringResource(R.string.cover),
            placeholder = rememberVectorPainter(image = Icons.Rounded.MusicNote),
            error = rememberVectorPainter(image = Icons.Rounded.MusicNote),
            modifier = Modifier
                .size(196.dp)
                .clip(RoundedCornerShape(16.dp))
        )

        Spacer(Modifier.height(16.dp))

        Text(
            track.title, maxLines = 1, style = MaterialTheme.typography.titleMedium
        )

        Text(
            track.artist,
            maxLines = 1,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        Slider(
            player.currentMillis.toFloat(),
            valueRange = 0f..track.duration.toFloat().coerceAtLeast(0f),
            onValueChange = {
                player.currentMillis = it.toLong()
                player.stopChronometerJob()
            },
            onValueChangeFinished = {
                player.seekTo(player.currentMillis)
                player.startChronometerJob()
            })

        Row(
            Modifier.fillMaxWidth(), Arrangement.SpaceBetween
        ) {
            Text(
                player.currentMillis.toTime(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                track.duration.toTime(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically
        ) {
            IconButton(
                {
                    player.seekToPrevious()
                }, Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.SkipPrevious,
                    stringResource(R.string.skip_previous),
                    Modifier.size(32.dp),
                    MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.width(8.dp))

            IconButton(
                {
                    player.playOrStop()
                },
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    stringResource(R.string.pause),
                    Modifier.size(36.dp),
                    MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(Modifier.width(8.dp))

            IconButton(
                {
                    player.seekToNext()
                }, Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    stringResource(R.string.skip_next),
                    Modifier.size(32.dp),
                    MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Row(
            Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    player.toggleRepeatMode()
                }, Modifier.size(48.dp)
            ) {
                Icon(
                    when (player.repeatMode) {
                        ExoPlayer.REPEAT_MODE_OFF -> Icons.Default.Repeat
                        ExoPlayer.REPEAT_MODE_ONE -> Icons.Default.RepeatOneOn
                        ExoPlayer.REPEAT_MODE_ALL -> Icons.Default.RepeatOn
                        else -> Icons.Default.Repeat
                    },
                    stringResource(R.string.repeat),
                    Modifier.size(32.dp),
                    MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    player.toggleShuffleMode()
                }, Modifier.size(48.dp)
            ) {
                Icon(
                    if (player.shuffleModeEnabled) Icons.Default.ShuffleOn else Icons.Default.Shuffle,
                    stringResource(R.string.shuffle_off),
                    Modifier.size(32.dp),
                    MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun Track(track: Track, onTrackClick: () -> Unit, onMoreClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = { onTrackClick() })
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            track.cover, // Наш Uri картинки из ViewModel
            stringResource(R.string.cover),
            placeholder = rememberVectorPainter(image = Icons.Rounded.MusicNote),
            error = rememberVectorPainter(image = Icons.Rounded.MusicNote),
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                track.title, maxLines = 1, style = MaterialTheme.typography.titleMedium
            )

            Text(
                track.artist,
                maxLines = 1,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            track.duration.toTime(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        IconButton(onClick = { onMoreClick() }) {
            Icon(
                Icons.Default.MoreVert,
                stringResource(R.string.more),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}