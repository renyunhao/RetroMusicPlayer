package code.name.monkey.retromusic.repository

import android.content.Context

import code.name.monkey.retromusic.helper.SortOrder.SongSortOrder
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.providers.BlacklistStore
import code.name.monkey.retromusic.util.PreferenceUtil

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.text.Collator

interface SongRepository {

    fun songs(): List<Song>

    fun songs(query: String): List<Song>

    fun song(songId: Long): Song

    fun songsByFilePath(filePath: String, ignoreBlacklist: Boolean = false): List<Song>

    fun refresh()
}

class RealSongRepository(private val context: Context) : SongRepository {

    @Volatile
    private var cachedSongs: List<Song> = emptyList()

    @Volatile
    private var hasScanned = false

    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "flac", "wav", "ogg", "m4a", "aac", "wma", "opus", "aiff", "ape"
    )

    private fun ensureScanned() {
        if (!hasScanned) {
            refresh()
        }
    }

    override fun refresh() {
        val blacklist = BlacklistStore.getInstance(context).paths
        val scanDirs = PreferenceUtil.scanDirectories
        if (scanDirs.isEmpty()) {
            cachedSongs = emptyList()
            hasScanned = true
            return
        }
        val firstDir = File(scanDirs.first())
        if (!firstDir.exists() || !firstDir.canRead()) {
            cachedSongs = emptyList()
            hasScanned = true
            return
        }
        val songs = mutableListOf<Song>()
        for (dirPath in scanDirs) {
            val dir = File(dirPath)
            songs.addAll(scanDirectory(dir, blacklist))
        }
        cachedSongs = songs
        hasScanned = true
    }

    override fun songs(): List<Song> {
        ensureScanned()
        return sortedSongs(cachedSongs)
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

    private fun scanDirectory(dir: File, blacklist: List<String>): List<Song> {
        val songs = mutableListOf<Song>()
        if (!dir.exists() || !dir.isDirectory) return songs

        dir.walkTopDown().forEach { file ->
            if (!file.isFile) return@forEach
            val path = file.absolutePath
            if (blacklist.any { path.startsWith(it) }) return@forEach
            if (file.extension.lowercase() !in AUDIO_EXTENSIONS) return@forEach
            val song = readSongFromFile(file) ?: return@forEach
            if (song.duration < PreferenceUtil.filterLength * 1000L) return@forEach
            songs.add(song)
        }
        return songs
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
