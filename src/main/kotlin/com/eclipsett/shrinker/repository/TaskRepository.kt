package com.eclipsett.shrinker.repository

import com.eclipsett.shrinker.model.TaskDetail
import com.eclipsett.shrinker.model.dto.TaskRequestDTO
import com.eclipsett.shrinker.model.entities.TaskEntity
import com.eclipsett.shrinker.model.entities.TaskTable
import com.eclipsett.shrinker.model.util.toDetail
import com.eclipsett.shrinker.model.util.toEntity
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteIgnoreWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TaskRepository {

    init { transaction { SchemaUtils.create(TaskTable) } }

    fun createTask(requestDTO: TaskRequestDTO): TaskEntity {
        return transaction {
            requestDTO.toEntity()
        }
    }

    fun getTask(id: UUID): TaskEntity? {
        return transaction {
            TaskEntity[id]
        }
    }

    fun deleteTask(id: UUID) {
        transaction {
            TaskTable.deleteIgnoreWhere {
                TaskTable.id eq id
            }
        }
    }

    fun updateTask(id: UUID, dto: TaskDetail): TaskDetail? {
        return transaction {
            TaskEntity.findByIdAndUpdate(id) {
                it.bpp = dto.bpp
                it.status = dto.status
                it.errors = dto.errors
                it.verdict = dto.verdict
                it.objectId = dto.objectId
                it.progress = dto.progress
                it.derivedName = dto.derivedName
                it.otherDetails = dto.otherDetails
                it.originalUrl = dto.originalUrl ?: ""
                it.finalFileSize = dto.finalFileSize
                it.outputFilePath = dto.outputFilePath
                it.compressedFileUrl = dto.compressedFileUrl
                it.originalFileSize = dto.originalFileSize
                it.updatedTimestamp = dto.updatedTimestamp
            }?.toDetail()
        }
    }

    fun getTaskByStatus(status: TaskTable.Status): List<TaskEntity> {
        return transaction {
            TaskEntity.find { TaskTable.status eq status }.toList()
        }

    }

}

