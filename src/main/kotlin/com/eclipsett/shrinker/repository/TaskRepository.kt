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

    fun createTask(requestDTO: TaskRequestDTO, bpp: Double? = null, verdict: String? = null): TaskDetail? {
        val task =  transaction { requestDTO.toEntity(bpp, verdict) }
        task?.let { notifier.publishNonSuspendEvent(DBEvent.Created(it)) }
        return task
    }

    fun getTask(taskId: UUID): TaskEntity? =
        try { transaction { TaskEntity[taskId] } } catch (_: Exception) { null }

    fun deleteTask(taskId: UUID, sendEvent: Boolean = false): Boolean {
        val tempTask = getTask(taskId) ?: return false
        val rows = transaction { TaskTable.deleteWhere { TaskTable.id eq tempTask.id } }
        if (sendEvent && rows != 0) notifier.publishNonSuspendEvent(DBEvent.Delete(tempTask.toDetail()))
        return rows != 0
    }

    fun updateTask(dto: TaskDetail, sendEvent: Boolean = false): TaskDetail? {
        val up = transaction { TaskEntity.findByIdAndUpdate(dto.id) {
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
        } }
        if (sendEvent && up != null) notifier.publishNonSuspendEvent(DBEvent.Update(up.toDetail()))
        return up?.toDetail()
    }

    fun getTaskByStatus(status: TaskTable.Status): List<TaskDetail> {
        return transaction { TaskEntity.find { TaskTable.status eq status }.toList().map { it.toDetail() } }
    }

    fun getTaskAllTask(predicate: (TaskEntity) -> Boolean): List<TaskDetail> {
        return transaction { buildList { addAll(TaskEntity.all().filter(predicate).map { it.toDetail() }) } }
    }

}

