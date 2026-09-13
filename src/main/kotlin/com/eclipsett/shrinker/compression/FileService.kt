package com.eclipsett.shrinker.compression

import com.eclipsett.shrinker.model.TaskDetail
import com.eclipsett.shrinker.repository.ShrinkerDB
import org.springframework.stereotype.Service
import java.io.File

@Service
class FileService {

    private val appDir = ShrinkerDB.appDir
    val tempDir = File("${appDir.absolutePath}/files")

    fun getOutputFile(dbTask: TaskDetail): String {
        tempDir.mkdirs()
        val finalDir = File(tempDir, dbTask.id.toString())
        finalDir.mkdirs()

        val name = dbTask.derivedName ?: dbTask.name
        val file = File("${finalDir.absolutePath}/$name.mp4")
        file.createNewFile()
        return file.absolutePath
    }

    fun deleteFile(filePath: String) {
        File(filePath).delete()
    }

}

