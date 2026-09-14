package com.eclipsett.shrinker.service

import com.eclipsett.shrinker.compression.FfmpegService
import com.eclipsett.shrinker.compression.FileService
import com.eclipsett.shrinker.compression.constant.CompressionLevel
import com.eclipsett.shrinker.model.TaskDetail
import com.eclipsett.shrinker.model.entities.TaskTable
import com.eclipsett.shrinker.repository.TaskRepository
import com.eclipsett.shrinker.repository.dbEvents.DBEvent
import com.eclipsett.shrinker.repository.dbEvents.EntityChangeNotifier
import com.eclipsett.shrinker.service.util.extractFileNameFromLink
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.net.URI
import java.net.URLDecoder
import java.util.*

@Service
class TaskProcessingService(
    private val ffmpegService: FfmpegService, private val fileService: FileService,
    private val repository: TaskRepository, private val notifier: EntityChangeNotifier,
) {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val tasksQueue: HashMap<UUID, DBEvent> = HashMap()
    private var taskQueueJob: Job? = null
    private var currentProcessingTask: Pair<UUID, Job>? = null

    init {
        scope.launch {
            delay(10000)
            notifier.events.collect { event ->
                log.info("DB event collected: {}", event::class)

                if (event is DBEvent.Created && (event.task.bpp == null || event.task.bpp > 0.08)) {
                    tasksQueue[event.task.id] = event
                }

                if (event is DBEvent.Delete) tasksQueue[event.task.id] = event

                if (event is DBEvent.Update) {
                    val oldTask = tasksQueue[event.task.id] as? DBEvent.Update

                    // Add a new updated task
                    if (oldTask == null) {
                        tasksQueue[event.task.id] = event
                        return@collect
                    }

                    // update an old task
                    val validStatus = listOf(TaskTable.Status.PENDING, TaskTable.Status.CANCELLED)
                    if (event.task.status != oldTask.task.status && event.task.status in validStatus) {
                        tasksQueue[event.task.id] = event
                    }
                }

                // Start processing tasksQueue if taskQueueJob is not active
                if (taskQueueJob == null || taskQueueJob?.isActive != true) processTaskQueue()
            }
        }
    }

    private fun processTaskQueue() {
        taskQueueJob = scope.launch {
            log.info("Task Queue has started processing")
            tasksQueue.forEach { (id, event) ->
                val taskJob = when (event) {
                    is DBEvent.Created -> scope.launch { processCreatedEvent(event.task) }
                    is DBEvent.Update -> scope.launch { processUpdatedEvent(event.task) }
                    is DBEvent.Delete -> scope.launch { processDeleteEvent(event.task) }
                }
                currentProcessingTask = id to taskJob
            }
        }
    }

    private suspend fun processCreatedEvent(task: TaskDetail) {
        try {
            log.error("advance processTask enter <> Task {} is being processed", task.id)

            val videoMetaData = ffmpegService.getVideoDetails(task)
            val derivedName = try {
                videoMetaData?.get("format")?.get("tags")?.get("title")?.asString() ?:
                    extractFileNameFromLink(task.originalUrl ?: "") ?: ""
            } catch (_: Exception) { "" }
            val fileSize = try {
                videoMetaData?.get("format")["size"]?.asString()?.toLong() ?: 0L
            } catch (_: Exception) { 0L }
            val otherDetails = videoMetaData?.get("format")?.toPrettyString()?.replace("\n", "")

            val outputPath = fileService.getOutputFile(task.derivedName ?: task.name, task.id.toString())
            val updatedTask = repository.updateTask(
                task.id, task.copy(
                    derivedName = derivedName, originalFileSize = fileSize,
                    otherDetails = otherDetails, outputFilePath = outputPath
                )
            ) ?: return

            log.debug("advance processTask <> update task - updatedTask: {},", updatedTask)

            ffmpegService.startCompression(
                dbTask = updatedTask, metaData = videoMetaData, outputPath = outputPath,
                level = CompressionLevel.toCompressionLevel(task.compressionLevel)
            )
        } catch (ex: Exception) { log.error("advance processTask error: {}", ex.message) } finally {
            tasksQueue.remove(task.id) // remove precessed task
        }
    }

    private suspend fun processDeleteEvent(task: TaskDetail) {
        log.info("processDeleteEvent <> Task id: {}", task.id)
        // Cancel an active task if a user deletes it
        if (currentProcessingTask?.first == task.id) currentProcessingTask?.second?.cancel()
        tasksQueue.remove(task.id) // delete task from queue
        val filePath = task.outputFilePath ?: return
        try { File(File(filePath).parent).deleteRecursively() // Delete locally file
        } catch (_: Exception) { }
    }

    private suspend fun processUpdatedEvent(task: TaskDetail) {
        log.info("processUpdatedEvent <> Task id: {}", task.id)
        processDeleteEvent(task) // Clean up the old task and process if the updated status is PENDING
        if (task.status == TaskTable.Status.PENDING) processCreatedEvent(task)
    }

    @PreDestroy
    fun shutDown() = scope.cancel()

}


