package com.eclipsett.shrinker.service

import com.eclipsett.shrinker.model.entities.TaskTable
import com.eclipsett.shrinker.remote_storage.S3StorageService
import com.eclipsett.shrinker.repository.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.File

@Component
class ScheduleService(private val repository: TaskRepository, private val s3StorageService: S3StorageService) {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(cron = "0 0 * * * *")
    fun cleanupStaleUploads() {
        log.info("Cleaning up stale tasks")
        val cancelledAndFailedTask =
            repository.getTaskByStatus(TaskTable.Status.CANCELLED) +
            repository.getTaskByStatus(TaskTable.Status.FAILED)

        cancelledAndFailedTask.forEach { task ->
            task.objectId?.let { s3StorageService.deleteFile(it) }
            task.outputFilePath?.let { File(File(it).parent).deleteRecursively() }
            repository.deleteTask(task.id.value)
        }
    }

}