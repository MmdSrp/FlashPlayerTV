package ir.flashtv.player.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.material3.LocalContentColor

/** Colour palette of the whole app (dark, violet / cyan accents). */
object Pal {
    val Bg = Color(0xFF0A0C12)
    val Bg2 = Color(0xFF111522)
    val Surface = Color(0xFF161A26)
    val Card = Color(0xFF1B2030)
    val CardHi = Color(0xFF2A3150)
    val Stroke = Color(0xFF2B3149)
    val Text = Color(0xFFF2F4FA)
    val Muted = Color(0xFF9AA3BD)
    val Accent = Color(0xFF7C8CFF)
    val Accent2 = Color(0xFF22D3EE)

    val Video = Color(0xFFFF5C7A)
    val Music = Color(0xFFA78BFA)
    val Photo = Color(0xFF34D399)
    val Files = Color(0xFFFBBF24)
}

/**
 * The UI is designed for a 960 x 540 dp viewport (a 1080p / 4K TV) and is laid out
 * right-to-left because the interface language is Persian.
 */
@Composable
fun FlashTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Pal.Accent,
            background = Pal.Bg,
            surface = Pal.Surface,
            onSurface = Pal.Text,
            onBackground = Pal.Text,
        )
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
            LocalContentColor provides Pal.Text,
        ) {
            content()
        }
    }
}
