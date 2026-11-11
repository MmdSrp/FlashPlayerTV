package ir.flashtv.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ir.flashtv.player.AppViewModel
import ir.flashtv.player.MediaKind
import ir.flashtv.player.Screen

/** Wraps latin text (paths, names) so it keeps its direction inside the RTL layout. */
fun ltr(s: String): String = "‪$s‬"

@Composable
fun FlashRoot(vm: AppViewModel, onExit: () -> Unit) {
    FlashTheme {
        BackHandler { if (!vm.pop()) onExit() }
        val screen = vm.current
        val immersive = screen is Screen.VideoPlayer || screen is Screen.PhotoViewer

        AppBackground {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    AnimatedContent(
                        targetState = screen,
                        transitionSpec = {
                            (fadeIn(tween(220)) togetherWith fadeOut(tween(120))).using(null)
                        },
                        label = "screens",
                    ) { s -> ScreenHost(vm, s) }
                }
                if (!immersive) Footer()
            }
        }
    }
}

@Composable
private fun ScreenHost(vm: AppViewModel, s: Screen) {
    when (s) {
        Screen.Drives -> DrivesScreen(vm)
        Screen.Dashboard -> DashboardScreen(vm)
        Screen.Videos -> CategoryScreen(vm, MediaKind.VIDEO)
        Screen.Music -> CategoryScreen(vm, MediaKind.AUDIO)
        Screen.Photos -> CategoryScreen(vm, MediaKind.IMAGE)
        is Screen.Explorer -> ExplorerScreen(vm, s.path)
        is Screen.PhotoViewer -> PhotoViewerScreen(s.paths, s.index)
        is Screen.VideoPlayer -> VideoPlayerScreen(s.paths, s.index)
        is Screen.MusicPlayer -> MusicPlayerScreen(s.paths, s.index)
    }
}

/** Slim strip at the very bottom of every normal screen. */
@Composable
fun Footer() {
    Row(
        Modifier
            .fillMaxWidth()
            .height(30.dp)
            .padding(horizontal = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        T("Flash Player  •  v1.0", size = 11, color = Pal.Muted.copy(alpha = 0.6f))
        Spacer(Modifier.weight(1f))
        Credit()
    }
}

@Composable
fun Credit(modifier: Modifier = Modifier) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(Pal.Accent2)
            )
            Spacer(Modifier.width(7.dp))
            T(
                "Created By Mohammad Shaerpour",
                size = 11,
                color = Pal.Muted,
                weight = FontWeight.Medium,
            )
        }
    }
}
