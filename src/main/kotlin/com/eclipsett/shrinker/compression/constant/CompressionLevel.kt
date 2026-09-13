package com.eclipsett.shrinker.compression.constant


enum class CompressionLevel(val flags: List<String>) {
    MILD(
        listOf(
            "-c:v", "libx264",
            "-crf", "23",
            "-preset", "slow",
            "-c:a", "aac",
            "-b:a", "160k",
            "-movflags", "+faststart"
        )
    ),

    MODERATE(
        listOf(
            "-c:v", "libx264",
            "-crf", "28",
            "-preset", "slow",
            "-vf", "scale='min(1280,iw)':-2",
            "-c:a", "aac",
            "-b:a", "128k",
            "-movflags", "+faststart"
        )
    ),

    AGGRESSIVE(
        listOf(
            "-c:v", "libx264",
            "-preset", "slow",
            "-b:v", "800k",
            "-maxrate", "1000k",
            "-bufsize", "2000k",
            "-vf", "scale='min(1280,iw)':-2",
            "-c:a", "aac",
            "-b:a", "96k",
            "-movflags", "+faststart"
        )
    ),

    EXTREME(
        listOf(
            "-c:v", "libx265",
            "-preset", "slow",
            "-b:v", "400k",
            "-maxrate", "500k",
            "-bufsize", "1000k",
            "-vf", "scale='min(854,iw)':-2",
            "-c:a", "aac",
            "-b:a", "64k",
            "-ac", "1",
            "-movflags", "+faststart"
        )
    );

    companion object {
        fun toCompressionLevel(level: String): CompressionLevel {
            return try {
                CompressionLevel.valueOf(level.uppercase())
            } catch (ex: Exception) {
                println("toCompressionLevel ex: ${ex.message}")
                MODERATE
            }
        }
    }
}