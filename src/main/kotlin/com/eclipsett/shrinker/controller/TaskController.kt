package com.eclipsett.shrinker.controller

import com.eclipsett.shrinker.model.dto.TaskDetailDTO
import com.eclipsett.shrinker.model.dto.TaskRequestDTO
import com.eclipsett.shrinker.model.dto.TaskResponseDTO
import com.eclipsett.shrinker.model.util.toDTO
import com.eclipsett.shrinker.model.util.toResponse
import com.eclipsett.shrinker.service.TaskProcessingService
import com.eclipsett.shrinker.service.TaskService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/task")
@Tag(name = "Task", description = "Api for managing tasks")
class TaskController(
    private val service: TaskService, private val processingService: TaskProcessingService
) {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping
    @Operation(description = "Submit new task")
    fun submitTask(@Valid @RequestBody requestDTO: TaskRequestDTO): ResponseEntity<TaskResponseDTO> {
        val task = service.submitTask(requestDTO)

        // Start asynchronous processing
        if (task.bpp == null || task.bpp > 0.08) { processingService.processTask(task.id) }

        // return immediately
        return ResponseEntity.accepted().body(task.toResponse())
    }

    @GetMapping("/status={statusValue}")
    @Operation(description = "Get tasks by status")
    fun getTasksByStatus(@PathVariable statusValue: String): ResponseEntity<List<TaskDetailDTO>> {
        return ResponseEntity.ok()
            .body(service.getTasksByStatus(statusValue).map { it.toDTO() })
    }

    @GetMapping("/{id}")
    @Operation(description = "Get tasks by id")
    fun getTasksById(@PathVariable("id") idString: String): ResponseEntity<TaskDetailDTO> {
        val task = service.getTaskById(idString)
        return if (task != null) ResponseEntity.ok().body(task)
        else ResponseEntity(HttpStatus.NOT_FOUND)
    }

    @DeleteMapping("/{id}")
    @Operation(description = "Delete tasks by id")
    fun stopTasksById(@PathVariable("id") idString: String): ResponseEntity<*> {
        val id = try {
            UUID.fromString(idString)
        } catch (_: Exception) { null } ?: return ResponseEntity("", HttpStatus.GONE)

        processingService.stopTask(id) // Start asynchronous processing

        service.stopTaskById(id.toString())
        print("stopTasksById stopTaskById")
        return ResponseEntity("", HttpStatus.GONE)
    }

}