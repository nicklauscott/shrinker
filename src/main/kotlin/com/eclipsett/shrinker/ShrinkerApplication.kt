package com.eclipsett.shrinker

import com.eclipsett.shrinker.repository.ShrinkerDB
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class ShrinkerApplication

fun main(args: Array<String>) {
	ShrinkerDB.init()
	runApplication<ShrinkerApplication>(*args)
}
