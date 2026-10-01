package me.bmax.apatch.ui.component.folk

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Measurements that give the FolkPatch settings its own rhythm.
 *
 * These are deliberately not Material defaults: preferences grow with their
 * content, the trailing slot keeps a wider margin than the leading edge and the
 * section spacing is generous, so the page reads as "breathing" rather than as
 * a dense system settings list.
 */
object FolkSettingsDimens {
    /**
     * Horizontal inset of a settings group from the screen edge.
     * Measured on the reference: panel edge sits ~16.2dp from the screen edge.
     */
    val ScreenPadding = 16.dp

    /**
     * Vertical gap after a section (before the next section title).
     * Reference: panel bottom -> next panel top is ~51.5dp in total, which is
     * sectionSpacing + one title line box (~18dp) + SectionTitleSpacing.
     */
    val SectionSpacing = 22.dp

    /** Gap between a section title and its group surface. */
    val SectionTitleSpacing = 12.dp

    /** Extra indent of the section title relative to the screen edge. */
    val SectionTitleIndent = 8.dp

    /**
     * Outer corner radius of a group surface.
     *
     * Measured on the reference settings sub-page by fitting a circle to the
     * corner arc: 60-62px @ density 2.975 = 20.2dp. The same fit reproduces our
     * own known radii within 0.5dp, so this is trustworthy. (The reference home
     * page's media cards use a different, smaller radius - the settings
     * sub-pages are what we match here.)
     */
    val GroupCornerRadius = 20.dp

    /**
     * Vertical padding inside a group surface.
     * The reference panel height equals the sum of its row heights exactly, so
     * the group must not add any padding of its own.
     */
    val GroupVerticalPadding = 0.dp

    /** Leading inset of a preference row. */
    val ItemHorizontalPadding = 16.dp

    /**
     * Trailing inset - wider than the leading one.
     * Measured on the reference: the switch's right edge sits ~28dp from the
     * panel edge, noticeably further in than the leading icon.
     */
    val ItemEndPadding = 28.dp

    /**
     * Vertical padding of a row.
     *
     * Constant, not content-dependent: the reference's single-line rows are
     * ~55dp (2x17 + one 21dp line) and its two-line rows ~74dp (2x17 + 21 + 18),
     * i.e. the height difference comes purely from the extra text line. Our
     * previous 13/17 split made single-line rows too tight and two-line rows
     * too tall, which is what read as "uneven / oddly large".
     */
    val ItemVerticalPadding = 17.dp

    val ItemVerticalPaddingWithSummary = 17.dp

    /** Leading icon size. The reference uses a standard 24dp icon box. */
    val IconSize = 24.dp

    /**
     * Gap between the leading icon and the text column.
     * Measured: text starts ~56dp from the panel edge = 16 inset + 24 icon + 16.
     */
    val IconSpacing = 16.dp

    /** Tight gap between a title and its summary so they read as one unit. */
    val TitleSummarySpacing = 2.dp

    /** Gap between the text column and the trailing slot. */
    val TrailingSpacing = 14.dp

    /** Trailing chevron size. */
    val ChevronSize = 20.dp

    val GroupShape = RoundedCornerShape(GroupCornerRadius)
}

/**
 * Group surface colour.
 *
 * The reference never turns a setting group into a strong coloured card - the
 * panel is only a touch lighter than the page. We therefore blend the page
 * background towards a container role instead of using the container directly,
 * which keeps the surface subtle in every theme (including saturated ones).
 */
@Composable
fun folkGroupColor(): Color {
    val scheme = MaterialTheme.colorScheme
    val background = scheme.background

    // With a custom background image the page is transparent and container
    // roles already carry the user's opacity, so use them as-is.
    if (background.alpha < 0.99f) return scheme.surfaceContainer

    return if (background.luminance() < 0.5f) {
        lerp(background, scheme.surfaceContainerHigh, 0.45f)
    } else {
        lerp(background, scheme.surfaceContainer, 0.9f)
    }
}

@Composable
fun folkSectionTitleColor(): Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)

@Composable
fun folkIconColor(enabled: Boolean = true): Color =
    if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)

/** Small, letter-spaced section label - brand voice, not a Material label. */
@Composable
fun folkSectionTitleStyle(): TextStyle = MaterialTheme.typography.labelLarge.copy(
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 0.8.sp,
)

/**
 * Preference title: tighter line height and a slightly heavier weight than the
 * Material body style, which is what gives the row its "app" rather than
 * "system settings" feel. The family still comes from the active Typography so
 * user-selected custom fonts keep working.
 */
@Composable
fun folkPreferenceTitleStyle(): TextStyle = MaterialTheme.typography.bodyLarge.copy(
    fontSize = 16.sp,
    // Matches the reference's 21dp title line box, so a single-line row lands
    // on ~53dp and a two-line row on ~73dp.
    lineHeight = 21.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 0.2.sp,
)

@Composable
fun folkPreferenceSummaryStyle(): TextStyle = MaterialTheme.typography.bodySmall.copy(
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.3.sp,
)

@Composable
fun folkPreferenceValueStyle(): TextStyle = MaterialTheme.typography.bodyMedium.copy(
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = 0.2.sp,
)
