package com.eclipsett.shrinker.service

import com.eclipsett.shrinker.compression.FfmpegService
import com.eclipsett.shrinker.compression.FileService
import com.eclipsett.shrinker.compression.constant.CompressionLevel
import com.eclipsett.shrinker.model.util.toDetail
import com.eclipsett.shrinker.repository.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.*

@Service
class TaskProcessingService(
    private val repository: TaskRepository,
    private val ffmpegService: FfmpegService, private val fileService: FileService,
) {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    @Async
    fun processTask(taskId: UUID) {
        try {
            log.error("processTask enter")
            val task = repository.getTask(taskId)?.toDetail() ?: return

            val videoMetaData = ffmpegService.getVideoDetails(task)
            val derivedName = try {
                //videoMetaData["format"]["filename"]?.asString()?.split("/")[1]?.split(".")[0] ?: ""
                videoMetaData?.get("format")["filename"]?.asString()?.takeLastWhile { it != '/' }?.split(".")[0] ?: ""
            } catch (_: Exception) { "" }
            val fileSize = try {
                videoMetaData?.get("format")["size"]?.asString()?.toLong() ?: 0L
            } catch (_: Exception) { 0L }
            val otherDetails = videoMetaData?.get("format")?.toPrettyString()?.replace("\n", "")

            val updatedTask = repository.updateTask(
                taskId, task.copy(derivedName = derivedName, originalFileSize = fileSize, otherDetails = otherDetails)
            ) ?: return

            log.debug("processTask <> update task - updatedTask: {},", updatedTask)

            ffmpegService.startCompression(
                dbTask = updatedTask, metaData = videoMetaData,
                outputPath = fileService.getOutputFile(updatedTask),
                level = CompressionLevel.toCompressionLevel(task.compressionLevel),
            )
        } catch (ex: Exception) { log.error("processTask error: {}", ex.message) }

    }

    @Async
    fun stopTask(taskId: UUID) {
        ffmpegService.stopProcess(taskId)
    }

}
