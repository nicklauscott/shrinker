package com.eclipsett.shrinker.service

import com.eclipsett.shrinker.compression.FileService
import com.eclipsett.shrinker.repository.ShrinkerDB
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Service

@Service
class ResourceCleanUpService(private val fileService: FileService) {

    @PreDestroy
    fun cleanUp() {
        println("Spring Boot is shutting down! Deleting app storage...")
        if (System.getProperty("java.home").contains("Users/mac")) ShrinkerDB.appDir.deleteRecursively()
        else fileService.tempDir.deleteRecursively()
    }

}