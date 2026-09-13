package com.eclipsett.shrinker.compression

import com.eclipsett.shrinker.compression.constant.CompressionLevel
import com.eclipsett.shrinker.model.TaskDetail
import com.eclipsett.shrinker.model.entities.TaskTable
import com.eclipsett.shrinker.model.util.toDetail
import com.eclipsett.shrinker.remote_storage.S3StorageService
import com.eclipsett.shrinker.repository.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.io.File
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Service

class FfmpegService(private val repository: TaskRepository, private val s3StorageService: S3StorageService) {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    private data class CompressionJob(val process: Process, val outputPath: String)
    private val jobs = ConcurrentHashMap<UUID, CompressionJob>()

    fun getVideoDetails(dbTask: TaskDetail): JsonNode? {
        val command = listOf(
            "ffprobe", "-v", "error",   // "error" instead of "quiet" — still suppresses noise, but shows real failures
            "-print_format", "json",
            "-show_format", "-show_streams",
            dbTask.originalUrl
        )
        val process = ProcessBuilder(command).redirectErrorStream(false).start()

        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val finished = process.waitFor(30, TimeUnit.SECONDS)

        if (!finished) {
            process.destroyForcibly()
            log.error("getVideoDetails <> ffprobe timed out for url: ${dbTask.originalUrl}")
            return null
        }

        if (process.exitValue() != 0 || stdout.isBlank()) {
            log.error("getVideoDetails <> ffprobe failed (exit=${process.exitValue()}): $stderr")
            return null
        }

        return ObjectMapper().readTree(stdout)
    }

    fun startCompression(
        dbTask: TaskDetail, outputPath: String, level: CompressionLevel, metaData: JsonNode?
    ): UUID {
        log.debug("startCompression <> dbTask: {}, outputPath: {}, CompressionLevel: {}", dbTask, outputPath, level)
        repository.updateTask(
            dbTask.id, dbTask.copy(status = TaskTable.Status.IN_PROGRESS, outputFilePath = outputPath)
        )

        val totalDurationSeconds = metaData?.get("format")["duration"]?.asDouble() ?: 0.0
        val command = mutableListOf("ffmpeg", "-i", dbTask.originalUrl ?: "", "-y")
        command += level.flags
        command += outputPath

        val processBuilder = ProcessBuilder(command).redirectErrorStream(true)
            .redirectInput(ProcessBuilder.Redirect.from(File("/dev/null")))
        val logs = StringBuilder()
        val process = processBuilder.start()

        // ffmpeg prints progress lines like: "frame=  120 fps=30 ... time=00:01:23.45 bitrate=..."
        val timeRegex = Regex("""time=(\d{2}):(\d{2}):(\d{2})\.(\d{2})""")
        var lastReportedProgress = -1

        Thread {
            process.inputStream.bufferedReader().forEachLine { line ->
                logs.appendLine(line)
                if (totalDurationSeconds > 0) {
                    timeRegex.find(line)?.let { match ->
                        val (h, m, s, cs) = match.destructured
                        val elapsedSeconds = h.toInt() * 3600 + m.toInt() * 60 + s.toInt() + cs.toInt() / 100.0
                        val progress = ((elapsedSeconds / totalDurationSeconds) * 100).toInt().coerceIn(0, 100)

                        // only hit the DB when progress actually changes, not on every log line
                        if (progress != lastReportedProgress) {
                            lastReportedProgress = progress
                            val updated = repository.getTask(dbTask.id)?.toDetail() ?: return@let
                            repository.updateTask(updated.id, updated.copy(progress = progress))
                            log.debug("startCompression <> update db progress - progress: {},", progress)
                        }
                    }
                }
            }
        }.start()

        process.onExit().thenAccept { p ->
            var updated = repository.getTask(dbTask.id)?.toDetail()
                ?.copy(
                    status = if (p.exitValue() == 0) TaskTable.Status.COMPLETED else TaskTable.Status.FAILED,
                    progress = if (p.exitValue() == 0) 100 else lastReportedProgress.coerceAtLeast(0),
                    updatedTimestamp = LocalDateTime.now().toString()
                )
                ?: return@thenAccept
            if (p.exitValue() != 0) updated = updated.copy(errors = logs.toString().take(1028))
            repository.updateTask(updated.id, updated)

            if (p.exitValue() == 0) {
                val objectId = "${dbTask.id}/${dbTask.name}.mp4"
                s3StorageService.uploadToS3(objectId, outputPath)
                val updated = repository.getTask(dbTask.id)?.toDetail()
                    ?.copy(
                        objectId = objectId,
                        outputFilePath = outputPath,
                        updatedTimestamp = LocalDateTime.now().toString(),
                        finalFileSize = File(outputPath).length()
                    ) ?: return@thenAccept
                repository.updateTask(updated.id, updated)
                log.info("startCompression completed <> output file: $outputPath")
            }

        }

        jobs[dbTask.id] = CompressionJob(process, outputPath)
        return dbTask.id
    }

    fun stopProcess(jobId: UUID) {
        val job = jobs[jobId]
        job?.let {
            it.process.destroy().also { log.info("Job $jobId cancelled.") }
            File(it.outputPath).delete()
        }
        jobs.remove(jobId)
    }

}
