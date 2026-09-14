package com.eclipsett.shrinker.repository.dbEvents

import com.eclipsett.shrinker.model.TaskDetail

sealed interface DBEvent {
    data class Created(val task: TaskDetail): DBEvent
    data class Update(val task: TaskDetail): DBEvent
    data class Delete(val task: TaskDetail): DBEvent
}