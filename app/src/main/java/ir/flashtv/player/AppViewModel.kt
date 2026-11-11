package ir.flashtv.player

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AppViewModel(app: Application) : AndroidViewModel(app) {

    var drives by mutableStateOf<List<DriveInfo>>(emptyList()); private set
    var loadingDrives by mutableStateOf(true); private set
    var drive by mutableStateOf<DriveInfo?>(null); private set
    var scan by mutableStateOf<ScanResult?>(null); private set
    var scanning by mutableStateOf(false); private set
    var scanned by mutableIntStateOf(0); private set
    var scanFailed by mutableStateOf(false); private set
    var hasPermission by mutableStateOf(true)

    /** Remembers which row/tile had focus per screen so Back returns to it. */
    val focusMemory = HashMap<String, Int>()

    val stack = mutableStateListOf<Screen>(Screen.Drives)
    val current: Screen get() = stack.last()

    private var scanJob: Job? = null

    fun refreshDrives() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { Storage.getDrives(getApplication()) }
            drives = list
            loadingDrives = false
            val cur = drive
            if (cur != null && list.none { it.path == cur.path }) closeDrive()
        }
    }

    fun selectDrive(d: DriveInfo) {
        drive = d
        stack.clear()
        stack.add(Screen.Drives)
        stack.add(Screen.Dashboard)
        startScan()
    }

    fun startScan() {
        val d = drive ?: return
        scanJob?.cancel()
        scan = null
        scanned = 0
        scanFailed = false
        scanning = true
        scanJob = viewModelScope.launch {
            try {
                val root = File(d.path)
                val result = withContext(Dispatchers.IO) {
                    Storage.scanDrive(root) { scanned = it }
                }
                scan = result
                scanFailed = root.listFiles() == null
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                scanFailed = true
            } finally {
                scanning = false
            }
        }
    }

    fun closeDrive() {
        scanJob?.cancel()
        drive = null
        scan = null
        scanning = false
        stack.clear()
        stack.add(Screen.Drives)
    }

    fun push(s: Screen) {
        stack.add(s)
    }

    /** Replace the top screen (used when moving between files inside a player). */
    fun replaceTop(s: Screen) {
        if (stack.isNotEmpty()) stack[stack.lastIndex] = s
    }

    /** @return true when something was popped. */
    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        if (stack.last() is Screen.Drives) closeDrive()
        return true
    }
}
