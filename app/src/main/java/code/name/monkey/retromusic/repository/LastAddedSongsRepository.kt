package code.name.monkey.retromusic.repository

import code.name.monkey.retromusic.model.Album
import code.name.monkey.retromusic.model.Artist
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.PreferenceUtil

interface LastAddedRepository {
    fun recentSongs(): List<Song>
    fun recentAlbums(): List<Album>
    fun recentArtists(): List<Artist>
}

class RealLastAddedRepository(
    private val songRepository: RealSongRepository,
    private val albumRepository: RealAlbumRepository,
    private val artistRepository: RealArtistRepository
) : LastAddedRepository {
    override fun recentSongs(): List<Song> {
        val cutoff = PreferenceUtil.lastAddedCutoff
        return songRepository.songs().filter {
            it.dateModified > cutoff
        }.sortedByDescending { it.dateModified }
    }

    override fun recentAlbums(): List<Album> {
        return albumRepository.splitIntoAlbums(recentSongs(), sorted = false)
    }

    override fun recentArtists(): List<Artist> {
        return artistRepository.splitIntoArtists(recentAlbums())
    }
}
