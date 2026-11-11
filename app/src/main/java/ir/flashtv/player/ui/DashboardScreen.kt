package ir.flashtv.player.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.flashtv.player.AppViewModel
import ir.flashtv.player.Screen
import ir.flashtv.player.Storage

@Composable
fun DashboardScreen(vm: AppViewModel) {
    val drive = vm.drive
    val scan = vm.scan
    val start = vm.focusMemory["dash"] ?: 0
    val focus = rememberAutoFocus()
    fun req(i: Int): FocusRequester? = if (i == start) focus else null

    fun countText(n: Int?, unit: String): String = when {
        n != null -> "$n $unit"
        vm.scanning -> "در حال اسکن…"
        else -> "—"
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp)
            .padding(top = 24.dp, bottom = 6.dp)
    ) {
        Header(
            title = drive?.name ?: "فلش",
            icon = Icons.Rounded.Usb,
            accent = Pal.Accent,
            subtitle = when {
                vm.scanning -> "در حال اسکن… ${vm.scanned} فایل پیدا شد"
                vm.scanFailed -> "خواندن فلش با مشکل مواجه شد"
                scan != null -> "اسکن کامل شد"
                else -> ""
            },
            trailing = {
                if (drive != null && drive.total > 0) {
                    Column(Modifier.width(250.dp)) {
                        StorageBar(drive.usedFraction, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                        T(
                            "${Storage.formatSize(drive.free)} آزاد از ${Storage.formatSize(drive.total)}",
                            size = 13, color = Pal.Muted,
                        )
                    }
                }
            },
        )

        Row(
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            CategoryTile(
                Modifier.weight(1f).fillMaxHeight(),
                "ویدیوها", countText(scan?.videos?.size, "ویدیو"), "فیلم و کلیپ",
                Icons.Rounded.VideoLibrary, Pal.Video, req(0),
                onFocused = { vm.focusMemory["dash"] = 0 },
            ) { vm.push(Screen.Videos) }
            CategoryTile(
                Modifier.weight(1f).fillMaxHeight(),
                "موسیقی", countText(scan?.audios?.size, "آهنگ"), "آهنگ و پادکست",
                Icons.Rounded.LibraryMusic, Pal.Music, req(1),
                onFocused = { vm.focusMemory["dash"] = 1 },
            ) { vm.push(Screen.Music) }
            CategoryTile(
                Modifier.weight(1f).fillMaxHeight(),
                "عکس‌ها", countText(scan?.images?.size, "تصویر"), "اسلایدشو و نمایش",
                Icons.Rounded.PhotoLibrary, Pal.Photo, req(2),
                onFocused = { vm.focusMemory["dash"] = 2 },
            ) { vm.push(Screen.Photos) }
            CategoryTile(
                Modifier.weight(1f).fillMaxHeight(),
                "همه فایل‌ها", "مرور پوشه‌ها", "مدیریت فایل",
                Icons.Rounded.FolderOpen, Pal.Files, req(3),
                onFocused = { vm.focusMemory["dash"] = 3 },
            ) { drive?.let { vm.push(Screen.Explorer(it.path)) } }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ActionButton("اسکن مجدد", Icons.Rounded.Refresh, req(4),
                onFocused = { vm.focusMemory["dash"] = 4 }) { vm.startScan() }
            ActionButton("تعویض فلش", Icons.Rounded.SwapHoriz, req(5),
                onFocused = { vm.focusMemory["dash"] = 5 }) { vm.pop() }
        }
    }
}

@Composable
private fun CategoryTile(
    modifier: Modifier,
    title: String,
    count: String,
    hint: String,
    icon: ImageVector,
    accent: Color,
    requester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    FocusBox(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        accent = accent,
        focusScale = 1.06f,
        focusRequester = requester,
        onFocused = onFocused,
    ) { focused ->
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = if (focused) 0.38f else 0.20f), Color.Transparent)
                    )
                )
        ) {
            Column(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.6f)))),
                    Alignment.Center,
                ) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(42.dp))
                }
                Spacer(Modifier.height(14.dp))
                T(title, size = 23, weight = FontWeight.Bold, align = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                T(count, size = 16, color = accent, weight = FontWeight.Medium, align = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
                T(hint, size = 13, color = Pal.Muted, align = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: ImageVector,
    requester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    FocusBox(
        onClick = onClick,
        modifier = Modifier.height(50.dp).width(190.dp),
        shape = RoundedCornerShape(25.dp),
        focusRequester = requester,
        onFocused = onFocused,
    ) {
        Row(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterVertically) {
            Icon(icon, null, tint = Pal.Text, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            T(label, size = 16, weight = FontWeight.Medium)
        }
    }
}
