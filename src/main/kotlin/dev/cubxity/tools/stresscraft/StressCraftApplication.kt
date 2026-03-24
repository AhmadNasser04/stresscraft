package dev.cubxity.tools.stresscraft

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class StressCraftApplication

fun main(args: Array<String>) {
    runApplication<StressCraftApplication>(*args) {
        setHeadless(false)
    }
}
