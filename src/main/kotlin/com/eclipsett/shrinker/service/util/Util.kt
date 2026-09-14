package com.eclipsett.shrinker.service.util

import java.net.URI
import java.net.URLDecoder
import kotlin.text.dropLastWhile


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
