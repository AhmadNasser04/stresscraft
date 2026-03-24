package dev.cubxity.tools.stresscraft

import dev.cubxity.tools.stresscraft.lifecycle.Initializer
import dev.cubxity.tools.stresscraft.log.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class StressCraftRunner(
    private val initializers: List<Initializer>,
) : ApplicationRunner, CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private val logger by logger()

    override fun run(args: ApplicationArguments) {
        initializers.forEach { initializer ->
            launch(Dispatchers.IO) {
                runCatching {
                    initializer.initialize()
                }
                    .onFailure { logger.error("Failed to run initializer task ${initializer::class.simpleName}", it) }
            }
        }
    }
}