package com.eclipsett.shrinker.exception

import java.time.LocalDateTime

data class ValidationErrorResponse(
     val timestamp: LocalDateTime,
     val status: Int,
     val error: String,
     val errors: Map<String, String>
)

data class MalformedJsonErrorResponse(
     val timestamp: LocalDateTime = LocalDateTime.now(),
     val status: Int,
     val error: String,
     val message: String,
)
