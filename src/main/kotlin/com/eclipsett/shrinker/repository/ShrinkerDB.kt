package com.eclipsett.shrinker.repository

import org.jetbrains.exposed.v1.jdbc.Database
import java.io.File
import java.sql.DriverManager

class ShrinkerDB private constructor() {
   private val dbFile = File("${appDir.absolutePath}/db", "shrinker_task.db")

    init {
        File(dbFile.parent).mkdirs()
        DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
        Database.connect("jdbc:sqlite:${dbFile.absolutePath}", "org.sqlite.JDBC")
    }

    companion object {
        val appDir: File = if (System.getProperty("java.home").contains("Users/mac"))
            File(System.getProperty("user.dir"), "/Shrinker")
        else File("/app/Shrinker")

        private var INSTANCE: ShrinkerDB? = null

        fun init() {
            if (INSTANCE == null) INSTANCE = ShrinkerDB()
            else println("Storage has already been initialized.")
        }

        fun get(): ShrinkerDB = INSTANCE ?: throw IllegalStateException("Database must be initialized first.")

    }

}