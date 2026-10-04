package me.bmax.apatch.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import me.bmax.apatch.ui.component.themeColorOptions
import me.bmax.apatch.ui.theme.tokens.FolkThemeCatalog

// 壁纸有效亮度低于该阈值时，内容采用深色主题的中性色（浅色文字）
private const val WALLPAPER_DARK_THRESHOLD = 0.5f

/**
 * Builds the raw Material3 [ColorScheme] for the requested light/dark mode, following the
 * user's color-generation mode (custom MaterialKolor, system dynamic, or classic catalog).
 */
internal fun generateColorScheme(
    context: Context,
    darkTheme: Boolean,
    colorGenerationMode: String?,
    dynamicColor: Boolean,
    customColorScheme: String?,
    colorStandard: String?,
    colorStyle: String?,
    contrastLevel: Double,
): ColorScheme {
    return when {
        // Custom dynamic generation (MaterialKolor) with system wallpaper seed
        colorGenerationMode == "custom" && dynamicColor -> {
            val standard = ColorStandard.fromName(colorStandard)
            val style = ColorStyle.fromName(colorStyle)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ColorSchemeGenerator.generateFromContext(
                    context, darkTheme, style.paletteStyle, standard.specVersion, contrastLevel
                )
            } else {
                // Fallback: system dynamic color not available, use selected color as seed
                val seedOption = themeColorOptions.find { it.key == (customColorScheme ?: "indigo") }
                val seedColor = if (darkTheme) {
                    seedOption?.darkPrimary ?: Color(0xFFBAC3FF)
                } else {
                    seedOption?.lightPrimary ?: Color(0xFF4355B9)
                }
                ColorSchemeGenerator.generate(
                    seedColor, darkTheme, style.paletteStyle, standard.specVersion, contrastLevel
                )
            }
        }
        // Custom dynamic generation (MaterialKolor) with selected color seed
        colorGenerationMode == "custom" -> {
            val seedOption = themeColorOptions.find { it.key == (customColorScheme ?: "indigo") }
            val seedColor = if (darkTheme) {
                seedOption?.darkPrimary ?: Color(0xFFBAC3FF)
            } else {
                seedOption?.lightPrimary ?: Color(0xFF4355B9)
            }
            val standard = ColorStandard.fromName(colorStandard)
            val style = ColorStyle.fromName(colorStyle)
            ColorSchemeGenerator.generate(
                seedColor, darkTheme, style.paletteStyle, standard.specVersion, contrastLevel
            )
        }
        // System dynamic color (standard Material3 wallpaper extraction)
        dynamicColor -> {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                else -> FolkThemeCatalog.scheme("blue", darkTheme)
            }
        }
        // Classic themes, from the catalog.
        else -> FolkThemeCatalog.scheme(customColorScheme, darkTheme)
    }
}

/**
 * Applies the custom-background adaptation (transparent background + wallpaper-aware neutral
 * content colors) or the AMOLED override on top of [baseColorScheme].
 *
 * 在自定义壁纸模式下，中性色/容器色会跟随壁纸的有效明暗（亮底→深字，暗底→浅字），
 * 以保证半透明容器与其上文字的对比度；强调色仍沿用 [baseColorScheme]。
 */
internal fun adaptColorScheme(
    context: Context,
    baseColorScheme: ColorScheme,
    darkTheme: Boolean,
    amoledTheme: Boolean,
    useCustomBackground: Boolean,
    activeBackgroundUri: String?,
    colorGenerationMode: String?,
    dynamicColor: Boolean,
    customColorScheme: String?,
    colorStandard: String?,
    colorStyle: String?,
    contrastLevel: Double,
): ColorScheme {
    if (!useCustomBackground) {
        return if (darkTheme && amoledTheme) baseColorScheme.toAmoled() else baseColorScheme
    }

    val wallpaperDim = BackgroundConfig.getEffectiveBackgroundDim(darkTheme)
    val effectiveLuminance = BackgroundConfig.wallpaperLuminanceFor(activeBackgroundUri)
        ?.let { it * (1f - wallpaperDim) }
    val contentIsDark = effectiveLuminance?.let { it < WALLPAPER_DARK_THRESHOLD } ?: darkTheme

    val neutralScheme = if (contentIsDark == darkTheme) {
        baseColorScheme
    } else {
        generateColorScheme(
            context = context,
            darkTheme = contentIsDark,
            colorGenerationMode = colorGenerationMode,
            dynamicColor = dynamicColor,
            customColorScheme = customColorScheme,
            colorStandard = colorStandard,
            colorStyle = colorStyle,
            contrastLevel = contrastLevel,
        )
    }

    val opacity = BackgroundConfig.customBackgroundOpacity
    return baseColorScheme.copy(
        background = Color.Transparent,
        surface = neutralScheme.surface.copy(alpha = opacity),
        surfaceDim = neutralScheme.surfaceDim,
        surfaceBright = neutralScheme.surfaceBright,
        surfaceContainer = neutralScheme.surfaceContainer.copy(alpha = opacity),
        surfaceContainerLow = neutralScheme.surfaceContainerLow,
        surfaceContainerLowest = neutralScheme.surfaceContainerLowest,
        surfaceContainerHigh = neutralScheme.surfaceContainerHigh,
        surfaceContainerHighest = neutralScheme.surfaceContainerHighest,
        surfaceVariant = neutralScheme.surfaceVariant,
        onBackground = neutralScheme.onBackground,
        onSurface = neutralScheme.onSurface,
        onSurfaceVariant = neutralScheme.onSurfaceVariant,
        outline = neutralScheme.outline,
        outlineVariant = neutralScheme.outlineVariant,
        inverseSurface = neutralScheme.inverseSurface,
        inverseOnSurface = neutralScheme.inverseOnSurface,
        secondaryContainer = baseColorScheme.secondaryContainer.copy(alpha = opacity),
    )
}
