package cn.puji.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Paper = Color(0xFFF6F2E9)
val PaperRaised = Color(0xFFFFFCF5)
val Ink = Color(0xFF292C2A)
val InkMuted = Color(0xFF6D6E69)
val ActionBlue = Color(0xFF365D6C)
val MutedGold = Color(0xFFD9BA7C)
val Hairline = Color(0xFFE3DED4)
// Kept for existing callers while all new UI consumes semantic theme colors.
val TicketGreen = ActionBlue
val TicketGreenSoft = Color(0xFFE4ECEE)
val ScannerOrange = ActionBlue
val WarningSoft = Color(0xFFF6E7E2)

private val LightColors = lightColorScheme(
    primary = ActionBlue, onPrimary = Color.White,
    primaryContainer = Color(0xFFE4ECEE), onPrimaryContainer = ActionBlue,
    secondary = Color(0xFF8A6736), onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0E5CF), onSecondaryContainer = Color(0xFF6A502B),
    background = Paper, onBackground = Ink, surface = PaperRaised, onSurface = Ink,
    surfaceContainerLowest = PaperRaised, surfaceContainerLow = PaperRaised,
    surfaceContainer = Paper, surfaceContainerHigh = PaperRaised, surfaceContainerHighest = Paper,
    surfaceBright = PaperRaised, surfaceDim = Color(0xFFE8E2D8), surfaceTint = ActionBlue,
    surfaceVariant = Color(0xFFEDE8DE), onSurfaceVariant = InkMuted,
    outline = Color(0xFF8B8C84), outlineVariant = Hairline,
    error = Color(0xFFAA3D36), errorContainer = WarningSoft, onErrorContainer = Color(0xFF8A2C27),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DC6D5), onPrimary = Color(0xFF183C49),
    primaryContainer = Color(0xFF294B58), onPrimaryContainer = Color(0xFFD2E8F0),
    secondary = Color(0xFFE2C48F), onSecondary = Color(0xFF3E2E15),
    secondaryContainer = Color(0xFF51432D), onSecondaryContainer = Color(0xFFF3DEB8),
    background = Color(0xFF1D1F1D), onBackground = Color(0xFFF2EDE3),
    surface = Color(0xFF272A27), onSurface = Color(0xFFF2EDE3),
    surfaceContainerLowest = Color(0xFF181B18), surfaceContainerLow = Color(0xFF222522),
    surfaceContainer = Color(0xFF272A27), surfaceContainerHigh = Color(0xFF30332F), surfaceContainerHighest = Color(0xFF383C36),
    surfaceBright = Color(0xFF383C36), surfaceDim = Color(0xFF181B18), surfaceTint = Color(0xFF9DC6D5),
    surfaceVariant = Color(0xFF333630), onSurfaceVariant = Color(0xFFBCBDB4),
    outline = Color(0xFF8D9086), outlineVariant = Color(0xFF42453E),
    error = Color(0xFFFFB4A9), errorContainer = Color(0xFF5E2D29), onErrorContainer = Color(0xFFFFDAD4),
)
private fun type(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.SansSerif, fontSize = size.sp, lineHeight = line.sp,
    fontWeight = weight, fontFeatureSettings = "tnum",
)
private val PujiTypography = Typography(
    displayLarge = type(26, 32, FontWeight.Medium), displayMedium = type(26, 32, FontWeight.Medium),
    displaySmall = type(26, 32, FontWeight.Medium),
    headlineLarge = type(22, 28, FontWeight.SemiBold), headlineMedium = type(22, 28, FontWeight.SemiBold),
    headlineSmall = type(22, 28, FontWeight.Medium),
    titleLarge = type(15, 22, FontWeight.Medium), titleMedium = type(15, 22, FontWeight.Medium),
    titleSmall = type(15, 22, FontWeight.Medium),
    bodyLarge = type(15, 22), bodyMedium = type(15, 22), bodySmall = type(13, 18),
    labelLarge = type(14, 20, FontWeight.Medium), labelMedium = type(12, 18),
    labelSmall = type(11, 16, FontWeight.Medium),
)
@Composable
fun PujiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = PujiTypography, shapes = Shapes(
            extraSmall = RoundedCornerShape(6.dp), small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(16.dp), extraLarge = RoundedCornerShape(20.dp)),
        content = content)
}
