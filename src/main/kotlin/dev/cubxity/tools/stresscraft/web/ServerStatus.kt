package dev.cubxity.tools.stresscraft.web

data class ServerStatus(
    val id: String,
    val host: String,
    val port: Int,
    val running: Boolean,
    val targetCount: Int,
    val connections: Int,
    val players: Int,
    val chunks: Int,
    val tps: Double?,
    val delay: Int,
    val prefix: String
)