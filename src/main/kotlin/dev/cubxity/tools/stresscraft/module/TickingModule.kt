package dev.cubxity.tools.stresscraft.module

import dev.cubxity.tools.stresscraft.data.StressCraftSession

interface TickingModule {
    fun tick(session: StressCraftSession)
}