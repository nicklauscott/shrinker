package com.eclipsett.shrinker.compression.constant

enum class JobStatus { RUNNING, DONE, FAILED }

data class CompressionJob(
    val id: String, var status: JobStatus,
    var outputPath: String? = null,
    var error: String? = null
)
