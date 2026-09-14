package com.eclipsett.shrinker.service

import com.eclipsett.shrinker.compression.CompressionValidator
import com.eclipsett.shrinker.exception.InvalidStatusNumber
import com.eclipsett.shrinker.model.TaskDetail
import com.eclipsett.shrinker.model.dto.TaskDetailDTO
import com.eclipsett.shrinker.model.dto.TaskRequestDTO
import com.eclipsett.shrinker.model.entities.TaskTable
import com.eclipsett.shrinker.model.util.toDTO
import com.eclipsett.shrinker.model.util.toDetail
import com.eclipsett.shrinker.remote_storage.S3StorageService
import com.eclipsett.shrinker.repository.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TaskService(
    private val repository: TaskRepository, private val compressionValidator: CompressionValidator,
    private val s3StorageService: S3StorageService
) {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun submitTask(requestDTO: TaskRequestDTO): TaskDetailDTO {
        val bppResult = compressionValidator.calculateBpp(requestDTO.originalUrl)
        val updatedTask = repository.createTask(requestDTO, bppResult?.bpp, verdict = bppResult?.verdict)
        return updatedTask.toDetail().toDTO()
    }

    fun retryTask(idString: String): UUID? {
        val id = try { UUID.fromString(idString) } catch (_: Exception) { null } ?: return null
        val dbTask = repository.getTask(id) ?: return null
        return repository
            .updateTask(id, dbTask.toDetail().copy(status = TaskTable.Status.PENDING), true)?.id
    }

    fun getTasksByStatus(statusValue: String): List<TaskDetail> {
        val status = if (statusValue.length == 1 && statusValue.first().isDigit()) {
            if (statusValue.toInt() !in 1..TaskTable.Status.entries.size)
                throw InvalidStatusNumber(
                    "Status number should be between 1 and ${TaskTable.Status.entries.size}",
                    "${statusValue.toInt()} is not a valid staus number"
                )
            TaskTable.Status.entries[statusValue.toInt() - 1]
        } else TaskTable.Status.toStatus(statusValue)

        return repository.getTaskByStatus(status)
            .map { it.toDetail() }
    }

    fun getAllTask(): List<TaskDetail> {
        return repository.getTaskAllTask { true }.map { it.toDetail() }
    }

    fun getTaskById(id: String): TaskDetailDTO? {
        return try {
            val task = repository.getTask(UUID.fromString(id))?.toDetail()?.toDTO() ?: return null
            val presignedRequest = task.objectId?.let { s3StorageService.getPresignedUrl(it) }
            task.copy(presignedUrl = presignedRequest?.presignedUrl, urlExpiration = presignedRequest?.expirationDate)
        } catch (_: Exception) { null }
    }

    fun stopAndDeleTaskById(id: String): Boolean {
       return try {
            val id = try { UUID.fromString(id) } catch (_: Exception) { null } ?: return false
            return repository.deleteTask(id, true)
        } catch (_: Exception) { false }
    }

}