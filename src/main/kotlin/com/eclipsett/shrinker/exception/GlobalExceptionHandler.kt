package com.eclipsett.shrinker.exception

import com.eclipsett.shrinker.model.dto.TaskRequestDTO
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.sqlite.SQLiteErrorCode
import org.sqlite.SQLiteException
import tools.jackson.databind.exc.InvalidFormatException
import tools.jackson.databind.exc.MismatchedInputException
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ValidationErrorResponse> {
        val errors = HashMap<String, String>()

        ex.bindingResult.allErrors.forEach({ error ->
            val fieldName = (error as FieldError).field
            val errorMessage = error.defaultMessage
            errors.put(fieldName, errorMessage ?: "")
        })

        val body = ValidationErrorResponse(
            LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(),
            "Validation Failed", errors
        )

        return ResponseEntity(body, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(InvalidStatusNumber::class)
    fun handleInvalidStatusNumber(ex: InvalidStatusNumber): ResponseEntity<MalformedJsonErrorResponse> {
        val errors = HashMap<String, String>()

        val body = MalformedJsonErrorResponse(
            LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(),
            ex.error, ex.message
        )

        return ResponseEntity(body, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<MalformedJsonErrorResponse> {
        var targetType: Class<*>? = null

        when (val cause = ex.cause) {
            is InvalidFormatException -> { targetType = cause.targetType }
            is MismatchedInputException -> { targetType = cause.targetType }
            else -> {}
        }

        val errorResponse = MalformedJsonErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Validation Failed",
            message = getMessage(targetType),
        )

        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    private fun getMessage(targetClass: Class<*>?): String {
        return when (targetClass) {
            TaskRequestDTO::class.java -> "Name and OriginalUrl fields are required"
            else -> ""
        }
    }


    @ExceptionHandler(SQLiteException::class)
    fun handleSQLiteException(ex: SQLiteException): ResponseEntity<Map<String, Any>> {
        // SQLITE_CONSTRAINT_UNIQUE maps to result code 2067 or standard error code 19 (SQLITE_CONSTRAINT)
        val isUniqueViolation = ex.resultCode == SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE
                || ex.message?.contains("UNIQUE constraint failed") == true

        val userFriendlyMessage = if (isUniqueViolation && ex.message?.contains("task.originalUrl") == true) {
            "The URL you provided has already been processed."
        } else if (isUniqueViolation) {
            "A unique database constraint rule was broken."
        } else {
            "A database storage error occurred."
        }

        val body = mapOf(
            "timestamp" to LocalDateTime.now(),
            "status" to HttpStatus.CONFLICT.value(), // 409 Conflict
            "error" to "Conflict",
            "message" to userFriendlyMessage
        )

        return ResponseEntity(body, HttpStatus.CONFLICT)
    }
}