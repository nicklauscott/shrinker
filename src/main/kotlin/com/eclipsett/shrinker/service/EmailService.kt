package com.eclipsett.shrinker.service

import com.eclipsett.shrinker.model.TaskDetail
import com.eclipsett.shrinker.model.util.formatFileSize
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.ResourceLoader
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.nio.file.Files

@Service
class EmailService(private val mailSender: JavaMailSender, private val resourceLoader: ResourceLoader) {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun sendMail(to: String, subject: String, content: String) {
        val message = mailSender.createMimeMessage()
        val messageHelper = MimeMessageHelper(message, true, "UTF-8")
        messageHelper.setFrom("Shrinker") // test
        messageHelper.setTo(to)
        messageHelper.setSubject(subject)
        messageHelper.setText(content, true)
        mailSender.send(message)
        log.info("File Processing Complete email sent to {}", to)
    }

    fun processAndSendEmail(task: TaskDetail) {
        try {
            log.info("Processing email: task: {}", task.userEMail)
            val userEMail = task.userEMail ?: return
            val resource = resourceLoader
                .getResource("classpath:" + "templates/File_process_complete_email_template.html")
            var emailContent = Files.readString(resource.file.toPath())
            mapData(task).forEach { (key, value) ->
                emailContent = emailContent.replace(key, value)
            }
            sendMail(userEMail, "File Processing Complete", emailContent)
        } catch (ex: Exception) {
            log.info("Error occurred while processing email. {}", ex.message)
        }
    }

    private fun mapData(task: TaskDetail): Map<String, String> {
        val obj = ObjectMapper().readTree(task.otherDetails ?: "")
        val data = HashMap<String, String>()
        data["{{userName}}"] = ""
        data["{{fileName}}"] = task.name
        data["{{originalFileSize}}"] = formatFileSize(task.originalFileSize ?: 0)
        data["{{finalFileSize}}"] = formatFileSize(task.finalFileSize ?: 0)
        data["{{spaceSavedPercent}}"] = percentSaved(task.originalFileSize, task.finalFileSize).toString()
        data["{{compressionLevel}}"] = task.compressionLevel
        data["{{mediaDuration}}"] = secondsToTimeString(obj?.get("duration")?.asString() ?: "")
        data["{{status}}"] = task.status.toString()
        data["{{completedAt}}"] = task.updatedTimestamp
        data["{{downloadUrl}}"] = task.compressedFileUrl ?: ""
        data["{{urlExpiration}}"] = "4 hrs"
        return data
    }

    private fun secondsToTimeString(secondsStr: String): String {
        val totalSeconds = secondsStr.toDouble()
        val hours = (totalSeconds / 3600).toInt()
        val minutes = ((totalSeconds % 3600) / 60).toInt()
        val seconds = (totalSeconds % 60).toInt()
        val millis = ((totalSeconds - totalSeconds.toInt()) * 1000).toInt()
        return if (hours > 0) {
            "%02d:%02d:%02d.%03d".format(hours, minutes, seconds, millis)
        } else {
            "%02d:%02d.%03d".format(minutes, seconds, millis)
        }
    }

    private fun percentSaved(originalSize: Long?, finalSize: Long?): Double {
        val original = originalSize ?: return 0.0
        val final = finalSize ?: return 0.0
        if (original <= 0L) return 0.0
        return (1.0 - (final.toDouble() / original.toDouble())) * 100
    }

}