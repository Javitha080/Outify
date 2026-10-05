package cc.tomko.outify.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cc.tomko.outify.data.dao.AlbumArtistDao
import cc.tomko.outify.data.dao.AlbumDao
import cc.tomko.outify.data.dao.AlbumTrackDao
import cc.tomko.outify.data.dao.ArtistDao
import cc.tomko.outify.data.dao.LikedDao
import cc.tomko.outify.data.dao.LikedItemsDao
import cc.tomko.outify.data.dao.PlaylistDao
import cc.tomko.outify.data.dao.TrackArtistDao
import cc.tomko.outify.data.dao.TrackDao
import cc.tomko.outify.data.dao.TrackFileDao
import cc.tomko.outify.data.database.album.AlbumArtistEntity
import cc.tomko.outify.data.database.album.AlbumTrackCrossRef
import cc.tomko.outify.data.database.playlist.PlaylistDiffEntity
import cc.tomko.outify.data.database.playlist.PlaylistItemEntity
import cc.tomko.outify.data.database.track.LikedTrackEntity
import cc.tomko.outify.data.database.track.PlaylistTrackEntity

@Database(
    entities = [
        TrackEntity::class,
        TrackFileEntity::class,
        ArtistEntity::class,
        TrackArtistEntity::class,
        AlbumEntity::class,
        AlbumTrackCrossRef::class,
        AlbumArtistEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        PlaylistDiffEntity::class,
        PlaylistTrackEntity::class,
        LikedTrackEntity::class,
        LikedItemsEntity::class,
    ],
    version = 16,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun trackFileDao(): TrackFileDao
    abstract fun artistDao(): ArtistDao
    abstract fun trackArtistDao(): TrackArtistDao
    abstract fun albumDao(): AlbumDao
    abstract fun albumArtistDao(): AlbumArtistDao
    abstract fun albumTrackDao(): AlbumTrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun likedDao(): LikedDao
    abstract fun likedItemsDao(): LikedItemsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS liked_items (
                        uri TEXT NOT NULL PRIMARY KEY,
                        type TEXT NOT NULL,
                        addedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "outify_database"
                )
                    .addMigrations(MIGRATION_15_16)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}