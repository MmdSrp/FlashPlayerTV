package ir.flashtv.player.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.flashtv.player.AppViewModel
import ir.flashtv.player.FileItem
import ir.flashtv.player.MediaKind
import ir.flashtv.player.Screen
import ir.flashtv.player.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ExplorerScreen(vm: AppViewModel, path: String) {
    val ctx = LocalContext.current
    val dir = remember(path) { File(path) }
    var items by remember(path) { mutableStateOf<List<FileItem>?>(null) }
    var focused by remember(path) { mutableStateOf<FileItem?>(null) }
    LaunchedEffect(path) {
        items = withContext(Dispatchers.IO) { Storage.listDir(dir) }
    }

    val key = "exp:$path"
    val focus = rememberAutoFocus(key = items?.size ?: -1, delayMs = 200)
    val root = vm.drive?.path ?: path
    val rel = path.removePrefix(root).ifEmpty { "/" }
    val title = if (path == root) (vm.drive?.name ?: dir.name) else dir.name

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp)
            .padding(top = 24.dp, bottom = 6.dp)
    ) {
        Header(
            title = title,
            icon = Icons.Rounded.FolderOpen,
            accent = Pal.Files,
            subtitle = ltr(rel),
            trailing = { items?.let { Pill("${it.size} مورد", Pal.Files) } },
        )

        val list = items
        when {
            list == null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                T("در حال خواندن پوشه…", size = 18, color = Pal.Muted)
            }
            list.isEmpty() -> EmptyState(
                Icons.Rounded.FolderOpen, "پوشه خالی است",
                "هیچ فایل یا پوشه‌ای در اینجا نیست.", Pal.Files,
            )
            else -> {
                val start = (vm.focusMemory[key] ?: 0).coerceIn(0, list.lastIndex)
                Row(Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        state = rememberLazyListState((start - 3).coerceAtLeast(0)),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    ) {
                        itemsIndexed(list, key = { _, it -> it.file.path }) { i, item ->
                            FileRow(
                                item, if (i == start) focus else null,
                                onFocused = { focused = item; vm.focusMemory[key] = i },
                            ) { openItem(vm, ctx, list, item) }
                        }
                    }
                    Spacer(Modifier.width(20.dp))
                    DetailPane(
                        focused,
                        Modifier.width(300.dp).fillMaxHeight().padding(vertical = 10.dp),
                    )
                }
            }
        }
    }
}

private fun openItem(vm: AppViewModel, ctx: Context, list: List<FileItem>, item: FileItem) {
    when (item.kind) {
        MediaKind.FOLDER -> vm.push(Screen.Explorer(item.file.path))
        MediaKind.IMAGE -> {
            val ps = list.filter { it.kind == MediaKind.IMAGE }.map { it.file.path }
            vm.push(Screen.PhotoViewer(ps, ps.indexOf(item.file.path).coerceAtLeast(0)))
        }
        MediaKind.VIDEO -> {
            val ps = list.filter { it.kind == MediaKind.VIDEO }.map { it.file.path }
            vm.push(Screen.VideoPlayer(ps, ps.indexOf(item.file.path).coerceAtLeast(0)))
        }
        MediaKind.AUDIO -> {
            val ps = list.filter { it.kind == MediaKind.AUDIO }.map { it.file.path }
            vm.push(Screen.MusicPlayer(ps, ps.indexOf(item.file.path).coerceAtLeast(0)))
        }
        MediaKind.OTHER ->
            Toast.makeText(ctx, "این نوع فایل قابل پخش نیست", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun FileRow(
    item: FileItem,
    requester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    val accent = kindColor(item.kind)
    val subtitle = when (item.kind) {
        MediaKind.FOLDER -> if (item.childCount >= 0) "پوشه • ${item.childCount} مورد" else "پوشه"
        else -> "${kindLabel(item.kind)} • ${item.extension.uppercase()}"
    }
    FocusBox(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(76.dp),
        shape = RoundedCornerShape(16.dp),
        accent = accent,
        focusScale = 1.02f,
        focusRequester = requester,
        onFocused = onFocused,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Thumb(
                item, Modifier.size(width = 88.dp, height = 56.dp),
                iconSize = if (item.isDir) 34.dp else 28.dp,
                pxW = 260, pxH = 170,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                T(item.name, size = 17, weight = FontWeight.Medium, auto = true)
                T(subtitle, size = 13, color = Pal.Muted)
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (!item.isDir) T(Storage.formatSize(item.size), size = 14, weight = FontWeight.Medium)
                T(Storage.formatDate(item.modified), size = 12, color = Pal.Muted)
            }
        }
    }
}

@Composable
private fun DetailPane(item: FileItem?, modifier: Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .clip(shape)
            .background(Pal.Surface)
            .border(1.dp, Pal.Stroke, shape)
            .padding(16.dp),
    ) {
        if (item == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                T("یک مورد را انتخاب کنید", size = 15, color = Pal.Muted)
            }
            return@Column
        }
        Thumb(
            item,
            Modifier.fillMaxWidth().aspectRatio(16f / 10f),
            iconSize = 64.dp, pxW = 640, pxH = 400,
            shape = RoundedCornerShape(16.dp),
        )
        Spacer(Modifier.height(14.dp))
        T(item.name, size = 19, weight = FontWeight.Bold, maxLines = 3, auto = true,
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Pill(kindLabel(item.kind), kindColor(item.kind))
        Spacer(Modifier.height(14.dp))
        InfoRow("نوع", if (item.isDir) "پوشه" else item.extension.uppercase().ifEmpty { "—" })
        if (item.isDir) {
            InfoRow("محتویات", if (item.childCount >= 0) "${item.childCount} مورد" else "—")
        } else {
            InfoRow("حجم", Storage.formatSize(item.size))
        }
        InfoRow("تغییر", Storage.formatDate(item.modified))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        T(label, size = 13, color = Pal.Muted)
        Spacer(Modifier.width(10.dp))
        T(value, size = 14, weight = FontWeight.Medium, modifier = Modifier.weight(1f),
            align = androidx.compose.ui.text.style.TextAlign.End)
    }
}
