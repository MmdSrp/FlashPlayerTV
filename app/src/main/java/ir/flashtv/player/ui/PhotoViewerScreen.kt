package ir.flashtv.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun PhotoViewerScreen(paths: List<String>, startIndex: Int) {
    if (paths.isEmpty()) return
    val ctx = LocalContext.current
    val n = paths.size
    var index by remember { mutableIntStateOf(startIndex.coerceIn(0, n - 1)) }
    var slideshow by remember { mutableStateOf(false) }
    var overlay by remember { mutableStateOf(true) }
    var tick by remember { mutableIntStateOf(0) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        runCatching { focus.requestFocus() }
    }

    fun go(d: Int) {
        index = (index + d + n) % n
        overlay = true
        tick++
    }

    // Auto-hide the overlay.
    LaunchedEffect(tick, overlay) {
        if (overlay) {
            delay(3500)
            overlay = false
        }
    }
    // Slideshow timer.
    LaunchedEffect(slideshow, index) {
        if (slideshow) {
            delay(5000)
            index = (index + 1) % n
        }
    }
    // Pre-load the neighbours so next / previous feels instant.
    LaunchedEffect(index) {
        if (n > 1) {
            for (d in intArrayOf(1, -1)) {
                val p = paths[(index + d + n) % n]
                ctx.imageLoader.enqueue(
                    ImageRequest.Builder(ctx).data(File(p)).size(1920, 1080).build()
                )
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionRight, Key.MediaNext, Key.PageDown -> { go(1); true }
                    Key.DirectionLeft, Key.MediaPrevious, Key.PageUp -> { go(-1); true }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter,
                    Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> {
                        slideshow = !slideshow
                        overlay = true
                        tick++
                        true
                    }
                    Key.DirectionUp, Key.DirectionDown -> {
                        overlay = !overlay
                        tick++
                        true
                    }
                    else -> false
                }
            },
    ) {
        AnimatedContent(
            targetState = index,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
            label = "photo",
        ) { i ->
            AsyncImage(
                model = ImageRequest.Builder(ctx)
                    .data(File(paths[i]))
                    .size(1920, 1080)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
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
                T(File(paths[index]).name, size = 20, weight = FontWeight.Bold, auto = true,
                    modifier = Modifier.weight(1f))
                Spacer(Modifier.width(16.dp))
                if (slideshow) {
                    Pill("اسلایدشو فعال", Pal.Photo)
                    Spacer(Modifier.width(10.dp))
                }
                Pill("${index + 1} / $n", Pal.Accent2)
            }
        }

        AnimatedVisibility(
            visible = overlay,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                    .padding(horizontal = 40.dp, vertical = 16.dp),
            ) {
                HintBar(
                    listOf(
                        "← →" to "قبلی / بعدی",
                        "OK" to "اسلایدشو",
                        "↑ ↓" to "نمایش اطلاعات",
                        "Back" to "خروج",
                    ),
                    Modifier.align(Alignment.CenterStart),
                )
            }
        }

        // The credit stays visible at the bottom corner, even when the overlay is hidden.
        Credit(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(horizontal = 40.dp, vertical = 16.dp)
        )
    }
}
