package com.eclipsett.shrinker.repository

import com.eclipsett.shrinker.model.TaskDetail
import com.eclipsett.shrinker.model.dto.TaskRequestDTO
import com.eclipsett.shrinker.model.entities.TaskEntity
import com.eclipsett.shrinker.model.entities.TaskTable
import com.eclipsett.shrinker.model.util.toDetail
import com.eclipsett.shrinker.model.util.toEntity
import com.eclipsett.shrinker.repository.dbEvents.DBEvent
import com.eclipsett.shrinker.repository.dbEvents.EntityChangeNotifier
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Service
import java.util.*


@Service
class TaskRepository(private val notifier: EntityChangeNotifier) {

    init { transaction { SchemaUtils.create(TaskTable) } }

    fun createTask(requestDTO: TaskRequestDTO, bpp: Double? = null, verdict: String? = null): TaskEntity {
        return transaction {
            val entity = requestDTO.toEntity(bpp, verdict)
            notifier.publishNonSuspendEvent(DBEvent.Created(entity.toDetail()))
            entity
        }
    }

    fun getTask(id: UUID): TaskEntity? {
        return transaction {
            TaskEntity[id]
        }
    }

    fun deleteTask(taskId: UUID, sendEvent: Boolean = false): Boolean {
        return transaction {
            val tempTask = TaskEntity[taskId]
            val rows = TaskTable.deleteWhere { TaskTable.id eq taskId }
            if (sendEvent && rows != 0)
                notifier.publishNonSuspendEvent(DBEvent.Delete(tempTask.toDetail()))
            rows != 0
        }
    }

    fun updateTask(id: UUID, dto: TaskDetail, sendEvent: Boolean = false): TaskDetail? {
        return transaction {
            val task = TaskEntity.findByIdAndUpdate(id) {
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
            if (sendEvent) {
                task?.let { notifier.publishNonSuspendEvent(DBEvent.Update(it)) }
            }
            task
        }
    }

    fun getTaskByStatus(status: TaskTable.Status): List<TaskEntity> {
        return transaction {
            TaskEntity.find { TaskTable.status eq status }.toList()
        }
    }

    fun getTaskAllTask(predicate: (TaskEntity) -> Boolean): List<TaskEntity> {
        return transaction {
            buildList { addAll(TaskEntity.all().filter(predicate)) }
        }
    }

}

