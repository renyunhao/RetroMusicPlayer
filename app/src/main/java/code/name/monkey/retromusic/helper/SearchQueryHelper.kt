package code.name.monkey.retromusic.helper

import android.app.SearchManager
import android.os.Bundle
import android.provider.MediaStore
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.repository.RealSongRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object SearchQueryHelper : KoinComponent {
    private val songRepository by inject<RealSongRepository>()
    var songs = ArrayList<Song>()

    @JvmStatic
    fun getSongs(extras: Bundle): List<Song> {
        val query = extras.getString(SearchManager.QUERY, null)
        val artistName = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST, null)
        val albumName = extras.getString(MediaStore.EXTRA_MEDIA_ALBUM, null)
        val titleName = extras.getString(MediaStore.EXTRA_MEDIA_TITLE, null)

        val allSongs = songRepository.songs()

        var songs = listOf<Song>()
        if (artistName != null && albumName != null && titleName != null) {
            songs = allSongs.filter {
                it.artistName.equals(artistName, ignoreCase = true) &&
                it.albumName.equals(albumName, ignoreCase = true) &&
                it.title.equals(titleName, ignoreCase = true)
            }
        }
        if (songs.isNotEmpty()) {
            return songs
        }
        if (artistName != null && titleName != null) {
            songs = allSongs.filter {
                it.artistName.equals(artistName, ignoreCase = true) &&
                it.title.equals(titleName, ignoreCase = true)
            }
        }
        if (songs.isNotEmpty()) {
            return songs
        }
        if (albumName != null && titleName != null) {
            songs = allSongs.filter {
                it.albumName.equals(albumName, ignoreCase = true) &&
                it.title.equals(titleName, ignoreCase = true)
            }
        }
        if (songs.isNotEmpty()) {
            return songs
        }
        if (artistName != null) {
            songs = allSongs.filter {
                it.artistName.equals(artistName, ignoreCase = true)
            }
        }
        if (songs.isNotEmpty()) {
            return songs
        }
        if (albumName != null) {
            songs = allSongs.filter {
                it.albumName.equals(albumName, ignoreCase = true)
            }
        }
        if (songs.isNotEmpty()) {
            return songs
        }
        if (titleName != null) {
            songs = allSongs.filter {
                it.title.equals(titleName, ignoreCase = true)
            }
        }
        if (songs.isNotEmpty()) {
            return songs
        }
        songs = allSongs.filter {
            it.artistName.equals(query, ignoreCase = true)
        }
        if (songs.isNotEmpty()) {
            return songs
        }
        songs = allSongs.filter {
            it.albumName.equals(query, ignoreCase = true)
        }
        if (songs.isNotEmpty()) {
            return songs
        }
        songs = allSongs.filter {
            it.title.equals(query, ignoreCase = true)
        }
        return songs.ifEmpty { ArrayList() }
    }
}
