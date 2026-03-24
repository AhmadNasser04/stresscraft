package dev.cubxity.tools.stresscraft.web

data class AddServerRequest(
    val id: String,
    val host: String,
    val port: Int = 25565,
    val count: Int = 50,
    val delay: Int = 20,
    val buffer: Int = 20,
    val prefix: String = "Player"
)