package cc.tomko.outify.data.queue

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SavedQueue(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val trackUris: List<String>,
    val currentIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)