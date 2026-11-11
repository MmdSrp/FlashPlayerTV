package ir.flashtv.player.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.flashtv.player.AppViewModel
import ir.flashtv.player.DriveInfo
import ir.flashtv.player.R
import ir.flashtv.player.Storage

@Composable
fun StorageBar(fraction: Float, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(Pal.Stroke)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0.03f, 1f))
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(Pal.Accent, Pal.Accent2)))
        )
    }
}

@Composable
fun DrivesScreen(vm: AppViewModel) {
    val drives = vm.drives
    val focus = rememberAutoFocus(key = drives.size to vm.loadingDrives)

    Row(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 24.dp)
    ) {
        Brand(Modifier.width(300.dp).fillMaxHeight())
        Spacer(Modifier.width(36.dp))
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Header(
                title = "انتخاب حافظه",
                icon = Icons.Rounded.Usb,
                accent = Pal.Accent,
                subtitle = when {
                    vm.loadingDrives -> "در حال جستجو…"
                    drives.isEmpty() -> "حافظه‌ای متصل نیست"
                    else -> "${drives.size} حافظه شناسایی شد"
                },
                trailing = {
                    FocusBox(
                        onClick = { vm.refreshDrives() },
                        modifier = Modifier.height(44.dp).width(140.dp),
                        shape = RoundedCornerShape(22.dp),
                        focusRequester = if (drives.isEmpty()) focus else null,
                    ) {
                        Row(
                            Modifier.fillMaxSize(),
                            Arrangement.Center,
                            Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Refresh, null, Modifier.size(20.dp), tint = Pal.Text)
                            Spacer(Modifier.width(8.dp))
                            T("بروزرسانی", size = 15, weight = FontWeight.Medium)
                        }
                    }
                },
            )
            when {
                vm.loadingDrives -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    T("در حال جستجوی حافظه‌ها…", size = 18, color = Pal.Muted)
                }
                !vm.hasPermission -> EmptyState(
                    Icons.Rounded.Lock,
                    "دسترسی لازم است",
                    "برای خواندن فلش، مجوز دسترسی به حافظه را در تنظیمات برنامه فعال کنید.",
                    Pal.Video,
                )
                drives.isEmpty() -> EmptyState(
                    Icons.Rounded.Usb,
                    "فلشی پیدا نشد",
                    "فلش را به پورت USB تلویزیون وصل کنید؛ به‌محض اتصال، خودکار شناسایی می‌شود.",
                )
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                ) {
                    itemsIndexed(drives, key = { _, d -> d.path }) { i, d ->
                        DriveCard(d, if (i == 0) focus else null) { vm.selectDrive(d) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Brand(modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.Center) {
        Image(
            painterResource(R.mipmap.ic_launcher),
            contentDescription = null,
            modifier = Modifier.size(104.dp).clip(RoundedCornerShape(26.dp)),
        )
        Spacer(Modifier.height(22.dp))
        T("Flash Player", size = 38, weight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        T("پخش‌کننده‌ی فلش برای تلویزیون", size = 17, color = Pal.Accent2, weight = FontWeight.Medium)
        Spacer(Modifier.height(16.dp))
        T(
            "ویدیو، موسیقی و عکس‌های فلش خود را با ریموت، راحت و شیک مرور کنید.",
            size = 14, color = Pal.Muted, maxLines = 3,
        )
        Spacer(Modifier.height(26.dp))
        HintBar(listOf("OK" to "انتخاب", "Back" to "بازگشت"))
    }
}

@Composable
private fun DriveCard(d: DriveInfo, requester: FocusRequester?, onClick: () -> Unit) {
    FocusBox(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(116.dp),
        shape = RoundedCornerShape(20.dp),
        focusRequester = requester,
        focusScale = 1.03f,
    ) { focused ->
        Row(
            Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Pal.Accent, Pal.Accent2))),
                Alignment.Center,
            ) {
                Icon(Icons.Rounded.Usb, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                T(ltr(d.name), size = 22, weight = FontWeight.Bold)
                T(ltr(d.path), size = 13, color = Pal.Muted)
                Spacer(Modifier.height(10.dp))
                StorageBar(d.usedFraction, Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                T(
                    if (d.total > 0) "${Storage.formatSize(d.free)} آزاد از ${Storage.formatSize(d.total)}"
                    else "حجم نامشخص",
                    size = 13, color = Pal.Muted,
                )
            }
            Spacer(Modifier.width(16.dp))
            Icon(
                Icons.Rounded.ChevronLeft, null,
                tint = if (focused) Pal.Accent2 else Pal.Muted,
                modifier = Modifier.size(34.dp),
            )
        }
    }
}
