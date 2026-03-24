package dev.cubxity.tools.stresscraft.data

import org.geysermc.mcprotocollib.protocol.MinecraftProtocol
import org.geysermc.mcprotocollib.protocol.data.game.ClientCommand
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundResourcePackPushPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundForgetLevelChunkPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetTimePacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundResourcePackPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket
import org.geysermc.mcprotocollib.network.Session
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter
import org.geysermc.mcprotocollib.network.packet.Packet
import org.geysermc.mcprotocollib.network.factory.ClientNetworkSessionFactory
import dev.cubxity.tools.stresscraft.StressCraft
import dev.cubxity.tools.stresscraft.util.ServerTimer
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundPingPacket
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundPongPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatAckPacket

class StressCraftSession(
    private val app: StressCraft
) : SessionAdapter() {
    private var wasAlive = false
    private var wasActive = false
    private var previousChunkCount = 0

    private val chunks = HashSet<Long>()
    private var _session: Session? = null

    val timer = ServerTimer()

    val session: Session
        get() = _session ?: error("session has not initialized")

    // Position & physics
    var x: Double = 0.0
    var y: Double = 0.0
    var z: Double = 0.0
    var yaw: Float = 0f
    var pitch: Float = 0f
    var velX: Double = 0.0
    var velY: Double = 0.0
    var velZ: Double = 0.0
    var onGround: Boolean = true
    var isActive: Boolean = false
        private set

    // Chat message tracking (for acknowledgements)
    var lastChatOffset: Int = 0

    fun connect(name: String) {
        val protocol = MinecraftProtocol(name)
        val session = ClientNetworkSessionFactory.factory()
            .setAddress(app.host, app.port)
            .setProtocol(protocol)
            .create()

        session.addListener(this)

        app.sessionCount.incrementAndGet()
        wasAlive = true
        try {
            _session = session
            session.connect()
        } catch (_: Throwable) {
            handleDisconnect()
        }
    }

    override fun packetReceived(session: Session, packet: Packet) {
        when (packet) {
            is ClientboundLoginPacket -> {
                if (!wasActive) {
                    app.activeSessions.incrementAndGet()
                    wasActive = true
                    isActive = true
                }
            }
            is ClientboundRespawnPacket -> {
                chunks.clear()
                app.chunksLoaded.addAndGet(-previousChunkCount)
                previousChunkCount = 0
                onGround = true
                velX = 0.0; velY = 0.0; velZ = 0.0
            }
            is ClientboundSetHealthPacket -> {
                if (packet.health <= 0) {
                    session.send(ServerboundClientCommandPacket(ClientCommand.RESPAWN))
                }
            }
            is ClientboundPlayerPositionPacket -> {
                val pos = packet.position
                x = pos.x
                y = pos.y
                z = pos.z
                yaw = packet.yRot
                pitch = packet.xRot
                // Reset velocity on server teleport
                val delta = packet.deltaMovement
                velX = delta.x
                velY = delta.y
                velZ = delta.z
                onGround = false
                session.send(ServerboundAcceptTeleportationPacket(packet.id))
            }
            is ClientboundLevelChunkWithLightPacket -> {
                chunks.add(computeKey(packet.x, packet.z))
                val size = chunks.size
                app.chunksLoaded.addAndGet(size - previousChunkCount)
                previousChunkCount = size
            }
            is ClientboundForgetLevelChunkPacket -> {
                chunks.remove(computeKey(packet.x, packet.z))
                val size = chunks.size
                app.chunksLoaded.addAndGet(size - previousChunkCount)
                previousChunkCount = size
            }
            is ClientboundSetTimePacket -> {
                timer.onWorldTimeUpdate(packet.gameTime)
            }
            is ClientboundResourcePackPushPacket -> {
                app.options.acceptResourcePacks
                    .let { ServerboundResourcePackPacket(packet.id, it) }
                    .let(session::send)
            }
            is ClientboundPingPacket -> {
                session.send(ServerboundPongPacket(packet.id))
            }
            is ClientboundPlayerChatPacket -> {
                // Track received messages for acknowledgement
                lastChatOffset++
                if (lastChatOffset % 20 == 0) {
                    session.send(ServerboundChatAckPacket(lastChatOffset))
                }
            }
        }
    }

    override fun disconnected(event: DisconnectedEvent?) {
        handleDisconnect()
    }

    private fun computeKey(x: Int, z: Int): Long =
        x.toLong().shl(32) or (z.toLong().and(0xFFFFFFFFL))

    private fun handleDisconnect() {
        app.removeSession(this)
        if (wasAlive) {
            app.sessionCount.decrementAndGet()
            wasAlive = false
        }
        if (wasActive) {
            app.activeSessions.decrementAndGet()
            wasActive = false
        }
        isActive = false
        chunks.clear()
        app.chunksLoaded.addAndGet(-previousChunkCount)
        previousChunkCount = 0
        _session = null
    }
}
