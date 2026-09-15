package com.eclipsett.shrinker.model.dto

import com.eclipsett.shrinker.repository.dbEvents.DBEvent
import java.util.UUID

data class ActiveProcessingTaskDTO(
    val tasksQueue: Map<UUID, DBEvent> = emptyMap(),
    val activeProcessingTaskID: String? = null,
    val isTaskQueueJobActive: Boolean = false
)