@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package ir.flashtv.player.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun VideoPlayerScreen(paths: List<String>, startIndex: Int) {
    if (paths.isEmpty()) return
    val ctx = LocalContext.current
    KeepScreenOn()
    val safeStart = startIndex.coerceIn(0, paths.lastIndex)

    val exo = remember {
        val factory = DefaultRenderersFactory(ctx).setEnableDecoderFallback(true)
        ExoPlayer.Builder(ctx, factory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            .build()
            .apply {
                setMediaItems(paths.map { MediaItem.fromUri(Uri.fromFile(File(it))) }, safeStart, 0L)
                prepare()
                playWhenReady = true
            }
    }

    var index by remember { mutableIntStateOf(safeStart) }
    var playing by remember { mutableStateOf(true) }
    var buffering by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var ended by remember { mutableStateOf(false) }
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var overlay by remember { mutableStateOf(true) }
    var tick by remember { mutableIntStateOf(0) }
    var toastText by remember { mutableStateOf("") }
    var toastTick by remember { mutableIntStateOf(0) }
    val focus = remember { FocusRequester() }

    DisposableEffect(exo) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }

            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                ended = state == Player.STATE_ENDED
                if (state == Player.STATE_READY) dur = exo.duration.coerceAtLeast(0L)
                if (state == Player.STATE_ENDED) overlay = true
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                index = exo.currentMediaItemIndex
                error = null
                ended = false
                dur = exo.duration.coerceAtLeast(0L)
            }

            override fun onPlayerError(e: PlaybackException) {
                error = "این ویدیو پخش نشد (فرمت یا کدک پشتیبانی نمی‌شود)"
                overlay = true
            }
        }
        exo.addListener(listener)
        onDispose {
            exo.removeListener(listener)
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
            delay(250)
        }
    }
    LaunchedEffect(tick, overlay, playing) {
        if (overlay && playing) {
            delay(4000)
            overlay = false
        }
    }
    LaunchedEffect(toastTick) {
        if (toastTick > 0) {
            delay(900)
            toastText = ""
        }
    }
    // Broken file: skip to the next one automatically.
    LaunchedEffect(error) {
        if (error != null && exo.hasNextMediaItem()) {
            delay(2500)
            exo.seekToNextMediaItem()
            exo.prepare()
            exo.play()
        }
    }

    fun showToast(t: String) {
        toastText = t
        toastTick++
    }

    fun seekBy(ms: Long) {
        val target = (exo.currentPosition + ms).coerceAtLeast(0L)
        val limited = if (exo.duration > 0) target.coerceAtMost(exo.duration) else target
        exo.seekTo(limited)
        pos = limited
        overlay = true
        tick++
        val sec = kotlin.math.abs(ms) / 1000
        showToast(if (ms > 0) "+ $sec ثانیه" else "− $sec ثانیه")
    }

    fun togglePlay() {
        if (ended) {
            exo.seekTo(0)
            exo.play()
        } else if (exo.isPlaying) exo.pause() else exo.play()
        overlay = true
        tick++
    }

    fun skip(next: Boolean) {
        if (next && exo.hasNextMediaItem()) {
            exo.seekToNextMediaItem(); exo.prepare(); exo.play()
        } else if (!next && exo.hasPreviousMediaItem()) {
            exo.seekToPreviousMediaItem(); exo.prepare(); exo.play()
        } else {
            showToast(if (next) "آخرین ویدیو" else "اولین ویدیو")
        }
        overlay = true
        tick++
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val held = e.nativeKeyEvent.repeatCount
                when (e.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.MediaPlayPause -> { togglePlay(); true }
                    Key.MediaPlay -> { exo.play(); true }
                    Key.MediaPause -> { exo.pause(); overlay = true; true }
                    Key.DirectionRight -> { seekBy(if (held > 6) 60_000L else 10_000L); true }
                    Key.DirectionLeft -> { seekBy(if (held > 6) -60_000L else -10_000L); true }
                    Key.MediaFastForward -> { seekBy(30_000L); true }
                    Key.MediaRewind -> { seekBy(-30_000L); true }
                    Key.MediaNext -> { skip(true); true }
                    Key.MediaPrevious -> { skip(false); true }
                    Key.DirectionUp, Key.DirectionDown -> { overlay = !overlay; tick++; true }
                    else -> false
                }
            },
    ) {
        AndroidView(
            factory = { c ->
                PlayerView(c).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    isFocusable = false
                    keepScreenOn = true
                    this.player = exo
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (buffering && error == null) {
            CircularProgressIndicator(
                Modifier.align(Alignment.Center).size(56.dp),
                color = Pal.Accent2,
                strokeWidth = 5.dp,
            )
        }

        error?.let { msg ->
            Column(
                Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 36.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Rounded.ErrorOutline, null, tint = Pal.Video, modifier = Modifier.size(52.dp))
                Spacer(Modifier.height(10.dp))
                T(msg, size = 18, weight = FontWeight.Medium, align = TextAlign.Center, maxLines = 2)
                if (exo.hasNextMediaItem()) {
                    Spacer(Modifier.height(6.dp))
                    T("در حال رفتن به ویدیوی بعدی…", size = 14, color = Pal.Muted)
                }
            }
        }

        if (toastText.isNotEmpty()) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 28.dp, vertical = 12.dp),
            ) { T(toastText, size = 22, weight = FontWeight.Bold) }
        }

        AnimatedVisibility(
            visible = overlay,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)))
                    .padding(horizontal = 40.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                T(File(paths[index.coerceIn(0, paths.lastIndex)]).nameWithoutExtension,
                    size = 22, weight = FontWeight.Bold, auto = true, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(16.dp))
                Pill("${index + 1} / ${paths.size}", Pal.Video)
            }
        }

        AnimatedVisibility(
            visible = overlay,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))))
                    .padding(start = 40.dp, end = 40.dp, top = 34.dp, bottom = 14.dp),
            ) {
                // Transport icons: always laid out left-to-right like a real player.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.width(22.dp))
                        Icon(Icons.Rounded.Replay10, null, tint = Color.White, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.width(22.dp))
                        Box(
                            Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Pal.Video, Pal.Accent))),
                            Alignment.Center,
                        ) {
                            Icon(
                                if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                null, tint = Color.White, modifier = Modifier.size(36.dp),
                            )
                        }
                        Spacer(Modifier.width(22.dp))
                        Icon(Icons.Rounded.Forward10, null, tint = Color.White, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.width(22.dp))
                        Icon(Icons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.size(34.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                TimeLine(pos, dur, Pal.Video, Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    HintBar(
                        listOf(
                            "OK" to "پخش / توقف",
                            "← →" to "۱۰ ثانیه",
                            "↑ ↓" to "نمایش نوار",
                            "Back" to "خروج",
                        )
                    )
                    Spacer(Modifier.weight(1f))
                    Credit()
                }
            }
        }
    }
}
