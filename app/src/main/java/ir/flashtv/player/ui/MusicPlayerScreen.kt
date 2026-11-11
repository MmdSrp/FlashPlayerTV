@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package ir.flashtv.player.ui

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private data class Meta(
    val title: String,
    val artist: String?,
    val album: String?,
    val cover: ImageBitmap?,
)

private fun readMeta(path: String): Meta {
    val f = File(path)
    val r = MediaMetadataRetriever()
    return try {
        r.setDataSource(path)
        fun s(key: Int) = r.extractMetadata(key)?.trim()?.takeIf { it.isNotEmpty() }
        val cover = r.embeddedPicture?.let { bytes ->
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 800) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)?.asImageBitmap()
        }
        Meta(
            s(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: f.nameWithoutExtension,
            s(MediaMetadataRetriever.METADATA_KEY_ARTIST),
            s(MediaMetadataRetriever.METADATA_KEY_ALBUM),
            cover,
        )
    } catch (e: Exception) {
        Meta(f.nameWithoutExtension, null, null, null)
    } finally {
        runCatching { r.release() }
    }
}

@Composable
fun MusicPlayerScreen(paths: List<String>, startIndex: Int) {
    if (paths.isEmpty()) return
    val ctx = LocalContext.current
    KeepScreenOn()
    val safeStart = startIndex.coerceIn(0, paths.lastIndex)

    val exo = remember {
        ExoPlayer.Builder(ctx)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .build()
            .apply {
                setMediaItems(paths.map { MediaItem.fromUri(Uri.fromFile(File(it))) }, safeStart, 0L)
                repeatMode = Player.REPEAT_ALL
                prepare()
                playWhenReady = true
            }
    }

    var index by remember { mutableIntStateOf(safeStart) }
    var playing by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    val focus = remember { FocusRequester() }

    DisposableEffect(exo) {
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) dur = exo.duration.coerceAtLeast(0L)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                index = exo.currentMediaItemIndex
                error = null
                dur = exo.duration.coerceAtLeast(0L)
            }

            override fun onPlayerError(e: PlaybackException) {
                error = "این آهنگ پخش نشد"
            }
        }
        exo.addListener(l)
        onDispose {
            exo.removeListener(l)
            exo.release()
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        runCatching { focus.requestFocus() }
    }
    LaunchedEffect(exo) {
        while (true) {
            pos = exo.currentPosition.coerceAtLeast(0L)
            if (exo.duration > 0) dur = exo.duration
            delay(300)
        }
    }
    LaunchedEffect(error) {
        if (error != null && paths.size > 1) {
            delay(2000)
            exo.seekToNextMediaItem()
            exo.prepare()
            exo.play()
        }
    }

    val meta by produceState<Meta?>(null, index) {
        value = withContext(Dispatchers.IO) { readMeta(paths[index.coerceIn(0, paths.lastIndex)]) }
    }

    fun seekBy(ms: Long) {
        val t = (exo.currentPosition + ms).coerceAtLeast(0L)
        exo.seekTo(if (exo.duration > 0) t.coerceAtMost(exo.duration) else t)
    }

    fun skip(next: Boolean) {
        if (next) exo.seekToNextMediaItem() else exo.seekToPreviousMediaItem()
        exo.prepare()
        exo.play()
    }

    Box(
        Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.MediaPlayPause -> {
                        if (exo.isPlaying) exo.pause() else exo.play(); true
                    }
                    Key.MediaPlay -> { exo.play(); true }
                    Key.MediaPause -> { exo.pause(); true }
                    Key.DirectionRight, Key.MediaFastForward -> { seekBy(10_000L); true }
                    Key.DirectionLeft, Key.MediaRewind -> { seekBy(-10_000L); true }
                    Key.DirectionDown, Key.MediaNext -> { skip(true); true }
                    Key.DirectionUp, Key.MediaPrevious -> { skip(false); true }
                    else -> false
                }
            },
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverArt(meta?.cover, playing)
            Spacer(Modifier.width(44.dp))
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                Pill("در حال پخش  •  ${index + 1} از ${paths.size}", Pal.Music)
                Spacer(Modifier.height(14.dp))
                T(meta?.title ?: File(paths[index.coerceIn(0, paths.lastIndex)]).nameWithoutExtension,
                    size = 32, weight = FontWeight.Bold, maxLines = 2, auto = true,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                val sub = listOfNotNull(meta?.artist, meta?.album).joinToString("  •  ")
                T(if (error != null) error!! else sub.ifEmpty { "هنرمند نامشخص" },
                    size = 18, color = if (error != null) Pal.Video else Pal.Muted, auto = true,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(22.dp))
                Equalizer(playing, Modifier.fillMaxWidth().height(64.dp))
                Spacer(Modifier.height(18.dp))
                TimeLine(pos, dur, Pal.Music, Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.SkipPrevious, null, tint = Pal.Text, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(22.dp))
                        Icon(Icons.Rounded.Replay10, null, tint = Pal.Text, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.width(22.dp))
                        Box(
                            Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Pal.Music, Pal.Accent2))),
                            Alignment.Center,
                        ) {
                            Icon(
                                if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                null, tint = Color.White, modifier = Modifier.size(38.dp),
                            )
                        }
                        Spacer(Modifier.width(22.dp))
                        Icon(Icons.Rounded.Forward10, null, tint = Pal.Text, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.width(22.dp))
                        Icon(Icons.Rounded.SkipNext, null, tint = Pal.Text, modifier = Modifier.size(36.dp))
                    }
                }
                Spacer(Modifier.height(18.dp))
                HintBar(
                    listOf(
                        "OK" to "پخش / توقف",
                        "← →" to "۱۰ ثانیه",
                        "↑ ↓" to "قبلی / بعدی",
                    )
                )
            }
        }
    }
}

@Composable
private fun CoverArt(cover: ImageBitmap?, playing: Boolean) {
    val shape = RoundedCornerShape(36.dp)
    Box(
        Modifier
            .size(290.dp)
            .shadow(30.dp, shape, ambientColor = Pal.Music, spotColor = Pal.Music)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Pal.Music.copy(alpha = 0.9f), Pal.Accent2.copy(alpha = 0.7f))))
            .border(1.dp, Pal.Stroke, shape),
        Alignment.Center,
    ) {
        if (cover != null) {
            Image(
                bitmap = cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(130.dp))
        }
    }
}

@Composable
private fun Equalizer(playing: Boolean, modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "eq")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val brush = Brush.verticalGradient(listOf(Pal.Music, Pal.Accent2))
    Canvas(modifier) {
        val bars = 32
        val gap = 6.dp.toPx()
        val bw = (size.width - gap * (bars - 1)) / bars
        for (i in 0 until bars) {
            val k = 1 + i % 3
            val wave = abs(sin(phase * k + i * 0.7f))
            val level = if (playing) 0.18f + 0.82f * wave else 0.08f
            val h = size.height * level
            drawRoundRect(
                brush = brush,
                topLeft = Offset(i * (bw + gap), size.height - h),
                size = Size(bw, h),
                cornerRadius = CornerRadius(bw / 2f, bw / 2f),
            )
        }
    }
}
