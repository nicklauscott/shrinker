package com.eclipsett.shrinker.compression

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class CompressionValidator {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    data class BppResult(val bpp: Double, val verdict: String)
    private data class TechnicalDetails(
        val bitRate: Long = 0L, val width: Int = -1, val height: Int = -1, val frameRate: Double = 0.0
    )

    private fun getVideoTechnicalDetails(videoLink: String): TechnicalDetails? {
        val command = listOf(
            "ffprobe", "-v", "error",
            "-select_streams", "v:0",
            "-show_entries", "stream=width,height,codec_name,bit_rate,r_frame_rate",
            "-of", "default=noprint_wrappers=1",
            videoLink
        )
        val process = ProcessBuilder(command).redirectErrorStream(false).start()

        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val finished = process.waitFor(30, TimeUnit.SECONDS)

        if (!finished) {
            process.destroyForcibly()
            log.error("getVideoTechnicalDetails <> ffprobe timed out for url: $videoLink")
        }

        if (process.exitValue() != 0 || stdout.isBlank()) {
            log.error("getVideoTechnicalDetails <> ffprobe failed (exit=${process.exitValue()}): $stderr")

        }

        var tDetails: TechnicalDetails? = TechnicalDetails()
        stdout.split("\n").forEach {
            try {
                val keyValue = it.split("=")
                when(keyValue.firstOrNull()) {
                    "bit_rate" -> tDetails = tDetails?.copy(bitRate = keyValue.last().toLong())
                    "height" -> tDetails = tDetails?.copy(height = keyValue.last().toInt())
                    "width" -> tDetails = tDetails?.copy(width = keyValue.last().toInt())
                    "r_frame_rate" -> tDetails = tDetails?.copy(frameRate = parseFrameRate(keyValue.last()))
                    else -> {}
                }
            } catch (_: Exception) { tDetails = null }
        }

        return tDetails

    }

    fun calculateBpp(videoLink: String): BppResult? {
        val tDetails = getVideoTechnicalDetails(videoLink) ?: return null
        val bitRate = tDetails.bitRate; val width = tDetails.width;
        val height = tDetails.height ; val frameRate = tDetails.frameRate
        parseFrameRate("tDetails: $tDetails")
        if (width <= 0 || height <= 0 || frameRate <= 0) return null

        val bpp = bitRate.toDouble() / (width * height * frameRate)

        val verdict = when {
            bpp < 0.05 -> "Near the floor — likely already well compressed, further shrinking will cost visible quality"
            bpp < 0.08 -> "Fairly efficient — some room to compress, but diminishing returns"
            bpp < 0.15 -> "Moderate — reasonable room to compress without much visible loss"
            else -> "High — likely real room to compress further"
        }
        return BppResult(bpp, verdict)
    }

    // Helper to parse ffprobe's "30/1" style frame rate string
    private fun parseFrameRate(rFrameRate: String): Double {
        return try {
            val (num, den) = rFrameRate.split("/").map { it.toDouble() }
            if (den == 0.0) 0.0 else num / den
        } catch (_: Exception) { 0.0 }
    }

}
