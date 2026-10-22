package code.name.monkey.retromusic.db

import androidx.room.*

data class FilePathAndModified(
    @ColumnInfo(name = "data") val data: String,
    @ColumnInfo(name = "date_modified") val dateModified: Long
)

@Dao
interface ScannedSongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<ScannedSongEntity>)

    @Query("SELECT * FROM ScannedSongEntity")
    suspend fun getAllSongs(): List<ScannedSongEntity>

    @Query("SELECT COUNT(*) FROM ScannedSongEntity")
    suspend fun count(): Int

    @Query("DELETE FROM ScannedSongEntity")
    suspend fun deleteAll()

    @Query("SELECT data, date_modified FROM ScannedSongEntity")
    suspend fun getAllDataAndDateModified(): List<FilePathAndModified>

    @Query("DELETE FROM ScannedSongEntity WHERE data IN (:paths)")
    suspend fun deleteByPaths(paths: List<String>)

    @Query("SELECT DISTINCT folder_path FROM ScannedSongEntity ORDER BY folder_path")
    suspend fun getAllFolderPaths(): List<String>

    @Query("SELECT * FROM ScannedSongEntity WHERE folder_path = :folderPath")
    suspend fun getSongsByFolder(folderPath: String): List<ScannedSongEntity>

    @Query("SELECT * FROM ScannedSongEntity WHERE folder_path LIKE :folderPrefix || '%'")
    suspend fun getSongsByFolderPrefix(folderPrefix: String): List<ScannedSongEntity>
}
