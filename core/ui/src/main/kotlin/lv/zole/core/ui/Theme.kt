package lv.zole.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

enum class ThemeMode { LIGHT, BLACK, SYSTEM }

/** Colours from the screen designs. Black (OLED) is the default. */
@Immutable
data class ZoleColors(
    val background: Color,
    val surface: Color,
    val surfaceBorder: Color,
    val divider: Color,
    val segmentBackground: Color,
    val segmentSelected: Color,
    val text: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentSoft: Color,
    val rowHighlight: Color,
    val cardFace: Color,
    val cardBorder: Color,
    val cardRed: Color,
    val cardBack: Color,
    val cardBackBorder: Color,
    val cardBackInner: Color,
    val avatar: Color,
    val outlineButton: Color,
    val primaryButton: Color,
    val onPrimaryButton: Color,
    val disabledButton: Color,
    val onDisabledButton: Color,
    val toggleOff: Color,
    val scrim: Color,
    val isDark: Boolean,
)

val BlackColors = ZoleColors(
    background = Color(0xFF000000),
    surface = Color(0xFF111111),
    surfaceBorder = Color(0xFF242424),
    divider = Color(0xFF1E1E1E),
    segmentBackground = Color(0xFF1C1C1C),
    segmentSelected = Color(0xFF333333),
    text = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF9C9C9C),
    textTertiary = Color(0xFFC8C8C8),
    accent = Color(0xFF7AA2FF),
    accentSoft = Color(0xFF12203F),
    rowHighlight = Color(0xFF0E1830),
    cardFace = Color(0xFF141414),
    cardBorder = Color(0xFF3A3A3A),
    cardRed = Color(0xFFFF7070),
    cardBack = Color(0xFF141C33),
    cardBackBorder = Color(0xFF26345C),
    cardBackInner = Color(0xFF34477A),
    avatar = Color(0xFF1C1C1C),
    outlineButton = Color(0xFF3A3A3A),
    primaryButton = Color(0xFFFFFFFF),
    onPrimaryButton = Color(0xFF000000),
    disabledButton = Color(0xFF1E1E1E),
    onDisabledButton = Color(0xFF6E6E6E),
    toggleOff = Color(0xFF3A3A3A),
    scrim = Color(0xB3000000),
    isDark = true,
)

val LightColors = ZoleColors(
    background = Color(0xFFF7F7F5),
    surface = Color(0xFFFFFFFF),
    surfaceBorder = Color(0xFFE3E3E0),
    divider = Color(0xFFECECEA),
    segmentBackground = Color(0xFFEBEBE8),
    segmentSelected = Color(0xFFFFFFFF),
    text = Color(0xFF111111),
    textSecondary = Color(0xFF6E6E6E),
    textTertiary = Color(0xFF444444),
    accent = Color(0xFF3E6BE0),
    accentSoft = Color(0xFFE5ECFF),
    rowHighlight = Color(0xFFEAF0FF),
    cardFace = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFD5D5D2),
    cardRed = Color(0xFFD83A3A),
    cardBack = Color(0xFF2C447F),
    cardBackBorder = Color(0xFF223566),
    cardBackInner = Color(0xFF4A62A0),
    avatar = Color(0xFFE6E6E3),
    outlineButton = Color(0xFFD0D0CD),
    primaryButton = Color(0xFF111111),
    onPrimaryButton = Color(0xFFFFFFFF),
    disabledButton = Color(0xFFE6E6E3),
    onDisabledButton = Color(0xFF9A9A9A),
    toggleOff = Color(0xFFD0D0CD),
    scrim = Color(0x66000000),
    isDark = false,
)

val LocalZoleColors = staticCompositionLocalOf { BlackColors }

/** Display settings the card drawing needs. */
@Immutable
data class CardStyle(val markTrumps: Boolean = true)

val LocalCardStyle = staticCompositionLocalOf { CardStyle() }

@OptIn(ExperimentalTextApi::class)
private fun inter(weight: Int) = Font(
    R.font.inter,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Inter = FontFamily(inter(400), inter(500), inter(600), inter(700))

object Zole {
    val colors: ZoleColors @Composable get() = LocalZoleColors.current
    val strings: Strings @Composable get() = LocalStrings.current
}

@Composable
fun ZoleTheme(
    mode: ThemeMode = ThemeMode.BLACK,
    cardStyle: CardStyle = CardStyle(),
    language: Language = Language.LV,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.BLACK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colors = if (dark) BlackColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.accent, background = colors.background, surface = colors.surface,
            onBackground = colors.text, onSurface = colors.text, surfaceContainer = colors.surface,
        )
    } else {
        lightColorScheme(
            primary = colors.accent, background = colors.background, surface = colors.surface,
            onBackground = colors.text, onSurface = colors.text, surfaceContainer = colors.surface,
        )
    }
    val base = TextStyle(fontFamily = Inter, color = colors.text)
    val typography = MaterialTheme.typography.let { t ->
        t.copy(
            bodyLarge = t.bodyLarge.merge(base), bodyMedium = t.bodyMedium.merge(base), bodySmall = t.bodySmall.merge(base),
            titleLarge = t.titleLarge.merge(base), titleMedium = t.titleMedium.merge(base), labelLarge = t.labelLarge.merge(base),
        )
    }
    CompositionLocalProvider(LocalZoleColors provides colors, LocalCardStyle provides cardStyle, LocalStrings provides stringsFor(language)) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
