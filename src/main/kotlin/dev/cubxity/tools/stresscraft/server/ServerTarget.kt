package dev.cubxity.tools.stresscraft.server

import dev.cubxity.tools.stresscraft.StressCraft
import dev.cubxity.tools.stresscraft.StressCraftOptions

data class ServerTarget(
    val id: String,
    val host: String,
    val port: Int,
    var options: StressCraftOptions,
    var stressCraft: StressCraft? = null,
    var running: Boolean = false
)
