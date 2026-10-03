package me.bmax.apatch.ui.component.folk

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import me.bmax.apatch.util.ui.NavigationBarsSpacer

/**
 * Colours for the settings bar.
 *
 * The bar fades from fully transparent to the elevated panel tone. Both ends
 * use the *same* RGB with only the alpha changing - using [Color.Transparent]
 * instead would make Material interpolate the colour from black, which showed
 * up as a grey scrim washing over the title and the content while scrolling.
 * The panel tone keeps its own alpha so the bar follows the custom background
 * opacity instead of turning opaque.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun folkTopAppBarColors(): TopAppBarColors {
    val elevated = folkGroupColor()
    return TopAppBarDefaults.largeTopAppBarColors(
        containerColor = elevated.copy(alpha = 0f),
        scrolledContainerColor = elevated,
    )
}

/**
 * Shared chrome for every settings sub-screen.
 *
 * Uses a compact [TopAppBar] with the title beside the back button. The bar
 * stays the same height while scrolling and gains the shared elevated tone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolkSettingsScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = folkTopAppBarColors(),
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    }
                },
                actions = actions,
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = Color.Transparent,
        snackbarHost = {
            if (snackbarHostState != null) {
                SnackbarHost(snackbarHostState)
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + FolkSettingsDimens.ScreenPadding,
            ),
        ) {
            content()
            item(key = "folk_bottom") {
                NavigationBarsSpacer()
            }
        }
    }
}
