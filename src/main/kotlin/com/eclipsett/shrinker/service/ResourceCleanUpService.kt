package com.eclipsett.shrinker.service

import com.eclipsett.shrinker.compression.FileService
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Service

@Service
class ResourceCleanUpService(private val fileService: FileService) {

    @PreDestroy
    fun cleanUp() {
        println("Spring Boot is shutting down! Deleting app storage...")
        fileService.tempDir.deleteRecursively()
    }

}