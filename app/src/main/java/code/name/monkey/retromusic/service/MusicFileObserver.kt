package code.name.monkey.retromusic.service

import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import code.name.monkey.retromusic.util.PreferenceUtil
import java.io.File

class MusicFileObserver(
    private val onChangeCallback: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var observers = mutableListOf<FileObserver>()
    private var pendingRefresh: Runnable? = null

    fun startWatching() {
        stopWatching()
        for (dirPath in PreferenceUtil.scanDirectories) {
            val dir = File(dirPath)
            if (!dir.exists() || !dir.isDirectory) continue
            val observer = object : FileObserver(dir, MOVED_TO or DELETE or CREATE or MODIFY) {
                override fun onEvent(event: Int, path: String?) {
                    if (path == null) return
                    val ext = path.substringAfterLast('.', "").lowercase()
                    if (ext in AUDIO_EXTENSIONS) {
                        scheduleRefresh()
                    }
                }
            }
            observer.startWatching()
            observers.add(observer)
        }
    }

    fun stopWatching() {
        for (observer in observers) {
            observer.stopWatching()
        }
        observers.clear()
        pendingRefresh?.let { handler.removeCallbacks(it) }
        pendingRefresh = null
    }

    private fun scheduleRefresh() {
        pendingRefresh?.let { handler.removeCallbacks(it) }
        val runnable = Runnable {
            onChangeCallback()
            pendingRefresh = null
        }
        pendingRefresh = runnable
        handler.postDelayed(runnable, REFRESH_DELAY)
    }

    companion object {
        private const val REFRESH_DELAY: Long = 2000
        private val AUDIO_EXTENSIONS = setOf(
            "mp3", "flac", "wav", "ogg", "m4a", "aac", "wma", "opus", "aiff", "ape"
        )
    }
}