package com.eclipsett.shrinker.repository.dbEvents

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.springframework.stereotype.Service

@Service
class EntityChangeNotifier {

    private val _events: MutableSharedFlow<DBEvent> = MutableSharedFlow(
        replay = 0, extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<DBEvent> = _events.asSharedFlow()


    suspend fun publishEvent(dbEvent: DBEvent) {
        _events.emit(dbEvent)
    }

    fun publishNonSuspendEvent(dbEvent: DBEvent) {
        _events.tryEmit(dbEvent)
    }

}