package dev.cubxity.tools.stresscraft.server

import dev.cubxity.tools.stresscraft.StressCraft
import dev.cubxity.tools.stresscraft.StressCraftOptions
import dev.cubxity.tools.stresscraft.module.TickingModule
import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Controller
import java.util.concurrent.ConcurrentHashMap

@Controller
class ServerController(
    private val tickers: List<TickingModule>
) : DisposableBean {
    private val servers = ConcurrentHashMap<String, ServerTarget>()

    fun all(): Map<String, ServerTarget> = servers
    fun server(id: String): ServerTarget? = servers[id]

    fun addServer(id: String, host: String, port: Int, options: StressCraftOptions): ServerTarget {
        require(!servers.containsKey(id)) { "Server '$id' already exists" }
        val target = ServerTarget(id, host, port, options)
        servers[id] = target
        return target
    }

    fun removeServer(id: String): Boolean {
        val target = servers.remove(id) ?: return false
        if (target.running) {
            target.stressCraft?.stop()
        }
        return true
    }

    fun startServer(id: String): Boolean {
        val target = servers[id] ?: return false
        if (target.running) return false
        val sc = StressCraft(
            host = target.host,
            port = target.port,
            options = target.options,
            tickers = tickers
        )
        target.stressCraft = sc
        target.running = true
        sc.start()
        return true
    }

    fun stopServer(id: String): Boolean {
        val target = servers[id] ?: return false
        if (!target.running) return false
        target.stressCraft?.stop()
        target.stressCraft = null
        target.running = false
        return true
    }

    fun setCount(id: String, count: Int) {
        val target = servers[id] ?: throw IllegalArgumentException("Server '$id' not found")
        target.options = target.options.copy(count = count)
        target.stressCraft?.setCount(count)
    }

    fun setDelay(id: String, delay: Int) {
        val target = servers[id] ?: throw IllegalArgumentException("Server '$id' not found")
        target.options = target.options.copy(delay = delay)
        target.stressCraft?.let { it.options = it.options.copy(delay = delay) }
    }

    override fun destroy() {
        for ((_, target) in servers) {
            if (target.running) {
                target.stressCraft?.stop()
            }
        }
        servers.clear()
    }
}
