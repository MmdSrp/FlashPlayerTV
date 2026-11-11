package ir.flashtv.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.flashtv.player.AppViewModel
import ir.flashtv.player.FileItem
import ir.flashtv.player.MediaKind
import ir.flashtv.player.Screen
import ir.flashtv.player.Storage
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed as listItemsIndexed

@Composable
fun CategoryScreen(vm: AppViewModel, kind: MediaKind) {
    val scan = vm.scan
    val items: List<FileItem>? = when (kind) {
        MediaKind.VIDEO -> scan?.videos
        MediaKind.AUDIO -> scan?.audios
        else -> scan?.images
    }
    val (title, icon, accent) = when (kind) {
        MediaKind.VIDEO -> Triple("ویدیوها", Icons.Rounded.VideoLibrary, Pal.Video)
        MediaKind.AUDIO -> Triple("موسیقی", Icons.Rounded.LibraryMusic, Pal.Music)
        else -> Triple("عکس‌ها", Icons.Rounded.PhotoLibrary, Pal.Photo)
    }
    val paths = remember(items) { items?.map { it.file.path } ?: emptyList() }
    val key = "cat:$kind"
    val focus = rememberAutoFocus(key = items?.size ?: -1, delayMs = 220)

    fun open(i: Int) {
        vm.push(
            when (kind) {
                MediaKind.VIDEO -> Screen.VideoPlayer(paths, i)
                MediaKind.AUDIO -> Screen.MusicPlayer(paths, i)
                else -> Screen.PhotoViewer(paths, i)
            }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp)
            .padding(top = 24.dp, bottom = 6.dp)
    ) {
        Header(
            title = title,
            icon = icon,
            accent = accent,
            subtitle = items?.let { "${it.size} مورد در این فلش" } ?: "در حال اسکن…",
            trailing = { items?.let { Pill("${it.size}", accent) } },
        )

        when {
            items == null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                T(
                    if (vm.scanning) "در حال اسکن فلش… ${vm.scanned} فایل" else "اطلاعاتی برای نمایش نیست",
                    size = 18, color = Pal.Muted,
                )
            }
            items.isEmpty() -> EmptyState(
                icon, "موردی پیدا نشد",
                "در این فلش فایلی از این نوع وجود ندارد.", accent,
            )
            kind == MediaKind.AUDIO -> {
                val start = (vm.focusMemory[key] ?: 0).coerceIn(0, items.lastIndex)
                LazyColumn(
                    state = rememberLazyListState((start - 2).coerceAtLeast(0)),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                ) {
                    listItemsIndexed(items, key = { _, it -> it.file.path }) { i, item ->
                        SongRow(
                            i, item, if (i == start) focus else null,
                            onFocused = { vm.focusMemory[key] = i },
                        ) { open(i) }
                    }
                }
            }
            else -> {
                val cols = if (kind == MediaKind.IMAGE) 5 else 4
                val start = (vm.focusMemory[key] ?: 0).coerceIn(0, items.lastIndex)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(cols),
                    state = rememberLazyGridState((start / cols) * cols),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(10.dp),
                ) {
                    gridItemsIndexed(items, key = { _, it -> it.file.path }) { i, item ->
                        MediaCard(
                            item, kind, accent, if (i == start) focus else null,
                            onFocused = { vm.focusMemory[key] = i },
                        ) { open(i) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaCard(
    item: FileItem,
    kind: MediaKind,
    accent: Color,
    requester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    val isImage = kind == MediaKind.IMAGE
    FocusBox(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        accent = accent,
        focusRequester = requester,
        onFocused = onFocused,
    ) {
        Column(Modifier.padding(8.dp)) {
            Box {
                Thumb(
                    item,
                    Modifier.fillMaxWidth().aspectRatio(if (isImage) 1f else 16f / 9f),
                    iconSize = 36.dp, pxW = 400, pxH = 300,
                    shape = RoundedCornerShape(10.dp),
                )
                if (kind == MediaKind.VIDEO) {
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                        Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                }
                Pill(
                    item.extension.uppercase(), accent,
                    Modifier.align(Alignment.TopStart).padding(6.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            if (isImage) {
                T(item.file.nameWithoutExtension, size = 13, auto = true,
                    modifier = Modifier.fillMaxWidth().height(20.dp))
            } else {
                T(item.file.nameWithoutExtension, size = 14, maxLines = 2, auto = true,
                    modifier = Modifier.fillMaxWidth().height(40.dp))
                T(Storage.formatSize(item.size), size = 12, color = Pal.Muted)
            }
        }
    }
}

@Composable
private fun SongRow(
    index: Int,
    item: FileItem,
    requester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    FocusBox(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(70.dp),
        shape = RoundedCornerShape(16.dp),
        accent = Pal.Music,
        focusScale = 1.02f,
        focusRequester = requester,
        onFocused = onFocused,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            T("${index + 1}", size = 14, color = Pal.Muted, align = TextAlign.Center,
                modifier = Modifier.width(44.dp))
            Thumb(item, Modifier.size(48.dp), iconSize = 26.dp, shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                T(item.file.nameWithoutExtension, size = 17, weight = FontWeight.Medium, auto = true)
                T(item.file.parentFile?.name ?: "", size = 13, color = Pal.Muted, auto = true)
            }
            Spacer(Modifier.width(12.dp))
            T(Storage.formatSize(item.size), size = 13, color = Pal.Muted)
            Spacer(Modifier.width(12.dp))
            Pill(item.extension.uppercase(), Pal.Music)
        }
    }
}
