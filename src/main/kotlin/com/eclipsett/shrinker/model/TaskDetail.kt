package com.eclipsett.shrinker.model

import com.eclipsett.shrinker.compression.constant.CompressionLevel
import com.eclipsett.shrinker.model.entities.TaskTable
import java.util.UUID

data class TaskDetail(
    val id: UUID,
    val name: String,
    val errors: String?,
    val derivedName: String?,
    val originalUrl: String? = null,
    val outputFilePath: String? = null,
    val userEMail: String? = null,

    val status: TaskTable.Status = TaskTable.Status.PENDING,
    val objectId: String? = null,
    val compressedFileUrl: String? = null,
    val compressionLevel: String = CompressionLevel.MODERATE.name,

    val createdTimestamp: String,
    val updatedTimestamp: String,

    val bpp: Double? = null,
    val verdict: String? = null,

    val progress: Int? = null,
    val finalFileSize: Long? = null,
    val originalFileSize: Long? = null,
    val otherDetails: String? = null,
)