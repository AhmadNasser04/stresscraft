package dev.cubxity.tools.stresscraft.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.cubxity.tools.stresscraft.StressCraftOptions
import dev.cubxity.tools.stresscraft.lifecycle.Initializer
import dev.cubxity.tools.stresscraft.server.ServerController
import kotlinx.coroutines.delay
import org.springframework.stereotype.Component

@Component
class StressCraftWindow(
    private val controller: ServerController
) : Initializer {
    override suspend fun initialize() {
        System.setProperty("java.awt.headless", "false")

        application {
            Window(
                onCloseRequest = { controller.destroy(); exitApplication() },
                title = "StressCraft",
                state = rememberWindowState(width = 960.dp, height = 680.dp),
            ) {
                MaterialTheme(colorScheme = StressCraftColors) { App(controller) }
            }
        }
    }
}

@Composable
fun App(controller: ServerController) {
    var servers by remember { mutableStateOf(listOf<ServerSnapshot>()) }
    LaunchedEffect(Unit) {
        while (true) {
            servers = controller.all().map { (id, t) ->
                val sc = t.stressCraft
                ServerSnapshot(
                    id, t.host, t.port, t.running, t.options.count,
                    sc?.sessionCount?.get() ?: 0, sc?.activeSessions?.get() ?: 0,
                    sc?.chunksLoaded?.get() ?: 0,
                    sc?.calculateAverageTps()?.let { if (it.isNaN()) null else it },
                    t.options.delay, t.options.prefix,
                )
            }
            delay(800)
        }
    }

    Column(Modifier.fillMaxSize().background(Canvas)) {
        TitleBar(servers)
        Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AddServerBar(controller)
            if (servers.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No targets", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Lo)
                        Spacer(Modifier.height(4.dp))
                        Text("Add a server above to begin.", fontSize = 13.sp, color = Lo.copy(alpha = .6f))
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                    items(servers, key = { it.id }) { s -> ServerCard(s, controller) }
                }
            }
        }
    }
}

@Composable
fun TitleBar(servers: List<ServerSnapshot>) {
    val conns = servers.sumOf { it.connections }
    val players = servers.sumOf { it.players }
    val running = servers.count { it.running }

    Row(
        Modifier.fillMaxWidth().height(48.dp).background(Panel).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(20.dp).clip(RoundedCornerShape(5.dp)).background(Emerald),
                contentAlignment = Alignment.Center
            ) {
                Text("S", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Black)
            }
            Text("StressCraft", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Hi)
        }
        if (servers.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                HeaderStat("$running", "active", if (running > 0) Emerald else Lo)
                HeaderStat("$players", "players", Sky)
                HeaderStat("$conns", "conns", Mid)
            }
        }
    }
}

@Composable
fun HeaderStat(value: String, label: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accent, fontFamily = FontFamily.Monospace)
        Text(label, fontSize = 11.sp, color = Lo)
    }
}

@Composable
fun AddServerBar(controller: ServerController) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("25565") }
    var count by remember { mutableStateOf("50") }
    var delayMs by remember { mutableStateOf("20") }
    var prefix by remember { mutableStateOf("Player") }

    Row(
        Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(10.dp)).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Field("Name", name, { name = it }, Modifier.width(110.dp), placeholder = "Lobby")
        Field("Host", host, { host = it }, Modifier.weight(1f), placeholder = "127.0.0.1")
        Field("Port", port, { port = it }, Modifier.width(72.dp))
        Field("Count", count, { count = it }, Modifier.width(60.dp))
        Field("Delay", delayMs, { delayMs = it }, Modifier.width(60.dp))
        Field("Prefix", prefix, { prefix = it }, Modifier.width(100.dp))
        HoverButton("Add", Emerald, Color.Black, filled = true) {
            if (name.isNotBlank() && host.isNotBlank()) {
                try {
                    controller.addServer(
                        name.trim(), host.trim(), port.toIntOrNull() ?: 25565,
                        StressCraftOptions(
                            count.toIntOrNull() ?: 50,
                            delayMs.toIntOrNull() ?: 20,
                            20,
                            prefix.ifBlank { "Player" })
                    )
                    name = ""; host = ""
                } catch (_: Exception) {
                    // no op
                }
            }
        }
    }
}

@Composable
fun ServerCard(server: ServerSnapshot, controller: ServerController) {
    val hover = remember { MutableInteractionSource() }
    val isHovered by hover.collectIsHoveredAsState()

    val borderColor by animateColorAsState(
        when {
            server.running && isHovered -> Emerald.copy(alpha = .45f)
            server.running -> Emerald.copy(alpha = .22f)
            isHovered -> Subtle
            else -> Divider
        },
        animationSpec = tween(200)
    )

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .background(Panel)
            .hoverable(hover)
    ) {
        // Head
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Pulsing dot for running
                val dotColor by animateColorAsState(if (server.running) Emerald else Lo, tween(300))
                Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
                Text(server.id, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Hi)
                Badge("${server.host}:${server.port}")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (server.running) {
                    HoverButton("Stop", Rose, Rose) { controller.stopServer(server.id) }
                } else {
                    HoverButton("Start", Emerald, Emerald) { controller.startServer(server.id) }
                }
                HoverButton("Delete", Rose, Rose) { controller.removeServer(server.id) }
            }
        }

        // Metrics — fixed height so all boxes are equal
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Metric("Connections", "${server.connections}", Sky, Modifier.weight(1f).height(80.dp))
            Metric(
                "Players", "${server.players} / ${server.targetCount}", Violet, Modifier.weight(1f).height(80.dp),
                progress = server.players.toFloat() / server.targetCount.coerceAtLeast(1)
            )
            Metric("Chunks", "${server.chunks}", Mid, Modifier.weight(1f).height(80.dp))
            Metric("TPS", fmtTps(server.tps), tpsColor(server.tps), Modifier.weight(1f).height(80.dp))
        }

        // Footer
        Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Knob("Count", server.targetCount.toString()) { v ->
                v.toIntOrNull()?.let { controller.setCount(server.id, it) }
            }
            Knob("Delay", server.delay.toString()) { v -> v.toIntOrNull()?.let { controller.setDelay(server.id, it) } }
        }
    }
}

@Composable
private fun HoverButton(label: String, accent: Color, textColor: Color, filled: Boolean = false, onClick: () -> Unit) {
    val hover = remember { MutableInteractionSource() }
    val isHovered by hover.collectIsHoveredAsState()

    val bg by animateColorAsState(
        when {
            filled && isHovered -> accent.copy(alpha = .85f)
            filled -> accent
            isHovered -> accent.copy(alpha = .15f)
            else -> accent.copy(alpha = .06f)
        }, tween(150)
    )
    val border by animateColorAsState(
        if (!filled && isHovered) accent.copy(alpha = .35f) else if (filled) Color.Transparent else accent.copy(alpha = .15f),
        tween(150)
    )

    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(6.dp))
            .hoverable(hover)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = hover, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = if (filled) textColor else accent, letterSpacing = .3.sp
        )
    }
}

@Composable
private fun Badge(text: String) {
    Text(
        text, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Lo,
        modifier = Modifier.background(Recessed, RoundedCornerShape(4.dp)).padding(horizontal = 7.dp, vertical = 3.dp)
    )
}

@Composable
private fun Metric(label: String, value: String, accent: Color, modifier: Modifier, progress: Float? = null) {
    val hover = remember { MutableInteractionSource() }
    val isHovered by hover.collectIsHoveredAsState()
    val borderColor by animateColorAsState(if (isHovered) Subtle else Color.Transparent, tween(150))

    Column(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Recessed)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .hoverable(hover)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Lo, letterSpacing = .5.sp)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
            fontFamily = FontFamily.Monospace,
            letterSpacing = (-.5).sp
        )
        if (progress != null) {
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(1.5.dp)).background(Divider)) {
                val animProg by animateFloatAsState(progress.coerceIn(0f, 1f), tween(500))
                Box(
                    Modifier.fillMaxHeight().fillMaxWidth(animProg).clip(RoundedCornerShape(1.5.dp)).background(Emerald)
                )
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier,
    placeholder: String = ""
) {
    val hover = remember { MutableInteractionSource() }
    val isHovered by hover.collectIsHoveredAsState()
    val borderColor by animateColorAsState(if (isHovered) Subtle else Divider, tween(150))

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Lo, letterSpacing = .5.sp)
        Box(
            Modifier.fillMaxWidth().height(34.dp)
                .background(Recessed, RoundedCornerShape(6.dp))
                .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                .hoverable(hover)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(placeholder, fontSize = 13.sp, color = Lo.copy(alpha = .35f), fontFamily = FontFamily.Monospace)
            }
            BasicTextField(
                value = value, onValueChange = onChange, singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = Hi, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(Emerald),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Knob(label: String, initial: String, onCommit: (String) -> Unit) {
    var v by remember(initial) { mutableStateOf(initial) }
    val hover = remember { MutableInteractionSource() }
    val isHovered by hover.collectIsHoveredAsState()
    val borderColor by animateColorAsState(if (isHovered) Subtle else Divider, tween(150))

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 11.sp, color = Lo, fontWeight = FontWeight.Medium)
        Box(
            Modifier.width(60.dp).height(28.dp)
                .background(Recessed, RoundedCornerShape(5.dp))
                .border(1.dp, borderColor, RoundedCornerShape(5.dp))
                .hoverable(hover)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = v, onValueChange = { v = it; onCommit(it) }, singleLine = true,
                textStyle = TextStyle(fontSize = 12.sp, color = Hi, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(Emerald),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun fmtTps(tps: Double?): String = if (tps == null) "—" else "%.1f".format(tps)
private fun tpsColor(tps: Double?): Color = when {
    tps == null -> Lo
    tps >= 18 -> Emerald
    tps >= 12 -> Amber
    else -> Rose
}
