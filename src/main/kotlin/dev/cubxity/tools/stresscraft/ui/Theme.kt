package dev.cubxity.tools.stresscraft.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val Canvas = Color(0xFF1B1B1F)
val Panel = Color(0xFF232328)
val Recessed = Color(0xFF19191D)
val Divider = Color(0xFF2E2E35)
val Subtle = Color(0xFF3A3A44)

val Hi = Color(0xFFECECF0)
val Mid = Color(0xFF9898A6)
val Lo = Color(0xFF5E5E6E)

val Emerald = Color(0xFF34D399)
val EmeraldDim = Color(0xFF1A6B4D)
val Rose = Color(0xFFF87171)
val RoseDim = Color(0xFF7F1D1D)
val Amber = Color(0xFFFBBF24)
val Sky = Color(0xFF60A5FA)
val Violet = Color(0xFFA78BFA)

val StressCraftColors = darkColorScheme(
    background = Canvas,
    surface = Panel,
    surfaceVariant = Recessed,
    primary = Emerald,
    secondary = Sky,
    tertiary = Violet,
    error = Rose,
    onBackground = Hi,
    onSurface = Hi,
    onPrimary = Color.Black,
    outline = Divider,
    outlineVariant = Subtle,
)
