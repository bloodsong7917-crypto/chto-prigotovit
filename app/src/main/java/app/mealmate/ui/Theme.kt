@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package app.mealmate.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.mealmate.R

/** Цвета КБЖУ и приёмов пищи — одинаковые в светлой и тёмной теме. */
object Accent {
    val Kcal = Color(0xFFF57C00)
    val Protein = Color(0xFFE5484D)
    val Fat = Color(0xFFE0A100)
    val Carbs = Color(0xFF2F9BD9)
    val Dinner = Color(0xFF6C5DD3)
    val Lime = Color(0xFF8BC34A)
    val Green = Color(0xFF2E9E3F)
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF14853B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC6F0B0),
    onPrimaryContainer = Color(0xFF0B3D18),
    secondary = Color(0xFF557F00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDF58A),
    onSecondaryContainer = Color(0xFF263500),
    tertiary = Color(0xFF8A6500),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE08A),
    onTertiaryContainer = Color(0xFF3F2E00),
    background = Color(0xFFF5FBEC),
    onBackground = Color(0xFF172015),
    surface = Color(0xFFF5FBEC),
    onSurface = Color(0xFF172015),
    surfaceVariant = Color(0xFFDDEBCF),
    onSurfaceVariant = Color(0xFF4A5743),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEAF5DD),
    surfaceContainerHigh = Color(0xFFE2F0D3),
    surfaceContainerHighest = Color(0xFFDAEAC9),
    outline = Color(0xFF758469),
    outlineVariant = Color(0xFFC4D5B5),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7EDC8B),
    onPrimary = Color(0xFF003913),
    primaryContainer = Color(0xFF1D5A2C),
    onPrimaryContainer = Color(0xFFC6F0B0),
    secondary = Color(0xFFC3E062),
    onSecondary = Color(0xFF263500),
    secondaryContainer = Color(0xFF3D5200),
    onSecondaryContainer = Color(0xFFDDF58A),
    tertiary = Color(0xFFFFD54A),
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF5B4300),
    onTertiaryContainer = Color(0xFFFFE08A),
    background = Color(0xFF0E170F),
    onBackground = Color(0xFFE0E8D8),
    surface = Color(0xFF0E170F),
    onSurface = Color(0xFFE0E8D8),
    surfaceVariant = Color(0xFF2B3A2A),
    onSurfaceVariant = Color(0xFFB9C8AD),
    surfaceContainerLowest = Color(0xFF0A110A),
    surfaceContainerLow = Color(0xFF172418),
    surfaceContainer = Color(0xFF1B2A1C),
    surfaceContainerHigh = Color(0xFF223423),
    surfaceContainerHighest = Color(0xFF2A3E2B),
    outline = Color(0xFF8A9A7E),
    outlineVariant = Color(0xFF3A4B38),
)

/** Градиент шапки и главных карточек. */
@Composable
fun headerBrush(): Brush =
    if (isSystemInDarkTheme()) Brush.linearGradient(listOf(Color(0xFF0B4D22), Color(0xFF1F6B2C), Color(0xFF3E7A1F)))
    else Brush.linearGradient(listOf(Color(0xFF0C7433), Color(0xFF2A9A3E), Color(0xFF4FA83A)))

private fun playfair(weight: FontWeight) = Font(
    R.font.playfair_display, weight,
    variationSettings = FontVariation.Settings(weight, FontStyle.Normal),
)

private fun manrope(weight: FontWeight) = Font(
    R.font.manrope, weight,
    variationSettings = FontVariation.Settings(weight, FontStyle.Normal),
)

val Playfair = FontFamily(playfair(FontWeight.SemiBold), playfair(FontWeight.Bold))
val Manrope = FontFamily(
    manrope(FontWeight.Normal), manrope(FontWeight.Medium),
    manrope(FontWeight.SemiBold), manrope(FontWeight.Bold), manrope(FontWeight.ExtraBold),
)

private fun TextStyle.heading() = copy(fontFamily = Playfair, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "lnum")
private fun TextStyle.body(weight: FontWeight = FontWeight.Medium) = copy(fontFamily = Manrope, fontWeight = weight)

private val AppTypography = Typography().run {
    copy(
        displayLarge = displayLarge.heading(),
        displayMedium = displayMedium.heading(),
        displaySmall = displaySmall.heading(),
        headlineLarge = headlineLarge.heading(),
        headlineMedium = headlineMedium.heading(),
        headlineSmall = headlineSmall.heading(),
        titleLarge = titleLarge.heading(),
        titleMedium = titleMedium.body(FontWeight.Bold),
        titleSmall = titleSmall.body(FontWeight.SemiBold),
        bodyLarge = bodyLarge.body(),
        bodyMedium = bodyMedium.body(),
        bodySmall = bodySmall.body(),
        labelLarge = labelLarge.body(FontWeight.Bold),
        labelMedium = labelMedium.body(FontWeight.SemiBold),
        labelSmall = labelSmall.body(FontWeight.SemiBold),
    )
}

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
