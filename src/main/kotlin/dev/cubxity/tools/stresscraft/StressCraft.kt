package dev.cubxity.tools.stresscraft

import dev.cubxity.tools.stresscraft.data.StressCraftSession
import dev.cubxity.tools.stresscraft.module.TickingModule
import kotlinx.coroutines.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil
import kotlin.system.measureNanoTime

class StressCraft(
    val host: String,
    val port: Int,
    private val tickers: List<TickingModule>,
    @Volatile var options: StressCraftOptions,
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var id = 0

    private val _sessions = CopyOnWriteArrayList<StressCraftSession>()

    val sessionCount = AtomicInteger()
    val activeSessions = AtomicInteger()
    val chunksLoaded = AtomicInteger()

    val sessions: List<StressCraftSession>
        get() = _sessions

    fun start() {
        coroutineScope.launch {
            while (true) {
                try {
                    val sessions = sessionCount.get()
                    val active = activeSessions.get()
                    if (sessions < options.count && sessions - active < options.buffer) {
                        createSession()
                    }
                } catch (_: Throwable) {
                    // no-op
                }
                delay(options.delay.toLong())
            }
        }

        coroutineScope.launch {
            while (true) {
                val time = measureNanoTime {
                    try {
                        for (session in _sessions) {
                            for (module in tickers) {
                                module.tick(session)
                            }
                        }
                    } catch (_: Throwable) {
                        // no-op
                    }
                }
                delay(50 - ceil(time / 1E6).toLong())
            }
        }
    }

    fun stop() {
        coroutineScope.coroutineContext.job.cancel()
        for (session in _sessions) {
            try {
                session.session.disconnect("StressCraft stopped")
            } catch (_: Throwable) {
                // no-op
            }
        }
        _sessions.clear()
        sessionCount.set(0)
        activeSessions.set(0)
        chunksLoaded.set(0)
    }

    fun setCount(count: Int) {
        options = options.copy(count = count)
        if (count < _sessions.size) {
            val excess = _sessions.size - count
            val toRemove = _sessions.takeLast(excess)
            for (session in toRemove) {
                try {
                    session.session.disconnect("Scaling down")
                } catch (_: Throwable) {
                    // no-op
                }
            }
        }
    }

    fun removeSession(session: StressCraftSession) {
        _sessions.remove(session)
    }

    fun calculateAverageTps(): Double {
        var count = 0
        var total = 0.0
        for (session in _sessions) {
            if (session.timer.hasData) {
                count++
                total += session.timer.tps
            }
        }
        return if (count > 0) total / count else Double.NaN
    }

    private fun createSession() {
        val name = options.prefix + "${id++}".padStart(4, '0')
        val session = StressCraftSession(this)
        _sessions.add(session)
        session.connect(name)
    }
}
