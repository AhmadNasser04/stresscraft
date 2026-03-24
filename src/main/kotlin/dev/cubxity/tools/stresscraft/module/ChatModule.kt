package dev.cubxity.tools.stresscraft.module

import dev.cubxity.tools.stresscraft.data.StressCraftSession
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket
import org.springframework.stereotype.Component
import kotlin.random.Random

/**
 * Floods chat using /me commands (ServerboundChatCommandPacket).
 *
 * Uses the unsigned command packet which doesn't require chat signing
 * and works through Velocity proxy without decode errors.
 * /say requires no permissions on most servers in offline mode.
 */
@Component
class ChatModule : TickingModule {
    private val counters = HashMap<StressCraftSession, Int>()

    override fun tick(session: StressCraftSession) {
        if (!session.isActive) return

        val count = counters.getOrPut(session) { Random.nextInt(100) }
        counters[session] = count + 1

        // Each bot sends roughly every 3-8 seconds, staggered
        val interval = 60 + (session.hashCode().and(0x7F))
        if (count % interval != 0) return

        val message = MESSAGES[Random.nextInt(MESSAGES.size)]

        session.session.send(ServerboundChatCommandPacket("me $message"))
    }

    companion object {
        private val MESSAGES = listOf(
            "hello everyone!",
            "anyone wanna play?",
            "this server is awesome",
            "gg",
            "lol nice",
            "where is spawn?",
            "how do i get out of here",
            "brb",
            "testing 123",
            "hey",
            "whats up",
            "first time here",
            "nice builds",
            "anyone online?",
            "lag check",
            "cool server",
            "hello world",
            "good morning",
            "haha",
            "can someone help me?",
            "where are the diamonds",
            "lets go mining",
            "watch out creeper!",
            "oof",
            "i just died lol",
            "where is everyone",
            "this is fun",
            "how many players online?",
            "nice weather today",
            "yo"
        )
    }
}
