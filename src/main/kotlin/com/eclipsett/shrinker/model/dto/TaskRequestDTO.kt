package com.eclipsett.shrinker.model.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class TaskRequestDTO(
    @NotBlank(message = "Name cannot be empty")
    @Size(min = 3, max = 128, message = "Name must be between 3 and 128 characters")
    val name: String,

    @NotBlank(message = "OriginalUrl cannot be empty")
    @Size(min = 3, max = 1024, message = "OriginalUrl must be between 3 and 1024 characters")
    val originalUrl: String,

    var compressionLevel: String = "Mild"
)