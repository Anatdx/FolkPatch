package me.bmax.apatch.ui.component.folk

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.bmax.apatch.ui.navigation.LocalBottomBarVisible
import me.bmax.apatch.ui.navigation.LocalIsFloatingNavMode

/**
 * How a [FolkScaffold] presents its title.
 *
 * [Inline] is the default on purpose: a collapsible [Large] title reads wrong on
 * a tab page (that combination caused a rework once already), so making the
 * large title an explicit opt-in keeps the mistake from happening by accident.
 */
enum class FolkTitleStyle { Large, Inline, None }

/**
 * The shared chrome for a screen: title, back button, actions, snackbar and the
 * insets that keep content clear of the status bar and the floating bottom bar.
 *
 * The content slot receives the padding to apply; it stays a plain lambda rather
 * than a `LazyListScope` because the screens are not all lists - a settings page
 * is a `LazyColumn`, the theme picker is a staggered grid and the audit log puts
 * a `TabRow` between the bar and the list. A shared "padding + chrome" contract
 * covers all of them without reshaping any of them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolkScaffold(
    title: String,
    titleStyle: FolkTitleStyle = FolkTitleStyle.Inline,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    titleContent: (@Composable () -> Unit)? = null,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        // Only the collapsible bar consumes scroll; the others need no connection.
        modifier = if (titleStyle == FolkTitleStyle.Large) {
            Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        } else {
            Modifier
        },
        topBar = {
            when (titleStyle) {
                FolkTitleStyle.Large -> LargeTopAppBar(
                    title = {
                        // A caller-supplied title (e.g. a search field) replaces
                        // the plain label entirely.
                        if (titleContent != null) {
                            titleContent()
                        } else {
                            Text(text = title, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = folkTopAppBarColors(),
                    navigationIcon = { FolkBackButton(onBack) },
                    actions = actions,
                    scrollBehavior = scrollBehavior,
                )

                FolkTitleStyle.Inline -> TopAppBar(
                    title = {
                        if (titleContent != null) {
                            titleContent()
                        } else {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    },
                    colors = folkTopAppBarColors(),
                    navigationIcon = { FolkBackButton(onBack) },
                    actions = actions,
                )

                FolkTitleStyle.None -> Unit
            }
        },
        containerColor = Color.Transparent,
        snackbarHost = {
            if (snackbarHostState != null) {
                SnackbarHost(snackbarHostState)
            }
        },
        floatingActionButton = floatingActionButton,
    ) { inner ->
        val layoutDirection = LocalLayoutDirection.current
        val clearance = folkBottomClearance()
        content(
            PaddingValues(
                start = inner.calculateStartPadding(layoutDirection),
                end = inner.calculateEndPadding(layoutDirection),
                top = inner.calculateTopPadding(),
                bottom = inner.calculateBottomPadding() + clearance,
            )
        )
    }
}

@Composable
private fun FolkBackButton(onBack: (() -> Unit)?) {
    if (onBack != null) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
        }
    }
}

/**
 * Clearance under the last item so it can be scrolled above the floating bottom
 * bar.
 *
 * The overlay only exists on main-tab routes in floating mode; on a detail page
 * the app already hides the bar, and in bottom/rail mode the shell pads the nav
 * host itself. In those cases a small gap under the content is all that is
 * needed, and adding the floating height there would just be dead scroll space.
 */
@Composable
private fun folkBottomClearance(): Dp {
    val floating = LocalIsFloatingNavMode.current
    val barVisible = LocalBottomBarVisible.current.value
    return animateDpAsState(
        targetValue = if (floating && barVisible) FloatingBarClearance else StaticBarClearance,
        label = "folkBottomClearance",
    ).value
}

private val FloatingBarClearance = 80.dp
private val StaticBarClearance = 16.dp
