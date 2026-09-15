package com.eclipsett.shrinker.model.entities

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import java.util.UUID

class TaskEntity(id: EntityID<UUID>): UUIDEntity(id) {
    companion object: UUIDEntityClass<TaskEntity>(TaskTable)

    var name by TaskTable.name
    var status by TaskTable.status
    var userEMail by TaskTable.userEMail
    var errors by TaskTable.errors
    var objectId by TaskTable.objectId
    var progress by TaskTable.progress
    var otherDetails by TaskTable.otherDetails
    var bpp by TaskTable.bpp
    var verdict by TaskTable.verdict
    var originalUrl by TaskTable.originalUrl
    var derivedName by TaskTable.derivedName
    var finalFileSize by TaskTable.finalFileSize
    var outputFilePath by TaskTable.outputFilePath
    var originalFileSize by TaskTable.originalFileSize
    var compressionLevel by TaskTable.compressionLevel
    var compressedFileUrl by TaskTable.compressedFileUrl
    var createdTimestamp by TaskTable.createdTimestamp
    var updatedTimestamp by TaskTable.updatedTimestamp

}

