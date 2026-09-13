package com.eclipsett.shrinker.model.entities

import com.eclipsett.shrinker.compression.constant.CompressionLevel
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import java.time.LocalDateTime

object TaskTable: UUIDTable("task") {

    var name = varchar("name", 128)
    var derivedName = varchar("derivedName", 128).nullable()

    var compressionLevel = varchar("compressionLevel", 10)
        .default(CompressionLevel.MODERATE.name)

    var originalUrl = varchar("originalUrl", 1024).uniqueIndex()
    var outputFilePath = varchar("outputFilePath", 1024).nullable()

    var finalFileSize = long("finalFileSize").nullable()
    var originalFileSize = long("originalFileSize").nullable()
    var progress = integer("progress").nullable()

    var bpp = double("bpp").nullable()
    val verdict = varchar("verdict", 92).nullable()

    var status = enumeration<Status>("status").default(Status.PENDING)
    var objectId = varchar("objectId", 128).nullable()
    var errors = varchar("errors", 1028).nullable()
    var otherDetails = varchar("otherDetails", 1028).nullable()
    var compressedFileUrl = varchar("compressedFileUrl", 1028).nullable()

    var createdTimestamp = varchar("createdTimestamp", 19)
        .default(LocalDateTime.now().toString().take(19))

    var updatedTimestamp = varchar("updatedTimestamp", 19)
        .default(LocalDateTime.now().toString().take(19))

    enum class Status {
        PENDING, COMPLETED, CANCELLED, IN_PROGRESS, FAILED;

        companion object {
            fun toStatus(status: String): Status {
                return try {
                    Status.valueOf(status)
                } catch (_: Exception) { PENDING }
            }
        }
    }

}

