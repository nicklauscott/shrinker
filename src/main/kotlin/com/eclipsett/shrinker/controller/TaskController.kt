package com.eclipsett.shrinker.controller

import com.eclipsett.shrinker.model.dto.ActiveProcessingTaskDTO
import com.eclipsett.shrinker.model.dto.TaskDetailDTO
import com.eclipsett.shrinker.model.dto.TaskRequestDTO
import com.eclipsett.shrinker.model.dto.TaskResponseDTO
import com.eclipsett.shrinker.model.util.toDTO
import com.eclipsett.shrinker.model.util.toResponse
import com.eclipsett.shrinker.service.TaskService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/task")
@Tag(name = "Task", description = "Api for managing tasks")
class TaskController(private val service: TaskService) {

    @PostMapping
    @Operation(description = "Submit new task")
    fun submitTask(@Valid @RequestBody requestDTO: TaskRequestDTO): ResponseEntity<TaskResponseDTO> {
        val task = service.submitTask(requestDTO)
        return if (task == null) {
            ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        } else ResponseEntity(task.toResponse(), HttpStatus.CREATED)
    }

    @GetMapping("/status={statusValue}")
    @Operation(description = "Get tasks by status")
    fun getTasksByStatus(@PathVariable statusValue: String): ResponseEntity<List<TaskDetailDTO>> {
        return ResponseEntity.ok()
            .body(service.getTasksByStatus(statusValue).map { it.toDTO() })
    }

    @GetMapping("/all")
    @Operation(description = "Get all tasks")
    fun getAllTask(): ResponseEntity<List<TaskDetailDTO>> {
        return ResponseEntity.ok().body(service.getAllTask().map { it.toDTO() })
    }

    @PatchMapping("/{id}")
    @Operation(description = "Retry a task")
    fun retryTask(@PathVariable("id") idString: String): ResponseEntity<*> {
        service.retryTask(idString) ?: return ResponseEntity("", HttpStatus.NOT_FOUND)
        return ResponseEntity("", HttpStatus.ACCEPTED)
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
        return if ( service.stopAndDeleTaskById(idString)) ResponseEntity("", HttpStatus.GONE)
        else ResponseEntity("", HttpStatus.NOT_FOUND)
    }

    @GetMapping("/active")
    @Operation(description = "Get active task processing details")
    fun getActiveProcessingTask(): ActiveProcessingTaskDTO {
        return service.getActiveProcessingTask()
    }

}