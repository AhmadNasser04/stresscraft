# StressCraft

State-of-the-art Minecraft stress testing software written in Kotlin. Features a native desktop GUI (Compose for Desktop) with a REST API for custom integrations.

## Disclaimer

StressCraft should **ONLY** be used in your own server environment. We do not endorse the use of StressCraft for any other purposes than testing your own infrastructure.

Attempting to execute this against an external server can be seen as **illegal** as it simulates a layer 7 DoS (denial-of-service) attack, which is against the law in most countries.

## Setup

> **NOTE:** DO NOT DO THIS IN PRODUCTION, EVER.

### Server Configuration (server.properties)

- Set `max-players` high enough for the number of bots you're testing
- Set `online-mode` to `false`
- Set `allow-flight` to `true` (prevents bots from being kicked for floating)
- Set `network-compression-threshold` to `-1`

### Bukkit/Paper Configuration

- Set `connection-throttle` (bukkit.yml) to `-1`
- Increase `max-joins-per-tick` (paper.yml) to your liking

### Velocity Configuration (velocity.toml)

If running behind a Velocity proxy:

- Set `login-ratelimit` to `0`
- Set `force-key-authentication` to `false` (required for unsigned bot commands/chat)
- Set `online-mode` to `false` (for bot connections)

## Building & Running

```bash
./gradlew bootJar
java -jar build/libs/stresscraft-0.0.1.jar
```

This launches the native desktop window and starts the API server on `http://localhost:8080`.

## Features

- Native desktop GUI (Compose for Desktop) with real-time stats
- REST API at `/api/servers` for custom frontends and automation
- Manage multiple server targets simultaneously
- Dynamically adjust player count and join delay per server
- Start/stop stress tests on the fly
- Bot movement with physics simulation (gravity, walking, jumping)
- Chat flooding
- Real-time TPS, connection, player, and chunk monitoring

## API

The REST API runs on port 8080 alongside the desktop app.

| Method   | Endpoint                  | Description                 |
|----------|---------------------------|-----------------------------|
| `GET`    | `/api/servers`            | List all servers with stats |
| `POST`   | `/api/servers`            | Add a server target         |
| `DELETE` | `/api/servers/{id}`       | Remove a server             |
| `POST`   | `/api/servers/{id}/start` | Start stress test           |
| `POST`   | `/api/servers/{id}/stop`  | Stop stress test            |
| `PUT`    | `/api/servers/{id}/count` | Update player count         |
| `PUT`    | `/api/servers/{id}/delay` | Update join delay           |

## Roadmap

- [x] Performant stresser
- [x] Native desktop GUI
- [x] REST API
- [x] Multi-server support
- [x] Random movements with physics simulation
- [x] Chat flooding
- [x] Dynamic player count adjustment
- [ ] Velocity forwarding support
- [ ] Scripting support
- [ ] Prometheus metrics exporter
- [ ] Dockerfile
