package cc.tomko.outify.data.database

import androidx.room.Embedded
import androidx.room.Relation
import cc.tomko.outify.core.model.Track
import cc.tomko.outify.data.dao.AlbumDao
import cc.tomko.outify.data.dao.LikedDao
import cc.tomko.outify.data.database.album.AlbumWithArtists
import cc.tomko.outify.data.database.track.LikedTrackEntity

/**
 * Join for cached liked track position with the TrackEntity
 */
data class LikedTrackWithTrack(
    @Embedded val liked: LikedTrackEntity,
    @Relation(
        parentColumn = "trackId",
        entityColumn = "trackUri"
    )
    val track: TrackEntity?
)

suspend fun LikedTrackWithTrack.toDomain(
    likedDao: LikedDao,
    albumDao: AlbumDao,
): Track {
    val trackEntity =
        track ?: throw IllegalStateException("Track missing for liked track ${liked.trackId}")

    val trackWithArtists = likedDao.getTracksWithArtists(listOf(trackEntity.id)).firstOrNull()
        ?: throw IllegalStateException("TrackWithArtists missing for ${trackEntity.id}")

    val albumWithArtists: AlbumWithArtists? = trackEntity.albumId?.let { albumId ->
        albumDao.getAlbumWithArtists(albumId)
    }

    return trackWithArtists.toDomain(albumWithArtists)
}