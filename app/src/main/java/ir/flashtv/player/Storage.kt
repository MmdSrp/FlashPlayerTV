package ir.flashtv.player

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.util.Date
import java.util.Locale

object Storage {

    private val videoExt = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "ts", "mpg", "mpeg",
        "3gp", "m2ts", "mts", "vob", "divx", "ogv", "rmvb",
    )
    private val audioExt = setOf(
        "mp3", "flac", "wav", "ogg", "m4a", "aac", "wma", "opus", "amr", "mid", "midi", "oga",
    )
    private val imageExt = setOf("jpg", "jpeg", "png", "webp", "bmp", "gif", "heic", "heif")

    private val skipDirs = setOf(
        "android", "lost.dir", "system volume information", "\$recycle.bin", "found.000",
    )

    fun classify(file: File): MediaKind {
        if (file.isDirectory) return MediaKind.FOLDER
        return when (file.extension.lowercase()) {
            in videoExt -> MediaKind.VIDEO
            in audioExt -> MediaKind.AUDIO
            in imageExt -> MediaKind.IMAGE
            else -> MediaKind.OTHER
        }
    }

    // ---------------------------------------------------------------- drives

    fun getDrives(ctx: Context): List<DriveInfo> {
        val result = LinkedHashMap<String, DriveInfo>()

        runCatching {
            val sm = ctx.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            for (v in sm.storageVolumes) {
                if (v.isPrimary && !v.isRemovable) continue
                val state = v.state
                if (state != Environment.MEDIA_MOUNTED && state != Environment.MEDIA_MOUNTED_READ_ONLY) continue
                val dir = volumeDir(v) ?: continue
                if (!dir.exists()) continue
                val label = runCatching { v.getDescription(ctx) }.getOrNull()
                result[dir.absolutePath] = makeInfo(if (label.isNullOrBlank()) dir.name else label, dir)
            }
        }

        // Fallback for boxes that do not report USB volumes properly.
        File("/storage").listFiles()?.forEach { f ->
            if (!f.isDirectory) return@forEach
            if (f.name == "emulated" || f.name == "self" || f.name == "sdcard0") return@forEach
            if (result.containsKey(f.absolutePath)) return@forEach
            if (f.canRead()) result[f.absolutePath] = makeInfo(f.name, f)
        }
        // Older boxes mount USB drives under /mnt.
        for (base in listOf("/mnt/usb_storage", "/mnt/usb", "/mnt/sda", "/mnt/media_rw", "/mnt/usbhost")) {
            File(base).listFiles()?.forEach { f ->
                if (!f.isDirectory || !f.canRead()) return@forEach
                if (result.keys.any { it.endsWith("/" + f.name) }) return@forEach
                if (f.list().isNullOrEmpty()) return@forEach
                result[f.absolutePath] = makeInfo(f.name, f)
            }
        }
        return result.values.toList()
    }

    private fun volumeDir(v: StorageVolume): File? {
        if (Build.VERSION.SDK_INT >= 30) return v.directory
        return runCatching {
            val path = StorageVolume::class.java.getMethod("getPath").invoke(v) as String
            File(path)
        }.getOrNull()
    }

    private fun makeInfo(name: String, dir: File): DriveInfo {
        val (total, free) = runCatching {
            val st = StatFs(dir.absolutePath)
            st.blockCountLong * st.blockSizeLong to st.availableBlocksLong * st.blockSizeLong
        }.getOrDefault(0L to 0L)
        return DriveInfo(name, dir.absolutePath, total, free)
    }

    // ------------------------------------------------------------------ scan

    suspend fun scanDrive(root: File, onProgress: (Int) -> Unit): ScanResult {
        val videos = ArrayList<FileItem>()
        val audios = ArrayList<FileItem>()
        val images = ArrayList<FileItem>()
        val queue = ArrayDeque<File>()
        queue.add(root)
        var seen = 0
        while (queue.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val dir = queue.removeFirst()
            val children = dir.listFiles() ?: continue
            for (f in children) {
                val n = f.name
                if (n.startsWith(".")) continue
                if (f.isDirectory) {
                    if (n.lowercase() in skipDirs) continue
                    queue.add(f)
                    continue
                }
                val kind = classify(f)
                if (kind == MediaKind.OTHER) continue
                val item = FileItem(f, kind, f.length(), f.lastModified())
                when (kind) {
                    MediaKind.VIDEO -> videos.add(item)
                    MediaKind.AUDIO -> audios.add(item)
                    MediaKind.IMAGE -> images.add(item)
                    else -> Unit
                }
                seen++
                if (seen % 25 == 0) onProgress(seen)
            }
        }
        onProgress(seen)
        val cmp = Comparator<FileItem> { a, b ->
            Natural.compare(a.file.path.lowercase(), b.file.path.lowercase())
        }
        return ScanResult(videos.sortedWith(cmp), audios.sortedWith(cmp), images.sortedWith(cmp))
    }

    // --------------------------------------------------------------- listing

    fun listDir(dir: File): List<FileItem> {
        val files = dir.listFiles() ?: return emptyList()
        var counted = 0
        val items = files.filter { !it.name.startsWith(".") }.map { f ->
            if (f.isDirectory) {
                val count = if (counted++ < 400) {
                    f.list()?.count { !it.startsWith(".") } ?: 0
                } else -1
                FileItem(f, MediaKind.FOLDER, 0, f.lastModified(), count)
            } else {
                FileItem(f, classify(f), f.length(), f.lastModified())
            }
        }
        return items.sortedWith(
            compareBy<FileItem> { !it.isDir }
                .thenComparator { a, b -> Natural.compare(a.name.lowercase(), b.name.lowercase()) }
        )
    }

    /** Files of one kind inside a folder (non recursive), used as a playlist. */
    fun siblings(file: File, kind: MediaKind): List<String> =
        listDir(file.parentFile ?: return listOf(file.path))
            .filter { it.kind == kind }
            .map { it.file.path }

    // ------------------------------------------------------------ formatting

    fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var v = bytes / 1024.0
        var i = 0
        while (v >= 1024 && i < units.lastIndex) {
            v /= 1024; i++
        }
        return String.format(Locale.US, if (v >= 100) "%.0f %s" else "%.1f %s", v, units[i])
    }

    private val faDate by lazy {
        android.icu.text.SimpleDateFormat(
            "yyyy/MM/dd  HH:mm",
            android.icu.util.ULocale("fa_IR@calendar=persian;numbers=latn"),
        )
    }

    fun formatDate(ms: Long): String =
        if (ms <= 0) "—" else runCatching { faDate.format(Date(ms)) }.getOrDefault("—")

    fun formatTime(ms: Long): String {
        if (ms < 0) return "0:00"
        val s = ms / 1000
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
        else String.format(Locale.US, "%d:%02d", m, sec)
    }
}

object Natural : Comparator<String> {
    override fun compare(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            val ca = a[i]
            val cb = b[j]
            if (ca.isDigit() && cb.isDigit()) {
                var ei = i
                while (ei < a.length && a[ei].isDigit()) ei++
                var ej = j
                while (ej < b.length && b[ej].isDigit()) ej++
                val na = a.substring(i, ei).trimStart('0')
                val nb = b.substring(j, ej).trimStart('0')
                if (na.length != nb.length) return na.length.compareTo(nb.length)
                val c = na.compareTo(nb)
                if (c != 0) return c
                i = ei
                j = ej
            } else {
                if (ca != cb) return ca.compareTo(cb)
                i++
                j++
            }
        }
        return (a.length - i).compareTo(b.length - j)
    }
}
