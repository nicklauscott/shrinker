package com.eclipsett.shrinker.compression

import com.eclipsett.shrinker.repository.ShrinkerDB
import org.springframework.stereotype.Service
import java.io.File

@Service
class FileService {

    private val appDir = ShrinkerDB.appDir
    val tempDir = File("${appDir.absolutePath}/files")

    fun getOutputFile(name: String, taskId: String): String {
        tempDir.mkdirs()
        val finalDir = File(tempDir, taskId)
        finalDir.mkdirs()

        val file = File("${finalDir.absolutePath}/$name.mp4")
        file.createNewFile()
        return file.absolutePath
    }

    fun deleteFile(filePath: String) {
        File(filePath).delete()
    }

}

