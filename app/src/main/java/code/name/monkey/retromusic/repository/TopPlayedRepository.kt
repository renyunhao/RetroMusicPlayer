package code.name.monkey.retromusic.repository

import android.content.Context
import code.name.monkey.retromusic.Constants.NUMBER_OF_TOP_TRACKS
import code.name.monkey.retromusic.model.Album
import code.name.monkey.retromusic.model.Artist
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.providers.HistoryStore
import code.name.monkey.retromusic.providers.SongPlayCountStore
import code.name.monkey.retromusic.util.PreferenceUtil

interface TopPlayedRepository {
    fun recentlyPlayedTracks(): List<Song>
    fun topTracks(): List<Song>
    fun notRecentlyPlayedTracks(): List<Song>
    fun topAlbums(): List<Album>
    fun topArtists(): List<Artist>
}

class RealTopPlayedRepository(
    private val context: Context,
    private val songRepository: RealSongRepository,
    private val albumRepository: RealAlbumRepository,
    private val artistRepository: RealArtistRepository
) : TopPlayedRepository {

    override fun recentlyPlayedTracks(): List<Song> {
        val historyStore = HistoryStore.getInstance(context)
        val cutoff = PreferenceUtil.getRecentlyPlayedCutoffTimeMillis()
        val cursor = historyStore.queryRecentIds(cutoff)
        val songIds = mutableListOf<Long>()
        cursor.use {
            if (it != null && it.moveToFirst()) {
                val idColumn = it.getColumnIndex(HistoryStore.RecentStoreColumns.ID)
                do {
                    songIds.add(it.getLong(idColumn))
                } while (it.moveToNext())
            }
        }
        val allSongs = songRepository.songs()
        val songMap = allSongs.associateBy { it.id }
        val result = mutableListOf<Song>()
        val validIds = mutableListOf<Long>()
        for (id in songIds) {
            val song = songMap[id]
            if (song != null && song != Song.emptySong) {
                result.add(song)
                validIds.add(id)
            }
        }
        val missingIds = songIds.filter { it !in validIds }
        for (id in missingIds) {
            historyStore.removeSongId(id)
        }
        return result
    }

    override fun topTracks(): List<Song> {
        val playCountStore = SongPlayCountStore.getInstance(context)
        val cursor = playCountStore.getTopPlayedResults(NUMBER_OF_TOP_TRACKS)
        val songIds = mutableListOf<Long>()
        cursor.use {
            if (it != null && it.moveToFirst()) {
                val idColumn = it.getColumnIndex(SongPlayCountStore.SongPlayCountColumns.ID)
                do {
                    songIds.add(it.getLong(idColumn))
                } while (it.moveToNext())
            }
        }
        val allSongs = songRepository.songs()
        val songMap = allSongs.associateBy { it.id }
        val result = mutableListOf<Song>()
        val validIds = mutableListOf<Long>()
        for (id in songIds) {
            val song = songMap[id]
            if (song != null && song != Song.emptySong) {
                result.add(song)
                validIds.add(id)
            }
        }
        val missingIds = songIds.filter { it !in validIds }
        for (id in missingIds) {
            playCountStore.removeItem(id)
        }
        return result
    }

    override fun notRecentlyPlayedTracks(): List<Song> {
        val historyStore = HistoryStore.getInstance(context)
        val cutoff = PreferenceUtil.getRecentlyPlayedCutoffTimeMillis()

        val playedCursor = historyStore.queryRecentIds(0)
        val playedIds = mutableSetOf<Long>()
        playedCursor.use {
            if (it != null && it.moveToFirst()) {
                val idColumn = it.getColumnIndex(HistoryStore.RecentStoreColumns.ID)
                do {
                    playedIds.add(it.getLong(idColumn))
                } while (it.moveToNext())
            }
        }

        val notRecentCursor = historyStore.queryRecentIds(-cutoff)
        val notRecentIds = mutableListOf<Long>()
        notRecentCursor.use {
            if (it != null && it.moveToFirst()) {
                val idColumn = it.getColumnIndex(HistoryStore.RecentStoreColumns.ID)
                do {
                    notRecentIds.add(it.getLong(idColumn))
                } while (it.moveToNext())
            }
        }

        val allSongs = songRepository.songs().toMutableList()
        val songMap = allSongs.associateBy { it.id }

        val playedSongs = playedIds.mapNotNull { songMap[it] }
        val notRecentSongs = notRecentIds.mapNotNull { songMap[it] }

        allSongs.removeAll(playedSongs.toSet())
        allSongs.addAll(notRecentSongs)

        val validIds = allSongs.map { it.id }.toSet()
        val missingPlayed = playedIds.filter { it !in validIds && it !in songMap.keys }
        for (id in missingPlayed) {
            historyStore.removeSongId(id)
        }

        return allSongs
    }

    override fun topAlbums(): List<Album> {
        return albumRepository.splitIntoAlbums(topTracks(), sorted = false)
    }

    override fun topArtists(): List<Artist> {
        return artistRepository.splitIntoArtists(topAlbums())
    }
}
