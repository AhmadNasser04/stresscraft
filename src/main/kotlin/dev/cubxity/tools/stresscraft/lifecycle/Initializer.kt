package dev.cubxity.tools.stresscraft.lifecycle

interface Initializer {
    suspend fun initialize()
}