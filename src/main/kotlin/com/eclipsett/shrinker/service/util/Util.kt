package com.eclipsett.shrinker.service.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import java.net.URI
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import kotlin.text.dropLastWhile
import java.time.format.DateTimeFormatter

private val log: Logger = LoggerFactory.getLogger("com.eclipsett.shrinker.service.util.Util.kt")


fun extractFileNameFromLink(url: String): String? {
    return try {
        val uri = URI(url)

        // 1. Try the "filename" query parameter first (common on download links)
        val queryFileName = uri.query
            ?.split("&")
            ?.map { it.split("=", limit = 2) }
            ?.firstOrNull { it.size == 2 && it[0].equals("filename", ignoreCase = true) }
            ?.get(1)
            ?.let { URLDecoder.decode(it, "UTF-8") }

        if (!queryFileName.isNullOrBlank()) return queryFileName.dropLastWhile { it != '.' }.dropLast(1)

        // 2. Fall back to the last segment of the path
        val pathFileName = uri.path
            ?.trimEnd('/')
            ?.substringAfterLast('/')
            ?.let { URLDecoder.decode(it, "UTF-8") }

        pathFileName?.takeIf { it.isNotBlank() }?.dropLastWhile { it != '.' }?.dropLast(1)
    } catch (_: Exception) {
        null
    }
}

fun parseDateTime(timeStamp: String): LocalDateTime {
    return try {
        LocalDateTime.parse(timeStamp)
    } catch (ex: DateTimeParseException) {
        log.warn("Failed to parse createdTimestamp '{}', treating as stale", timeStamp, ex)
        LocalDateTime.now().minusDays(5)
    }
}

fun formatDateTime(timeStamp: String): String {
    return try {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        parseDateTime(timeStamp).format(formatter)
    } catch (ex: DateTimeParseException) {
        log.warn("Failed to format date and time {}", ex.message)
        ""
    }
}