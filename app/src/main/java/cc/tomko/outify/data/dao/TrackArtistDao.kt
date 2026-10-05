package cc.tomko.outify.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import cc.tomko.outify.data.database.TrackArtistEntity
import javax.inject.Singleton

@Dao
@Singleton
interface TrackArtistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(joins: List<TrackArtistEntity>)
}