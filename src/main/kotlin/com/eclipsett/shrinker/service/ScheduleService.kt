package com.eclipsett.shrinker.service

import com.eclipsett.shrinker.model.TaskDetail
import com.eclipsett.shrinker.model.entities.TaskEntity
import com.eclipsett.shrinker.model.entities.TaskTable
import com.eclipsett.shrinker.remote_storage.S3StorageService
import com.eclipsett.shrinker.repository.TaskRepository
import com.eclipsett.shrinker.service.util.parseDateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

@Component
class ScheduleService(private val repository: TaskRepository, private val s3StorageService: S3StorageService) {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(cron = "0 0 * * * *")
    fun cleanupStaleUploads() {
        log.info("Cleaning up stale tasks")

        repository.getTaskAllTask { it.status != TaskTable.Status.IN_PROGRESS }.forEach { task ->
            if (task.status in listOf(TaskTable.Status.CANCELLED, TaskTable.Status.FAILED)) {
                deleteTask(task)
            }

            if (task.status in listOf(TaskTable.Status.COMPLETED, TaskTable.Status.PENDING)) {
                val duration = Duration.between(parseDateTime(task.createdTimestamp), LocalDateTime.now())
                if (duration.toHours() >= 24L) deleteTask(task)
            }
        }
    }

    private fun deleteTask(task: TaskDetail) {
        task.objectId?.let { s3StorageService.deleteFile(it) }
        task.outputFilePath?.let { File(File(it).parent).deleteRecursively() }
        repository.deleteTask(task.id)
    }

}