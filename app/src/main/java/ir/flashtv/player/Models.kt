package ir.flashtv.player

import java.io.File

enum class MediaKind { FOLDER, VIDEO, AUDIO, IMAGE, OTHER }

data class DriveInfo(
    val name: String,
    val path: String,
    val total: Long,
    val free: Long,
) {
    val used: Long get() = (total - free).coerceAtLeast(0)
    val usedFraction: Float
        get() = if (total > 0) (used.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f) else 0f
}

data class FileItem(
    val file: File,
    val kind: MediaKind,
    val size: Long,
    val modified: Long,
    /** Number of entries inside a folder, -1 when unknown. */
    val childCount: Int = -1,
) {
    val name: String get() = file.name
    val isDir: Boolean get() = kind == MediaKind.FOLDER
    val extension: String get() = file.extension.lowercase()
}

data class ScanResult(
    val videos: List<FileItem>,
    val audios: List<FileItem>,
    val images: List<FileItem>,
)

sealed interface Screen {
    data object Drives : Screen
    data object Dashboard : Screen
    data object Videos : Screen
    data object Music : Screen
    data object Photos : Screen
    data class Explorer(val path: String) : Screen
    data class PhotoViewer(val paths: List<String>, val index: Int) : Screen
    data class VideoPlayer(val paths: List<String>, val index: Int) : Screen
    data class MusicPlayer(val paths: List<String>, val index: Int) : Screen
}
