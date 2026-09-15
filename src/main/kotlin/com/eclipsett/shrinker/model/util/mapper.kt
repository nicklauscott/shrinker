package com.eclipsett.shrinker.model.util

import com.eclipsett.shrinker.model.TaskDetail
import com.eclipsett.shrinker.model.dto.TaskDetailDTO
import com.eclipsett.shrinker.model.dto.TaskRequestDTO
import com.eclipsett.shrinker.model.dto.TaskResponseDTO
import com.eclipsett.shrinker.model.entities.TaskEntity

// from client
fun TaskRequestDTO.toEntity(bpp: Double? = null, verdict: String? = null, callback:(TaskEntity) -> Unit): TaskEntity {
    return TaskEntity.new {
        name = this@toEntity.name
        userEMail = this@toEntity.email
        this.bpp = bpp
        this.verdict = verdict
        originalUrl = this@toEntity.originalUrl
        compressionLevel = this@toEntity.compressionLevel
    } .apply { callback(this) }
}

fun TaskEntity.toDetail(): TaskDetail { // to client; add compressedFileUrl dynamically
    return TaskDetail(
        bpp = bpp,
        name = name,
        id = id.value,
        status = status,
        errors = errors,
        verdict = verdict,
        progress = progress,
        objectId = objectId,
        userEMail = userEMail,
        derivedName = derivedName,
        originalUrl = originalUrl,
        otherDetails = otherDetails,
        finalFileSize = finalFileSize,
        originalFileSize = originalFileSize,
        createdTimestamp = createdTimestamp,
        updatedTimestamp = updatedTimestamp,
        compressionLevel = compressionLevel,
    )
}

fun TaskDetail.toDTO(): TaskDetailDTO {
    return TaskDetailDTO(
        bpp = bpp,
        name = name,
        id = id,
        status = status,
        errors = errors,
        verdict = verdict,
        progress = progress,
        objectId = objectId,
        derivedName = derivedName,
        originalUrl = originalUrl,
        otherDetails = otherDetails,
        createdTimestamp = createdTimestamp,
        updatedTimestamp = updatedTimestamp,
        compressionLevel = compressionLevel,
        finalFileSize = finalFileSize?.let {  formatFileSize(it) } ,
        originalFileSize = originalFileSize?.let {  formatFileSize(it) }
    )
}

fun TaskDetailDTO.toResponse(): TaskResponseDTO {
    return TaskResponseDTO(id, name, bpp, verdict,createdTimestamp)
}

fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"

    val units = arrayOf("KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unitIndex = -1

    do {
        size /= 1024
        unitIndex++
    } while (size >= 1024 && unitIndex < units.lastIndex)

    return "%.2f %s".format(size, units[unitIndex])
}