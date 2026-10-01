package me.bmax.apatch.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp

import me.bmax.apatch.ui.theme.tokens.wrapAware

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Material's sizes with our font and our line breaking. The sizes stay Material's: a screen that
 * wants one of our three levels asks [me.bmax.apatch.ui.theme.tokens.FolkType] for it.
 */
fun getTypography(fontFamily: FontFamily): Typography {
    fun heading(style: TextStyle) =
        style.copy(fontFamily = fontFamily).wrapAware(LineBreak.Heading)

    fun paragraph(style: TextStyle) =
        style.copy(fontFamily = fontFamily).wrapAware()

    return Typography(
        displayLarge = heading(Typography.displayLarge),
        displayMedium = heading(Typography.displayMedium),
        displaySmall = heading(Typography.displaySmall),
        headlineLarge = heading(Typography.headlineLarge),
        headlineMedium = heading(Typography.headlineMedium),
        headlineSmall = heading(Typography.headlineSmall),
        titleLarge = heading(Typography.titleLarge),
        titleMedium = heading(Typography.titleMedium),
        titleSmall = paragraph(Typography.titleSmall),
        bodyLarge = paragraph(Typography.bodyLarge),
        bodyMedium = paragraph(Typography.bodyMedium),
        bodySmall = paragraph(Typography.bodySmall),
        labelLarge = paragraph(Typography.labelLarge),
        labelMedium = paragraph(Typography.labelMedium),
        labelSmall = paragraph(Typography.labelSmall)
    )
}
