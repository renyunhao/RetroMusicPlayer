package code.name.monkey.retromusic.repository

import android.content.Context
import code.name.monkey.retromusic.db.ScannedSongDao
import code.name.monkey.retromusic.db.ScannedSongEntity
import code.name.monkey.retromusic.db.toSong
import code.name.monkey.retromusic.helper.SortOrder.SongSortOrder
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.providers.BlacklistStore
import code.name.monkey.retromusic.util.PreferenceUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.text.Collator
import java.util.concurrent.atomic.AtomicInteger

data class ScanProgress(
    val scannedFiles: Int,
    val totalFiles: Int,
    val currentPath: String,
    val isComplete: Boolean = false
)

interface SongRepository {

    fun songs(): List<Song>

    fun songs(query: String): List<Song>

    fun song(songId: Long): Song

    fun songsByFilePath(filePath: String, ignoreBlacklist: Boolean = false): List<Song>

    fun refresh()

    suspend fun refreshWithProgress(): Flow<ScanProgress>

    fun scanProgress(): Flow<ScanProgress>
}

class RealSongRepository(
    private val context: Context,
    private val scannedSongDao: ScannedSongDao
) : SongRepository {

    @Volatile
    private var cachedSongs: List<Song> = emptyList()

    @Volatile
    private var hasScanned = false

    // Cache sorted results to avoid sorting 5000+ items on every songs() query
    @Volatile
    private var cachedSortedSongs: List<Song>? = null
    @Volatile
    private var lastSortOrder: String? = null

    private val _scanProgress = MutableStateFlow(ScanProgress(0, 0, ""))
    override fun scanProgress() = _scanProgress.asStateFlow()

    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "flac", "wav", "ogg", "m4a", "aac", "wma", "opus", "aiff", "ape"
    )

    private fun ensureScanned() {
        if (!hasScanned) {
            loadFromDatabase()
        }
    }

    private fun loadFromDatabase() {
        runBlocking {
            val dbSongs = scannedSongDao.getAllSongs()
            if (dbSongs.isNotEmpty()) {
                cachedSongs = dbSongs.map { it.toSong() }
                hasScanned = true
            }
        }
    }

    override fun songs(): List<Song> {
        ensureScanned()
        val currentOrder = PreferenceUtil.songSortOrder
        val currentCachedSorted = cachedSortedSongs
        if (currentCachedSorted != null && lastSortOrder == currentOrder) {
            return currentCachedSorted
        }
        val sorted = sortedSongs(cachedSongs)
        cachedSortedSongs = sorted
        lastSortOrder = currentOrder
        return sorted
    }

    override fun songs(query: String): List<Song> {
        ensureScanned()
        return sortedSongs(cachedSongs.filter {
            it.title.contains(query, ignoreCase = true)
        })
    }

    override fun song(songId: Long): Song {
        ensureScanned()
        return cachedSongs.find { it.id == songId } ?: Song.emptySong
    }

    override fun songsByFilePath(filePath: String, ignoreBlacklist: Boolean): List<Song> {
        ensureScanned()
        if (ignoreBlacklist) {
            return cachedSongs.filter { it.data == filePath }
        }
        val blacklist = BlacklistStore.getInstance(context).paths
        return cachedSongs.filter {
            it.data == filePath && blacklist.none { path -> it.data.startsWith(path) }
        }
    }

    override fun refresh() {
        runBlocking {
            performIncrementalScan(null)
        }
    }

    override suspend fun refreshWithProgress(): Flow<ScanProgress> {
        _scanProgress.value = ScanProgress(0, 0, "")
        performIncrementalScan(_scanProgress)
        return _scanProgress.asStateFlow()
    }

    /**
     * Incremental & parallel scanning optimized for in-vehicle head units (CASKA Pro / K24).
     * 1. Collects all audio files from disk.
     * 2. Checks against existing database entries by file path and last modified time.
     * 3. For unchanged files, reuses existing DB records directly (zero Tag parsing overhead).
     * 4. For new or modified files, parses tags using coroutine concurrency (4 parallel workers).
     * 5. Deletes obsolete files no longer on disk.
     * 6. Throttles progress emission so weak car CPU is not overwhelmed.
     */
    private suspend fun performIncrementalScan(progressFlow: MutableStateFlow<ScanProgress>?) = withContext(Dispatchers.IO) {
        val blacklist = BlacklistStore.getInstance(context).paths
        val scanDirs = PreferenceUtil.scanDirectories

        if (scanDirs.isEmpty()) {
            cachedSongs = emptyList()
            cachedSortedSongs = null
            hasScanned = true
            scannedSongDao.deleteAll()
            progressFlow?.value = ScanProgress(0, 0, "", isComplete = true)
            return@withContext
        }

        // Collect all audio files on disk
        val allAudioFiles = mutableListOf<File>()
        for (dirPath in scanDirs) {
            val dir = File(dirPath)
            if (dir.exists() && dir.isDirectory) {
                collectAudioFiles(dir, blacklist, allAudioFiles)
            }
        }

        val totalFiles = allAudioFiles.size
        progressFlow?.value = ScanProgress(0, totalFiles, "")

        if (totalFiles == 0) {
            cachedSongs = emptyList()
            cachedSortedSongs = null
            hasScanned = true
            scannedSongDao.deleteAll()
            progressFlow?.value = ScanProgress(0, 0, "", isComplete = true)
            return@withContext
        }

        // Fetch existing database records
        val existingEntities = scannedSongDao.getAllSongs()
        val existingMap = HashMap<String, ScannedSongEntity>(existingEntities.size)
        for (entity in existingEntities) {
            existingMap[entity.data] = entity
        }

        val filesToParse = mutableListOf<File>()
        val reusedEntities = ArrayList<ScannedSongEntity>(totalFiles)
        val currentDiskPaths = HashSet<String>(totalFiles)

        for (file in allAudioFiles) {
            val path = file.absolutePath
            currentDiskPaths.add(path)
            val modifiedSec = file.lastModified() / 1000
            val existing = existingMap[path]

            if (existing != null && existing.dateModified == modifiedSec) {
                // File unchanged, reuse directly without reading tags via Jaudiotagger
                reusedEntities.add(existing)
            } else {
                // New or updated file, needs parsing
                filesToParse.add(file)
            }
        }

        // Parse new/modified files in parallel (limit concurrency to 4 to balance CPU and I/O)
        val concurrency = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
        val semaphore = Semaphore(concurrency)
        val parsedEntities = mutableListOf<ScannedSongEntity>()

        val filterLengthMs = PreferenceUtil.filterLength * 1000L
        val processedCounter = AtomicInteger(reusedEntities.size)
        var lastReportTime = System.currentTimeMillis()

        // Report initial progress with reused count
        progressFlow?.value = ScanProgress(
            scannedFiles = reusedEntities.size,
            totalFiles = totalFiles,
            currentPath = if (filesToParse.isNotEmpty()) filesToParse.first().name else "",
            isComplete = filesToParse.isEmpty()
        )

        val deferredResults = filesToParse.map { file ->
            async(Dispatchers.IO) {
                val song = semaphore.withPermit {
                    readSongFromFile(file)
                }

                val currentCount = processedCounter.incrementAndGet()

                // Throttle progress emissions: only update every 20 files or every 200ms
                if (progressFlow != null) {
                    val now = System.currentTimeMillis()
                    if (currentCount % 20 == 0 || now - lastReportTime > 200 || currentCount == totalFiles) {
                        lastReportTime = now
                        progressFlow.value = ScanProgress(
                            scannedFiles = currentCount,
                            totalFiles = totalFiles,
                            currentPath = file.name,
                            isComplete = currentCount == totalFiles
                        )
                    }
                }

                if (song != null && song.duration >= filterLengthMs) {
                    song.toScannedSongEntity()
                } else null
            }
        }

        val parsedResults = deferredResults.awaitAll().filterNotNull()
        parsedEntities.addAll(parsedResults)

        // Combine all valid entities
        val allValidEntities = ArrayList<ScannedSongEntity>(reusedEntities.size + parsedEntities.size)
        allValidEntities.addAll(reusedEntities)
        allValidEntities.addAll(parsedEntities)

        // Find obsolete entries in DB that were deleted from disk
        val deletedPaths = existingMap.keys.filter { it !in currentDiskPaths }

        // Batch update database
        if (filesToParse.isNotEmpty()) {
            // Insert or replace updated/new records in batches of 100
            parsedEntities.chunked(100).forEach { batch ->
                scannedSongDao.insertAll(batch)
            }
        }

        if (deletedPaths.isNotEmpty()) {
            deletedPaths.chunked(100).forEach { batch ->
                scannedSongDao.deleteByPaths(batch)
            }
        }

        val finalSongs = allValidEntities.map { it.toSong() }
        cachedSongs = finalSongs
        cachedSortedSongs = null // Invalidate sorted cache
        hasScanned = true

        progressFlow?.value = ScanProgress(totalFiles, totalFiles, "", isComplete = true)
    }

    private fun collectAudioFiles(dir: File, blacklist: List<String>, result: MutableList<File>) {
        dir.walkTopDown().forEach { file ->
            if (!file.isFile) return@forEach
            val path = file.absolutePath
            if (blacklist.any { path.startsWith(it) }) return@forEach
            if (file.extension.lowercase() !in AUDIO_EXTENSIONS) return@forEach
            result.add(file)
        }
    }

    private fun sortedSongs(songs: List<Song>): List<Song> {
        val collator = Collator.getInstance()
        return when (PreferenceUtil.songSortOrder) {
            SongSortOrder.SONG_A_Z -> {
                songs.sortedWith { s1, s2 -> collator.compare(s1.title, s2.title) }
            }
            SongSortOrder.SONG_Z_A -> {
                songs.sortedWith { s1, s2 -> collator.compare(s2.title, s1.title) }
            }
            SongSortOrder.SONG_ARTIST -> {
                songs.sortedWith { s1, s2 -> collator.compare(s1.artistName, s2.artistName) }
            }
            SongSortOrder.SONG_ALBUM_ARTIST -> {
                songs.sortedWith { s1, s2 -> collator.compare(s1.albumArtist ?: "", s2.albumArtist ?: "") }
            }
            SongSortOrder.SONG_ALBUM -> {
                songs.sortedWith { s1, s2 -> collator.compare(s1.albumName, s2.albumName) }
            }
            SongSortOrder.SONG_YEAR -> {
                songs.sortedByDescending { it.year }
            }
            SongSortOrder.SONG_DURATION -> {
                songs.sortedByDescending { it.duration }
            }
            SongSortOrder.SONG_DATE -> {
                songs.sortedByDescending { it.dateModified }
            }
            SongSortOrder.SONG_DATE_MODIFIED -> {
                songs.sortedByDescending { it.dateModified }
            }
            SongSortOrder.COMPOSER -> {
                songs.sortedWith { s1, s2 -> collator.compare(s1.composer ?: "", s2.composer ?: "") }
            }
            else -> songs
        }
    }

    private fun readSongFromFile(file: File): Song? {
        return try {
            val audioFile = AudioFileIO.read(file)
            val header = audioFile.audioHeader
            val tag = audioFile.tag

            val duration = header.trackLength * 1000L
            val title = tag?.getFirst(FieldKey.TITLE)?.ifBlank { file.nameWithoutExtension }
                ?: file.nameWithoutExtension
            val trackNumber = tag?.getFirst(FieldKey.TRACK)?.toIntOrNull() ?: -1
            val year = tag?.getFirst(FieldKey.YEAR)?.toIntOrNull() ?: 0
            val albumName = tag?.getFirst(FieldKey.ALBUM) ?: ""
            val artistName = tag?.getFirst(FieldKey.ARTIST) ?: ""
            val composer = tag?.getFirst(FieldKey.COMPOSER) ?: ""
            val albumArtist = tag?.getFirst(FieldKey.ALBUM_ARTIST) ?: ""
            val genre = tag?.getFirst(FieldKey.GENRE) ?: ""

            val data = file.absolutePath
            val dateModified = file.lastModified() / 1000

            Song(
                id = data.hashCode().toLong(),
                title = title,
                trackNumber = trackNumber,
                year = year,
                duration = duration,
                data = data,
                dateModified = dateModified,
                albumId = albumName.hashCode().toLong(),
                albumName = albumName,
                artistId = artistName.hashCode().toLong(),
                artistName = artistName,
                composer = composer,
                albumArtist = albumArtist,
                genre = genre
            )
        } catch (e: Exception) {
            null
        }
    }
}

private fun Song.toScannedSongEntity(): ScannedSongEntity {
    val folderPath = data.substringBeforeLast("/")
    return ScannedSongEntity(
        id = id,
        title = title,
        trackNumber = trackNumber,
        year = year,
        duration = duration,
        data = data,
        dateModified = dateModified,
        albumId = albumId,
        albumName = albumName,
        artistId = artistId,
        artistName = artistName,
        composer = composer,
        albumArtist = albumArtist,
        genre = genre,
        folderPath = folderPath
    )
}

private fun ScannedSongEntity.toSong(): Song {
    return Song(
        id = id,
        title = title,
        trackNumber = trackNumber,
        year = year,
        duration = duration,
        data = data,
        dateModified = dateModified,
        albumId = albumId,
        albumName = albumName,
        artistId = artistId,
        artistName = artistName,
        composer = composer,
        albumArtist = albumArtist,
        genre = genre
    )
}
