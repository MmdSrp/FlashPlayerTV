package ir.flashtv.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ir.flashtv.player.Storage

/** Keeps the TV from dimming / sleeping while a player screen is open. */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

/** Progress bar that always fills left-to-right (a timeline is not mirrored in RTL). */
@Composable
fun SeekBar(
    pos: Long,
    dur: Long,
    accent: Color,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
) {
    val f = if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier
                .height(height)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.22f))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(f)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(accent, Pal.Accent2)))
            )
        }
    }
}

@Composable
fun TimeLine(pos: Long, dur: Long, accent: Color, modifier: Modifier = Modifier) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            T(Storage.formatTime(pos), size = 14, weight = FontWeight.Medium,
                modifier = Modifier.width(66.dp))
            SeekBar(pos, dur, accent, Modifier.weight(1f))
            T(Storage.formatTime(dur), size = 14, color = Pal.Muted, align = TextAlign.End,
                modifier = Modifier.width(66.dp))
        }
    }
}
