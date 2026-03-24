package dev.cubxity.tools.stresscraft.module

import dev.cubxity.tools.stresscraft.data.StressCraftSession
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundPlayerInputPacket
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand
import org.springframework.stereotype.Component
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Simulates realistic player movement with basic gravity physics.
 *
 * Each bot has a state machine that cycles through behaviors:
 * - IDLE: stand still, look around occasionally
 * - WALKING: move in a direction at walking speed
 * - JUMPING: apply upward velocity (combined with walking)
 *
 * Physics constants match vanilla Minecraft to avoid anti-cheat kicks.
 */
@Component
class MovementModule : TickingModule {
    private val states = HashMap<StressCraftSession, BotState>()

    override fun tick(session: StressCraftSession) {
        if (!session.isActive) return

        val state = states.getOrPut(session) { BotState() }
        state.tickCounter++

        // Record spawn Y on first tick so we know ground level
        if (!state.initialized) {
            state.spawnY = session.y
            state.initialized = true
        }

        // Apply gravity
        applyPhysics(session, state)

        // State machine
        state.ticksInState++
        if (state.ticksInState >= state.stateDuration) {
            transition(state)
        }

        when (state.behavior) {
            Behavior.IDLE -> tickIdle(session, state)
            Behavior.WALKING -> tickWalking(session, state)
            Behavior.JUMPING -> tickJumping(session, state)
        }

        // Send position every tick to keep server happy
        sendPosition(session)

        // Swing arm occasionally
        if (Random.nextInt(200) == 0) {
            session.session.send(ServerboundSwingPacket(Hand.MAIN_HAND))
        }
    }

    private fun applyPhysics(session: StressCraftSession, state: BotState) {
        if (!session.onGround) {
            // Apply gravity
            session.velY -= GRAVITY
            // Apply drag
            session.velY *= (1.0 - DRAG)
        }

        // Apply velocity
        session.y += session.velY

        // Simple ground collision: don't fall below spawn Y
        // (We don't have block data, so use spawn Y as ground reference)
        if (session.y <= state.spawnY && session.velY <= 0) {
            session.y = state.spawnY
            session.velY = 0.0
            session.onGround = true
        } else if (session.velY < 0) {
            session.onGround = false
        }
    }

    private fun transition(state: BotState) {
        state.ticksInState = 0
        val roll = Random.nextInt(100)
        when {
            roll < 40 -> {
                // 40%: idle for 2-5 seconds
                state.behavior = Behavior.IDLE
                state.stateDuration = Random.nextInt(40, 100)
            }

            roll < 85 -> {
                // 45%: walk in a random direction for 1-4 seconds
                state.behavior = Behavior.WALKING
                state.walkAngle = Random.nextDouble() * Math.PI * 2
                state.stateDuration = Random.nextInt(20, 80)
            }

            else -> {
                // 15%: jump (single jump, then transitions)
                state.behavior = Behavior.JUMPING
                state.stateDuration = 10
            }
        }
    }

    private fun tickIdle(session: StressCraftSession, state: BotState) {
        // Look around randomly every ~2 seconds
        if (Random.nextInt(40) == 0) {
            session.yaw += Random.nextFloat() * 60f - 30f
            session.pitch = (Random.nextFloat() * 40f - 20f).coerceIn(-60f, 60f)
        }

        // Send input: not moving
        if (state.tickCounter % 4 == 0) {
            session.session.send(
                ServerboundPlayerInputPacket(
                    false, false, false, false, false, false, false
                )
            )
        }
    }

    private fun tickWalking(session: StressCraftSession, state: BotState) {
        // Small direction adjustments for natural-looking movement
        if (Random.nextInt(20) == 0) {
            state.walkAngle += (Random.nextDouble() - 0.5) * 0.5
        }

        // Move at walking speed
        val dx = cos(state.walkAngle) * WALK_SPEED
        val dz = sin(state.walkAngle) * WALK_SPEED

        session.x += dx
        session.z += dz
        session.yaw = Math.toDegrees(state.walkAngle).toFloat()

        // Send forward input
        if (state.tickCounter % 4 == 0) {
            session.session.send(
                ServerboundPlayerInputPacket(
                    true, false, false, false, false, false, false
                )
            )
        }
    }

    private fun tickJumping(session: StressCraftSession, state: BotState) {
        // Apply jump velocity on first tick of jump
        if (state.ticksInState == 0 && session.onGround) {
            session.velY = JUMP_VELOCITY
            session.onGround = false
        }

        // Send jump input on first tick
        if (state.ticksInState == 0) {
            session.session.send(
                ServerboundPlayerInputPacket(
                    false, false, false, false, true, false, false
                )
            )
        }
    }

    private fun sendPosition(session: StressCraftSession) {
        session.session.send(
            ServerboundMovePlayerPosRotPacket(
                session.onGround, false,
                session.x, session.y, session.z,
                session.yaw, session.pitch
            )
        )
    }

    data class BotState(
        var walkAngle: Double = 0.0,
        var ticksInState: Int = 0,
        var stateDuration: Int = 60,
        var behavior: Behavior = Behavior.IDLE,
        var tickCounter: Int = 0,
        var spawnY: Double = 0.0,
        var initialized: Boolean = false
    )

    enum class Behavior {
        IDLE,
        JUMPING,
        WALKING,
    }

    companion object {
        const val GRAVITY = 0.08
        const val DRAG = 0.02
        const val WALK_SPEED = 0.098
        const val JUMP_VELOCITY = 0.42
    }
}
