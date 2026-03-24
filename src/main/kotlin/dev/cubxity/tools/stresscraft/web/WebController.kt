package dev.cubxity.tools.stresscraft.web

import dev.cubxity.tools.stresscraft.StressCraftOptions
import dev.cubxity.tools.stresscraft.server.ServerController
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/servers")
class WebController(
    private val controller: ServerController
) {

    @GetMapping
    fun listServers(): List<ServerStatus> {
        return controller.all().map { (id, target) ->
            val sc = target.stressCraft
            ServerStatus(
                id = id,
                host = target.host,
                port = target.port,
                running = target.running,
                targetCount = target.options.count,
                connections = sc?.sessionCount?.get() ?: 0,
                players = sc?.activeSessions?.get() ?: 0,
                chunks = sc?.chunksLoaded?.get() ?: 0,
                tps = sc?.calculateAverageTps()?.let { if (it.isNaN()) null else it },
                delay = target.options.delay,
                prefix = target.options.prefix
            )
        }
    }

    @PostMapping
    fun addServer(@RequestBody req: AddServerRequest): ResponseEntity<Any> {
        return try {
            val options = StressCraftOptions(req.count, req.delay, req.buffer, req.prefix)
            controller.addServer(req.id, req.host, req.port, options)
            ResponseEntity.ok(mapOf("status" to "added", "id" to req.id))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @DeleteMapping("/{id}")
    fun removeServer(@PathVariable id: String): ResponseEntity<Any> {
        return if (controller.removeServer(id)) {
            ResponseEntity.ok(mapOf("status" to "removed"))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/{id}/start")
    fun startServer(@PathVariable id: String): ResponseEntity<Any> {
        return when {
            controller.server(id) == null -> ResponseEntity.notFound().build()
            controller.startServer(id) -> ResponseEntity.ok(mapOf("status" to "started"))
            else -> ResponseEntity.badRequest().body(mapOf("error" to "Already running"))
        }
    }

    @PostMapping("/{id}/stop")
    fun stopServer(@PathVariable id: String): ResponseEntity<Any> {
        return when {
            controller.server(id) == null -> ResponseEntity.notFound().build()
            controller.stopServer(id) -> ResponseEntity.ok(mapOf("status" to "stopped"))
            else -> ResponseEntity.badRequest().body(mapOf("error" to "Not running"))
        }
    }

    @PutMapping("/{id}/count")
    fun setCount(@PathVariable id: String, @RequestBody req: SetCountRequest): ResponseEntity<Any> {
        return try {
            controller.setCount(id, req.count)
            ResponseEntity.ok(mapOf("status" to "updated", "count" to req.count))
        } catch (_: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @PutMapping("/{id}/delay")
    fun setDelay(@PathVariable id: String, @RequestBody req: SetDelayRequest): ResponseEntity<Any> {
        return try {
            controller.setDelay(id, req.delay)
            ResponseEntity.ok(mapOf("status" to "updated", "delay" to req.delay))
        } catch (_: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }
}
