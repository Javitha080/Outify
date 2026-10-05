package cc.tomko.outify.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cc.tomko.outify.data.database.TrackFileEntity
import javax.inject.Singleton

@Dao
@Singleton
interface TrackFileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackFiles(files: List<TrackFileEntity>)

    @Query("DELETE FROM track_files WHERE trackId IN (:trackIds)")
    suspend fun deleteFilesForTracks(trackIds: List<String>)
}