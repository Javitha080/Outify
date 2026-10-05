package cc.tomko.outify.playback.model

enum class Bitrate {
    KBPS320,
    KBPS160,
    KBPS96,
}

fun Bitrate.getName() = when (this) {
    Bitrate.KBPS320 -> "Very high"
    Bitrate.KBPS160 -> "High"
    Bitrate.KBPS96 -> "Normal"
}

fun Bitrate.getSpeed() = when (this) {
    Bitrate.KBPS320 -> 320
    Bitrate.KBPS160 -> 160
    Bitrate.KBPS96 -> 96
}