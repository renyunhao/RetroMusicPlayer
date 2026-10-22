package code.name.monkey.retromusic.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE LyricsEntity")
        database.execSQL("DROP TABLE BlackListStoreEntity")
    }
}

val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE SongEntity ADD COLUMN genre TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE HistoryEntity ADD COLUMN genre TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE PlayCountEntity ADD COLUMN genre TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ScannedSongEntity (
                row_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                id INTEGER NOT NULL,
                title TEXT NOT NULL,
                track_number INTEGER NOT NULL,
                year INTEGER NOT NULL,
                duration INTEGER NOT NULL,
                data TEXT NOT NULL,
                date_modified INTEGER NOT NULL,
                album_id INTEGER NOT NULL,
                album_name TEXT NOT NULL,
                artist_id INTEGER NOT NULL,
                artist_name TEXT NOT NULL,
                composer TEXT,
                album_artist TEXT,
                genre TEXT NOT NULL,
                folder_path TEXT NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_ScannedSongEntity_data ON ScannedSongEntity(data)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_ScannedSongEntity_album_name ON ScannedSongEntity(album_name)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_ScannedSongEntity_artist_name ON ScannedSongEntity(artist_name)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_ScannedSongEntity_folder_path ON ScannedSongEntity(folder_path)")
    }
}