package kr.devport.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Composition root. Component, entity, and repository scanning is explicit so the Kotlin
 * domain modules under `kr.devport.api` and the gitrepo modules under `kr.devport.gitrepo`
 * are composed into one bootable application.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EntityScan(basePackages = ["kr.devport.api", "kr.devport.gitrepo"])
@EnableJpaRepositories(basePackages = ["kr.devport.api", "kr.devport.gitrepo"])
class DevportApiApplication

fun main(args: Array<String>) {
    runApplication<DevportApiApplication>(*args)
}
