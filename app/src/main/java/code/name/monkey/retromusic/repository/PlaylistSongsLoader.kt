package code.name.monkey.retromusic.repository

import code.name.monkey.retromusic.model.Song

object PlaylistSongsLoader {

    @JvmStatic
    fun getPlaylistSongList(playlistRepository: PlaylistRepository, playlistId: Long): List<Song> {
        return playlistRepository.playlistSongs(playlistId)
    }
}
