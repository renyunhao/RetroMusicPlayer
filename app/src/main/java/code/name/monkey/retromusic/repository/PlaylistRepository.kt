package code.name.monkey.retromusic.repository

import code.name.monkey.retromusic.db.PlaylistDao

import code.name.monkey.retromusic.db.SongEntity
import code.name.monkey.retromusic.model.Playlist
import code.name.monkey.retromusic.model.Song
import java.text.Collator

interface PlaylistRepository {
    fun searchPlaylist(query: String): List<Playlist>
    fun playlist(playlistName: String): Playlist
    fun playlists(): List<Playlist>
    fun favoritePlaylist(playlistName: String): List<Playlist>
    fun deletePlaylist(playlistId: Long)
    fun playlist(playlistId: Long): Playlist
    fun playlistSongs(playlistId: Long): List<Song>
}

class RealPlaylistRepository(
    private val playlistDao: PlaylistDao,
    private val songRepository: RealSongRepository
) : PlaylistRepository {

    override fun playlist(playlistName: String): Playlist {
        val entities = playlistDao.playlist(playlistName)
        return entities.firstOrNull()?.let { Playlist(it.playListId, it.playlistName) }
            ?: Playlist.empty
    }

    override fun playlist(playlistId: Long): Playlist {
        val entities = playlistDao.playlistById(playlistId)
        return entities.firstOrNull()?.let { Playlist(it.playListId, it.playlistName) }
            ?: Playlist.empty
    }

    override fun searchPlaylist(query: String): List<Playlist> {
        return playlists().filter { it.name.equals(query, ignoreCase = true) }
    }

    override fun playlists(): List<Playlist> {
        val entities = playlistDao.playlistsSync()
        val playlists = entities.map { Playlist(it.playListId, it.playlistName) }
        val collator = Collator.getInstance()
        return playlists.sortedWith { p1, p2 -> collator.compare(p1.name, p2.name) }
    }

    override fun favoritePlaylist(playlistName: String): List<Playlist> {
        val entities = playlistDao.playlist(playlistName)
        return entities.map { Playlist(it.playListId, it.playlistName) }
    }

    override fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylistSongsSync(playlistId)
        playlistDao.deletePlaylistSync(playlistId)
    }

    override fun playlistSongs(playlistId: Long): List<Song> {
        if (playlistId == -1L) return emptyList()
        val songEntities: List<SongEntity> = playlistDao.favoritesSongs(playlistId)
        val allSongs = songRepository.songs()
        val songMap = allSongs.associateBy { it.id }
        return songEntities.mapNotNull { entity -> songMap[entity.id] }
    }
}
