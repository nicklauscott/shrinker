package com.eclipsett.shrinker.model.dto

import java.util.UUID

data class TaskResponseDTO(
    val id: UUID,
    val name: String,

    val bpp: Double? = null,
    val verdict: String? = null,

    val createdAt: String
)