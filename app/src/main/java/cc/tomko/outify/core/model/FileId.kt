package cc.tomko.outify.core.model

import cc.tomko.outify.data.database.TrackFileEntity
import kotlinx.serialization.Serializable

@Serializable
data class FileId(
    val type: FileType,
    val id: String,
)

@Serializable
enum class FileType {
    OGG_VORBIS_320,
    OGG_VORBIS_96,
    OGG_VORBIS_160,
    AAC_24,
}

fun FileId.toEntity(trackId: String): TrackFileEntity {
    return TrackFileEntity(
        trackId = trackId,
        type = type,
        fileId = id
    )
}