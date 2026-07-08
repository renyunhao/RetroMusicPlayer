package code.name.monkey.retromusic.repository

import code.name.monkey.retromusic.model.Genre
import code.name.monkey.retromusic.model.Song
import java.text.Collator

interface GenreRepository {
    fun genres(): List<Genre>
    fun genres(query: String): List<Genre>
    fun songs(genreId: Long): List<Song>
    fun song(genreId: Long): Song
}

class RealGenreRepository(
    private val songRepository: RealSongRepository
) : GenreRepository {

    override fun genres(): List<Genre> {
        val songs = songRepository.songs()
        return buildGenres(songs)
    }

    override fun genres(query: String): List<Genre> {
        val songs = songRepository.songs()
        return buildGenres(songs).filter { it.name.equals(query, ignoreCase = true) }
    }

    override fun songs(genreId: Long): List<Song> {
        return songRepository.songs().filter { song ->
            song.genre.hashCode().toLong() == genreId
        }
    }

    override fun song(genreId: Long): Song {
        return songs(genreId).firstOrNull() ?: Song.emptySong
    }

    private fun buildGenres(songs: List<Song>): List<Genre> {
        val grouped = songs.groupBy { it.genre.ifBlank { "" } }
        val genres = grouped.map { (genreName, songsInGenre) ->
            Genre(
                id = genreName.hashCode().toLong(),
                name = genreName,
                songCount = songsInGenre.size
            )
        }.filter { it.songCount > 0 }
        val collator = Collator.getInstance()
        return genres.sortedWith { g1, g2 -> collator.compare(g1.name, g2.name) }
    }
}
