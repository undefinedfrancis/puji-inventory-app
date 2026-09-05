package cn.puji.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Paper = Color(0xFFF7F1E5)
val PaperRaised = Color(0xFFFFFBF2)
val Ink = Color(0xFF17221C)
val InkMuted = Color(0xFF607067)
val TicketGreen = Color(0xFF176B4D)
val TicketGreenSoft = Color(0xFFDDEDE4)
val ScannerOrange = Color(0xFFE4572E)
val WarningSoft = Color(0xFFFFE1D6)
val Hairline = Color(0xFFD9D1C4)

private val LightColors = lightColorScheme(
    primary = TicketGreen,
    onPrimary = Color.White,
    secondary = ScannerOrange,
    onSecondary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = PaperRaised,
    onSurface = Ink,
    surfaceVariant = Color(0xFFECE5D9),
    onSurfaceVariant = InkMuted,
    outline = Hairline,
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7BD5AA),
    secondary = Color(0xFFFF9B7A),
    background = Color(0xFF101713),
    surface = Color(0xFF18211C),
    onBackground = Color(0xFFF4F0E7),
    onSurface = Color(0xFFF4F0E7),
)

@Composable
fun PujiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = MaterialTheme.typography.copy(
            displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 38.sp),
            headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 26.sp),
            titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 21.sp),
            bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
            labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 14.sp),
        ),
        content = content,
    )
}
