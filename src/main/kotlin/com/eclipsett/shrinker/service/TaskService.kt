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
import java.io.File
import java.util.UUID

@Service
class TaskService(
    private val repository: TaskRepository, private val compressionValidator: CompressionValidator,
    private val s3StorageService: S3StorageService
) {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun submitTask(requestDTO: TaskRequestDTO): TaskDetailDTO {
        val bppResult = compressionValidator.calculateBpp(requestDTO.originalUrl)
        val updatedTask = repository.createTask(requestDTO).toDetail()
            .copy(bpp = bppResult?.bpp, verdict = bppResult?.verdict)
        repository.updateTask(updatedTask.id, updatedTask)
        return updatedTask.toDTO()
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

    fun getTaskById(id: String): TaskDetailDTO? {
        return try {
            val task = repository.getTask(UUID.fromString(id))?.toDetail()?.toDTO() ?: return null
            val presignedRequest = task.objectId?.let { s3StorageService.getPresignedUrl(it) }
            task.copy(presignedUrl = presignedRequest?.presignedUrl, urlExpiration = presignedRequest?.expirationDate)
        } catch (_: Exception) { null }
    }

    fun stopTaskById(id: String) {
        try {
            log.error("TaskService stopTaskById")
            val task = repository.getTask(UUID.fromString(id))?.toDetail() ?: return
            val updatedTask = task.copy(
                status = TaskTable.Status.CANCELLED,
                originalUrl = "${('A'..'z').random()}" +
                        "${('A'..'z').random()}```" + task.originalUrl?.drop(5)
            )
            task.objectId?.let { s3StorageService.deleteFile(it) }
            task.outputFilePath?.let { File(File(it).parent).deleteRecursively() }
            repository.updateTask(updatedTask.id, updatedTask)
        } catch (ex: Exception) { log.error("\n\nTaskService ex: ${ex.printStackTrace()}") }
    }

}