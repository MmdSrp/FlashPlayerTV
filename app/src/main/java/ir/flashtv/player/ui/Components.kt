package ir.flashtv.player.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFramePercent
import ir.flashtv.player.FileItem
import ir.flashtv.player.MediaKind
import kotlinx.coroutines.delay

// ------------------------------------------------------------------- text

@Composable
fun T(
    text: String,
    modifier: Modifier = Modifier,
    size: Int = 16,
    color: Color = Pal.Text,
    weight: FontWeight = FontWeight.Normal,
    maxLines: Int = 1,
    align: TextAlign = TextAlign.Start,
    auto: Boolean = false,
) {
    Text(
        text = text,
        modifier = modifier,
        style = if (auto) LocalTextStyle.current.copy(textDirection = TextDirection.Content)
        else LocalTextStyle.current,
        color = color,
        fontSize = size.sp,
        fontWeight = weight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = align,
        lineHeight = (size * 1.35f).sp,
    )
}

// ------------------------------------------------------------- background

@Composable
fun AppBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Pal.Bg2, Pal.Bg)))
            .drawBehind {
                val c1 = Offset(size.width * 0.85f, size.height * 0.05f)
                val r1 = size.width * 0.5f
                drawCircle(
                    Brush.radialGradient(
                        listOf(Pal.Accent.copy(alpha = 0.18f), Color.Transparent),
                        center = c1, radius = r1,
                    ),
                    radius = r1, center = c1,
                )
                val c2 = Offset(size.width * 0.08f, size.height * 0.98f)
                val r2 = size.width * 0.42f
                drawCircle(
                    Brush.radialGradient(
                        listOf(Pal.Accent2.copy(alpha = 0.11f), Color.Transparent),
                        center = c2, radius = r2,
                    ),
                    radius = r2, center = c2,
                )
            },
        content = content,
    )
}

// ------------------------------------------------------------------ focus

@Composable
fun rememberAutoFocus(key: Any? = Unit, delayMs: Long = 120): FocusRequester {
    val requester = remember { FocusRequester() }
    LaunchedEffect(key) {
        delay(delayMs)
        runCatching { requester.requestFocus() }
    }
    return requester
}

/** A focusable card that grows, glows and gets an accent border when focused. */
@Composable
fun FocusBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    accent: Color = Pal.Accent,
    focusScale: Float = 1.04f,
    background: Color = Pal.Card,
    focusedBackground: Color = Pal.CardHi,
    focusRequester: FocusRequester? = null,
    onFocused: (() -> Unit)? = null,
    content: @Composable BoxScope.(Boolean) -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) focusScale else 1f, tween(140), label = "focusScale")
    LaunchedEffect(focused) { if (focused) onFocused?.invoke() }

    Box(
        modifier
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                elevation = if (focused) 18.dp else 0.dp,
                shape = shape,
                ambientColor = accent,
                spotColor = accent,
            )
            .clip(shape)
            .background(if (focused) focusedBackground else background)
            .border(
                width = if (focused) 2.5.dp else 1.dp,
                color = if (focused) accent else Pal.Stroke,
                shape = shape,
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
    ) {
        content(focused)
    }
}

// ------------------------------------------------------------- file icons

fun kindIcon(kind: MediaKind, ext: String = ""): ImageVector = when (kind) {
    MediaKind.FOLDER -> Icons.Rounded.Folder
    MediaKind.VIDEO -> Icons.Rounded.Movie
    MediaKind.AUDIO -> Icons.Rounded.MusicNote
    MediaKind.IMAGE -> Icons.Rounded.Image
    MediaKind.OTHER -> when (ext) {
        "zip", "rar", "7z", "tar", "gz" -> Icons.Rounded.Archive
        "pdf" -> Icons.Rounded.PictureAsPdf
        "apk" -> Icons.Rounded.Android
        "txt", "doc", "docx", "srt", "log", "xls", "xlsx" -> Icons.Rounded.Description
        else -> Icons.Rounded.InsertDriveFile
    }
}

fun kindColor(kind: MediaKind): Color = when (kind) {
    MediaKind.FOLDER -> Pal.Files
    MediaKind.VIDEO -> Pal.Video
    MediaKind.AUDIO -> Pal.Music
    MediaKind.IMAGE -> Pal.Photo
    MediaKind.OTHER -> Pal.Muted
}

fun kindLabel(kind: MediaKind): String = when (kind) {
    MediaKind.FOLDER -> "پوشه"
    MediaKind.VIDEO -> "ویدیو"
    MediaKind.AUDIO -> "موسیقی"
    MediaKind.IMAGE -> "تصویر"
    MediaKind.OTHER -> "فایل"
}

/**
 * Square-ish thumbnail: tinted icon underneath, real image / video frame on top
 * once Coil has decoded it.
 */
@Composable
fun Thumb(
    item: FileItem,
    modifier: Modifier = Modifier,
    iconSize: Dp = 30.dp,
    pxW: Int = 360,
    pxH: Int = 270,
    shape: Shape = RoundedCornerShape(12.dp),
) {
    val color = kindColor(item.kind)
    val ctx = LocalContext.current
    Box(
        modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0.10f)))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(kindIcon(item.kind, item.extension), null, tint = color, modifier = Modifier.size(iconSize))
        if (item.kind == MediaKind.IMAGE || item.kind == MediaKind.VIDEO) {
            val request = remember(item.file.path) {
                ImageRequest.Builder(ctx)
                    .data(item.file)
                    .size(pxW, pxH)
                    .apply { if (item.kind == MediaKind.VIDEO) videoFramePercent(0.1) }
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// ------------------------------------------------------------- small bits

@Composable
fun Header(
    title: String,
    icon: ImageVector,
    accent: Color,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.55f)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            T(title, size = 28, weight = FontWeight.Bold, auto = true)
            if (subtitle != null) T(subtitle, size = 14, color = Pal.Muted)
        }
        trailing()
    }
}

@Composable
fun Pill(text: String, color: Color = Pal.Muted, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        T(text, size = 13, color = color, weight = FontWeight.Medium)
    }
}

/** Bottom key-hint strip: list of (key, action). */
@Composable
fun HintBar(hints: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        hints.forEach { (key, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Pal.Card)
                        .border(1.dp, Pal.Stroke, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) { T(key, size = 12, color = Pal.Text, weight = FontWeight.Medium) }
                Spacer(Modifier.width(8.dp))
                T(label, size = 13, color = Pal.Muted)
            }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, accent: Color = Pal.Accent) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = accent, modifier = Modifier.size(52.dp)) }
        Spacer(Modifier.size(18.dp))
        T(title, size = 24, weight = FontWeight.Bold, align = TextAlign.Center)
        Spacer(Modifier.size(6.dp))
        T(message, size = 15, color = Pal.Muted, align = TextAlign.Center, maxLines = 3,
            modifier = Modifier.width(420.dp))
    }
}
