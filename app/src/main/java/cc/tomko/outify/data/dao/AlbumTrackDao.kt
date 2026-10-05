package cc.tomko.outify.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cc.tomko.outify.data.database.TrackEntity
import cc.tomko.outify.data.database.album.AlbumTrackCrossRef
import javax.inject.Singleton

@Dao
@Singleton
interface AlbumTrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<AlbumTrackCrossRef>)

    @Query("DELETE FROM album_tracks WHERE albumId = :albumId")
    suspend fun deleteByAlbumId(albumId: String)

    @Query("DELETE FROM album_tracks WHERE albumId IN (:albumIds)")
    suspend fun deleteByAlbumIds(albumIds: List<String>)

    @Query(
        """
        SELECT t.* FROM tracks t
        INNER JOIN album_tracks at ON t.id = at.trackId
        WHERE at.albumId = :albumId
        ORDER BY at.position
    """
    )
    suspend fun getTracksForAlbum(albumId: String): List<TrackEntity>

    @Query("SELECT trackId FROM album_tracks WHERE albumId = :albumId ORDER BY position ASC")
    suspend fun getTrackIdsForAlbum(albumId: String): List<String>

    @Query("SELECT albumId FROM album_tracks WHERE trackId = :trackId LIMIT 1")
    suspend fun getAlbumIdForTrack(trackId: String): String?
}