package me.bmax.apatch.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.utils.isRouteOnBackStackAsState
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator
import me.bmax.apatch.ui.theme.BackgroundConfig

/**
 * Softer rounded selected icons for the plain (always-bottom) navigation bar.
 * Kept local so the shared [BottomBarDestination] icon set used by the
 * floating bar and the side rail stays untouched.
 */
private fun BottomBarDestination.plainBarSelectedIcon(): ImageVector = when (this) {
    BottomBarDestination.Home -> Icons.Rounded.Home
    BottomBarDestination.KModule -> Icons.Rounded.Archive
    BottomBarDestination.SuperUser -> Icons.Rounded.AdminPanelSettings
    BottomBarDestination.AModule -> Icons.Rounded.Extension
    BottomBarDestination.Settings -> Icons.Rounded.Settings
}

/**
 * The plain docked bottom bar (nav_mode = "bottom"). It uses a Material3
 * [NavigationBar], but replaces the default pill indicator with a rounded-rect
 * block plus a small spring pop, and keeps every label visible.
 */
@Composable
internal fun PlainBottomNavigationBar(
    visibleDestinations: List<BottomBarDestination>,
    effectiveSelectedIndex: Int,
    containerColor: Color,
    superuserCount: Int,
    apmModuleCount: Int,
    kernelModuleCount: Int,
    enableSuperUserBadge: Boolean,
    enableApmBadge: Boolean,
    enableKernelBadge: Boolean,
    navController: NavHostController,
    onUserInteraction: (() -> Unit)?
) {
    val context = LocalContext.current
    val navigator = navController.rememberDestinationsNavigator()

    NavigationBar(
        tonalElevation = if (BackgroundConfig.isCustomBackgroundEnabled) 0.dp else 8.dp,
        containerColor = containerColor
    ) {
        visibleDestinations.forEachIndexed { index, destination ->
            key(destination) {
                val isCurrentDestOnBackStack by navController.isRouteOnBackStackAsState(destination.direction)
                val isSelected = index == effectiveSelectedIndex

                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        onUserInteraction?.invoke()
                        if (me.bmax.apatch.ui.theme.SoundEffectConfig.scope == me.bmax.apatch.ui.theme.SoundEffectConfig.SCOPE_BOTTOM_BAR) {
                            me.bmax.apatch.util.SoundEffectManager.play(context)
                        }
                        if (me.bmax.apatch.ui.theme.VibrationConfig.scope == me.bmax.apatch.ui.theme.VibrationConfig.SCOPE_BOTTOM_BAR) {
                            me.bmax.apatch.util.VibrationManager.vibrate(context)
                        }
                        if (isCurrentDestOnBackStack) {
                            navigator.popBackStack(destination.direction, false)
                        }
                        navigator.navigate(destination.direction) {
                            popUpTo(NavGraphs.root) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent
                    ),
                    icon = {
                        val badgeContent = when {
                            destination == BottomBarDestination.SuperUser && enableSuperUserBadge -> superuserCount
                            destination == BottomBarDestination.AModule && enableApmBadge -> apmModuleCount
                            destination == BottomBarDestination.KModule && enableKernelBadge -> kernelModuleCount
                            else -> 0
                        }

                        // Custom rounded-rect selection block. The M3 pill is made
                        // transparent above and we draw a softer "squircle" here with a
                        // small spring pop. Plain bar only, so floating/rail stay untouched.
                        val selectionProgress by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "plainBarSelection"
                        )
                        val indicatorColor = MaterialTheme.colorScheme.secondaryContainer

                        BadgedBox(
                            modifier = Modifier.drawBehind {
                                val alpha = selectionProgress.coerceIn(0f, 1f)
                                if (alpha > 0.001f) {
                                    val pop = selectionProgress.coerceIn(0f, 1.3f)
                                    val width = 56.dp.toPx() * (0.72f + 0.28f * pop)
                                    val height = 34.dp.toPx() * (0.72f + 0.28f * pop)
                                    val corner = 12.dp.toPx()
                                    drawRoundRect(
                                        color = indicatorColor.copy(alpha = alpha),
                                        topLeft = Offset(
                                            (size.width - width) / 2f,
                                            (size.height - height) / 2f
                                        ),
                                        size = Size(width, height),
                                        cornerRadius = CornerRadius(corner, corner)
                                    )
                                }
                            },
                            badge = {
                                if (badgeContent > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                                        Text(text = badgeContent.toString())
                                    }
                                }
                            }
                        ) {
                            val iconScale = 1f + 0.06f * selectionProgress.coerceIn(0f, 1.3f)
                            if (isSelected) {
                                NavBarIcon(
                                    destination,
                                    isSelected = true,
                                    selectedIcon = destination.plainBarSelectedIcon(),
                                    modifier = Modifier.scale(iconScale)
                                )
                            } else {
                                NavBarIcon(
                                    destination,
                                    isSelected = false,
                                    modifier = Modifier.scale(iconScale)
                                )
                            }
                        }
                    },
                    label = {
                        Text(
                            text = stringResource(destination.label),
                            overflow = TextOverflow.Visible,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    alwaysShowLabel = true
                )
            }
        }
    }
}
