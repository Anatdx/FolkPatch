package me.bmax.apatch.ui.screen.settings.appearance

import android.content.ActivityNotFoundException
import android.net.Uri
import me.bmax.apatch.util.ui.showToast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ExpressiveCard
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.LoadingDialogHandle
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.BackgroundManager
import me.bmax.apatch.ui.theme.refreshTheme
import me.bmax.apatch.ui.screen.settings.appearance.homeLayoutStyleToString
import me.bmax.apatch.util.PermissionUtils
import me.bmax.apatch.util.BottomBarIconConfig
import me.bmax.apatch.util.ui.FloatingBarConfig
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import me.bmax.apatch.ui.component.folk.FolkSettingsSectionGroup
import me.bmax.apatch.ui.component.folk.FolkSliderPreference
import me.bmax.apatch.ui.component.folk.FolkSwitchPreference
import me.bmax.apatch.ui.component.folk.FolkValuePreference
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceLayoutSection(
    flat: Boolean,
    highlightKey: String?,
    kPatchReady: Boolean,
    snackBarHost: SnackbarHostState,
    loadingDialog: LoadingDialogHandle,
    currentStyle: String?,
    isStatsLayout: Boolean,
    statsTopLayoutValue: String,
    onShowStatsTopLayoutDialog: () -> Unit,
    navSchemeLabel: String,
    onShowNavSchemeDialog: () -> Unit,
    isFloatingNav: Boolean,
    floatingAutoHide: Boolean,
    onFloatingAutoHideChange: (Boolean) -> Unit,
    floatingSwipeHide: Boolean,
    onFloatingSwipeHideChange: (Boolean) -> Unit,
    showNavApm: Boolean,
    onShowNavApmChange: (Boolean) -> Unit,
    showNavKpm: Boolean,
    onShowNavKpmChange: (Boolean) -> Unit,
    showNavSuperUser: Boolean,
    onShowNavSuperUserChange: (Boolean) -> Unit,
    isListStyle: Boolean,
    isDefaultStyle: Boolean,
    currentBadgeTextMode: String,
    showCustomBadgeTextDialog: MutableState<Boolean>,
    showHomeLayoutChooseDialog: MutableState<Boolean>,
) {
    val prefs = APApplication.sharedPreferences
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

        val pickTitleImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                loadingDialog.show()
                val success = BackgroundManager.saveAndApplyTitleImage(context, it)
                loadingDialog.hide()
                if (success) {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_title_image_saved))
                    refreshTheme.value = true
                } else {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_title_image_error))
                }
            }
        }
    }

    FolkSettingsSectionGroup(title = stringResource(R.string.settings_appearance_layout), flat = flat, highlightKey = highlightKey) {
        item(key = "appearance_home_layout") {
            FolkValuePreference(
            icon = Icons.Outlined.Dashboard,
            title = stringResource(id = R.string.settings_home_layout_style),
            summary = stringResource(homeLayoutStyleToString(currentStyle.toString())),
            onClick = { showHomeLayoutChooseDialog.value = true },
        )
        }

        item(key = "appearance_stats_top_layout", visible = isStatsLayout) {
            FolkValuePreference(
            icon = Icons.Outlined.GridView,
            title = stringResource(id = R.string.settings_stats_top_layout),
            summary = statsTopLayoutValue,
            onClick = { onShowStatsTopLayoutDialog() },
        )
        }

        if (kPatchReady) {
            item(key = "appearance_nav_layout") {
                var expanded by remember { mutableStateOf(false) }
                val rotationState by animateFloatAsState(
                    targetValue = if (expanded) 180f else 0f,
                    label = "ArrowRotation",
                )
                ExpressiveCard(flat = flat, onClick = { expanded = !expanded }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = Icons.Filled.Navigation, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.settings_nav_layout_title),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(id = R.string.settings_nav_layout_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.rotate(rotationState),
                        )
                    }
                }
                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp)) {
                        me.bmax.apatch.ui.component.CheckboxItem(
                            icon = null,
                            title = stringResource(id = R.string.settings_show_apm),
                            summary = null,
                            checked = showNavApm,
                            onCheckedChange = {
                                onShowNavApmChange(it)
                                prefs.edit().putBoolean("show_nav_apm", it).apply()
                            },
                        )
                        me.bmax.apatch.ui.component.CheckboxItem(
                            icon = null,
                            title = stringResource(id = R.string.settings_show_kpm),
                            summary = null,
                            checked = showNavKpm,
                            onCheckedChange = {
                                onShowNavKpmChange(it)
                                prefs.edit().putBoolean("show_nav_kpm", it).apply()
                            },
                        )
                        me.bmax.apatch.ui.component.CheckboxItem(
                            icon = null,
                            title = stringResource(id = R.string.settings_show_superuser),
                            summary = null,
                            checked = showNavSuperUser,
                            onCheckedChange = {
                                onShowNavSuperUserChange(it)
                                prefs.edit().putBoolean("show_nav_superuser", it).apply()
                            },
                        )
                    }
                }
            }
        }

        item(key = "appearance_nav_scheme") {
            FolkValuePreference(
            icon = Icons.Outlined.Menu,
            title = stringResource(id = R.string.settings_nav_scheme),
            summary = navSchemeLabel,
            onClick = { onShowNavSchemeDialog() },
        )
        }

        if (isFloatingNav) {
            item(key = "appearance_navbar_glass") {
                FolkSwitchPreference(
                    icon = Icons.Outlined.AutoAwesome,
                    title = stringResource(id = R.string.settings_navbar_glass_effect),
                    summary = stringResource(id = R.string.settings_navbar_glass_effect_summary),
                    checked = BackgroundConfig.isNavBarGlassEnabled,
                    onCheckedChange = {
                        BackgroundConfig.setNavBarGlassEnabledState(it)
                        BackgroundConfig.save(context)
                    },
                )
            }

            if (BackgroundConfig.isNavBarGlassEnabled) {
                item(key = "appearance_navbar_glass_blur") {
                    FolkSliderPreference(
                        title = stringResource(id = R.string.settings_navbar_glass_blur_strength),
                        value = BackgroundConfig.navBarGlassBlurStrength,
                        onValueChange = { BackgroundConfig.setNavBarGlassBlurStrengthValue(it) },
                        onValueChangeFinished = { BackgroundConfig.save(context) },
                    )
                }

                item(key = "appearance_navbar_glass_transparency") {
                    FolkSliderPreference(
                        title = stringResource(id = R.string.settings_navbar_glass_transparency),
                        value = BackgroundConfig.navBarGlassTransparency,
                        onValueChange = { BackgroundConfig.setNavBarGlassTransparencyValue(it) },
                        onValueChangeFinished = { BackgroundConfig.save(context) },
                    )
                }

                item(key = "appearance_navbar_glass_highlight") {
                    FolkSliderPreference(
                        title = stringResource(id = R.string.settings_navbar_glass_highlight_strength),
                        value = BackgroundConfig.navBarGlassHighlightStrength,
                        onValueChange = { BackgroundConfig.setNavBarGlassHighlightStrengthValue(it) },
                        onValueChangeFinished = { BackgroundConfig.save(context) },
                    )
                }

                item(key = "appearance_navbar_glass_specular") {
                    FolkSwitchPreference(
                        icon = Icons.Outlined.LensBlur,
                        title = stringResource(id = R.string.settings_navbar_glass_specular),
                        summary = stringResource(id = R.string.settings_navbar_glass_specular_summary),
                        checked = BackgroundConfig.isNavBarGlassSpecularEnabled,
                        onCheckedChange = {
                            BackgroundConfig.setNavBarGlassSpecularEnabledState(it)
                            BackgroundConfig.save(context)
                        },
                    )
                }

                item(key = "appearance_navbar_glass_glow") {
                    FolkSwitchPreference(
                        icon = Icons.Outlined.Grain,
                        title = stringResource(id = R.string.settings_navbar_glass_inner_glow),
                        summary = stringResource(id = R.string.settings_navbar_glass_inner_glow_summary),
                        checked = BackgroundConfig.isNavBarGlassInnerGlowEnabled,
                        onCheckedChange = {
                            BackgroundConfig.setNavBarGlassInnerGlowEnabledState(it)
                            BackgroundConfig.save(context)
                        },
                    )
                }

                item(key = "appearance_navbar_glass_border") {
                    FolkSwitchPreference(
                        icon = Icons.Outlined.BorderStyle,
                        title = stringResource(id = R.string.settings_navbar_glass_border),
                        summary = stringResource(id = R.string.settings_navbar_glass_border_summary),
                        checked = BackgroundConfig.isNavBarGlassBorderEnabled,
                        onCheckedChange = {
                            BackgroundConfig.setNavBarGlassBorderEnabledState(it)
                            BackgroundConfig.save(context)
                        },
                    )
                }
            }

            // ---- 紧凑圆角风格 ----
            // 仅非毛玻璃模式显示（毛玻璃已有独立的外观控制）
            if (!BackgroundConfig.isNavBarGlassEnabled) {
                item(key = "appearance_compact_rounded_bar") {
                    FolkSwitchPreference(
                        icon = Icons.Outlined.RoundedCorner,
                        title = stringResource(id = R.string.settings_compact_rounded_bar),
                        summary = stringResource(id = R.string.settings_compact_rounded_bar_summary),
                        checked = FloatingBarConfig.isCompactRoundedStyle,
                        onCheckedChange = { enabled ->
                            FloatingBarConfig.isCompactRoundedStyle = enabled
                            FloatingBarConfig.save(context)
                        },
                    )
                }
            }

            item(key = "appearance_floating_auto_hide") {
                FolkSwitchPreference(
                    icon = Icons.Outlined.VisibilityOff,
                    title = stringResource(id = R.string.settings_floating_auto_hide),
                    summary = stringResource(id = R.string.settings_floating_auto_hide_summary),
                    checked = floatingAutoHide,
                    onCheckedChange = {
                        onFloatingAutoHideChange(it)
                        prefs.edit().putBoolean("floating_auto_hide", it).apply()
                    },
                )
            }

            item(key = "appearance_floating_swipe_hide") {
                FolkSwitchPreference(
                    icon = Icons.Outlined.Swipe,
                    title = stringResource(id = R.string.settings_floating_swipe_hide),
                    summary = stringResource(id = R.string.settings_floating_swipe_hide_summary),
                    checked = floatingSwipeHide,
                    onCheckedChange = {
                        onFloatingSwipeHideChange(it)
                        prefs.edit().putBoolean("floating_swipe_hide", it).apply()
                    },
                )
            }
        }

        item(key = "appearance_nav_custom_icons") {
            val customNavIconsEnabled = remember { mutableStateOf(prefs.getBoolean("nav_icon_custom_enabled", false)) }
            var editingDestName by remember { mutableStateOf<String?>(null) }
            // Observe config revision so the previews below refresh immediately after pick/clear.
            val iconRevision by BottomBarIconConfig.revision.collectAsStateWithLifecycle()
            val iconPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                val dest = editingDestName ?: return@rememberLauncherForActivityResult
                if (uri != null) {
                    // Copy the picked image into internal storage so it survives app
                    // restarts (content:// read grants are only temporary).
                    scope.launch {
                        val saved = withContext(Dispatchers.IO) {
                            BottomBarIconConfig.saveCustomIcon(context, dest, uri)
                        }
                        snackBarHost.showSnackbar(
                            context.getString(
                                if (saved) R.string.nav_icon_set
                                else R.string.nav_icon_set_failed
                            )
                        )
                    }
                }
                editingDestName = null
            }

            FolkSwitchPreference(
                icon = Icons.Outlined.Image,
                title = stringResource(R.string.settings_nav_custom_icons),
                summary = stringResource(R.string.settings_nav_custom_icons_summary),
                checked = customNavIconsEnabled.value,
                onCheckedChange = {
                    customNavIconsEnabled.value = it
                    BottomBarIconConfig.isEnabled = it
                }
            )

            if (customNavIconsEnabled.value) {
                Spacer(Modifier.height(8.dp))
                val navDestinations = listOf(
                    Triple("Home", R.string.nav_icon_home, Icons.Filled.Home),
                    Triple("KModule", R.string.nav_icon_kpm, Icons.Filled.Archive),
                    Triple("SuperUser", R.string.nav_icon_superuser, Icons.Filled.AdminPanelSettings),
                    Triple("AModule", R.string.nav_icon_apm, Icons.Filled.Extension),
                    Triple("Settings", R.string.nav_icon_settings, Icons.Filled.Settings),
                )

                Column {
                    navDestinations.forEach { (destName, labelRes, defaultIcon) ->
                        val customUri = remember(iconRevision, destName) { prefs.getString("nav_icon_$destName", null) }
                        ExpressiveCard(
                            flat = flat,
                            onClick = {
                                editingDestName = destName
                                try { iconPickerLauncher.launch("image/*") } catch (_: Throwable) {}
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (customUri != null) {
                                    AsyncImage(
                                        model = customUri,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = defaultIcon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        stringResource(labelRes),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        if (customUri != null) stringResource(R.string.nav_icon_custom_selected)
                                        else stringResource(R.string.nav_icon_default),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (customUri != null) {
                                    IconButton(
                                        onClick = {
                                            BottomBarIconConfig.clearCustomIcon(context, destName)
                                            scope.launch {
                                                snackBarHost.showSnackbar(context.getString(R.string.nav_icon_cleared))
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.Close, stringResource(R.string.nav_icon_clear), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }

        item(key = "appearance_list_card_badge", visible = isListStyle) {
            FolkSwitchPreference(
                icon = Icons.Outlined.LabelOff,
                title = stringResource(id = R.string.settings_list_card_hide_status_badge),
                summary = stringResource(id = R.string.settings_list_card_hide_status_badge_summary),
                checked = BackgroundConfig.isListWorkingCardModeHidden,
                onCheckedChange = {
                    BackgroundConfig.setListWorkingCardModeHiddenState(it)
                    BackgroundConfig.save(context)
                },
            )
        }

        item(key = "appearance_custom_badge_text_list", visible = isListStyle && !BackgroundConfig.isListWorkingCardModeHidden) {
            FolkValuePreference(
            icon = Icons.Outlined.Badge,
            title = stringResource(id = R.string.settings_custom_badge_text),
            summary = currentBadgeTextMode,
            onClick = { showCustomBadgeTextDialog.value = true },
        )
        }

        item(key = "appearance_list_info_icons", visible = isDefaultStyle) {
            var showListInfoIcons by remember { mutableStateOf(prefs.getBoolean("list_info_show_icons", false)) }
            FolkSwitchPreference(
                icon = Icons.Outlined.ViewList,
                title = stringResource(id = R.string.settings_list_info_show_icons),
                summary = stringResource(id = R.string.settings_list_info_show_icons_summary),
                checked = showListInfoIcons,
                onCheckedChange = {
                    showListInfoIcons = it
                    prefs.edit().putBoolean("list_info_show_icons", it).apply()
                    refreshTheme.value = true
                },
            )
        }

        item(key = "appearance_advanced_title") {
            FolkSwitchPreference(
                icon = Icons.Outlined.Title,
                title = stringResource(id = R.string.settings_advanced_title_style),
                summary = if (BackgroundConfig.isAdvancedTitleStyleEnabled) stringResource(id = R.string.settings_advanced_title_style_enabled) else stringResource(id = R.string.settings_advanced_title_style_summary),
                checked = BackgroundConfig.isAdvancedTitleStyleEnabled,
                onCheckedChange = {
                    BackgroundConfig.setAdvancedTitleStyleEnabledState(it)
                    BackgroundConfig.save(context)
                    refreshTheme.value = true
                },
            )
        }

        if (BackgroundConfig.isAdvancedTitleStyleEnabled) {
            item(key = "appearance_title_day_opacity") {
                FolkSliderPreference(
                    title = stringResource(id = R.string.settings_title_image_day_opacity),
                    value = BackgroundConfig.titleImageDayOpacity,
                    onValueChange = { BackgroundConfig.setTitleImageDayOpacityValue(it) },
                    onValueChangeFinished = { BackgroundConfig.save(context) },
                )
            }

            item(key = "appearance_title_night_opacity") {
                FolkSliderPreference(
                    title = stringResource(id = R.string.settings_title_image_night_opacity),
                    value = BackgroundConfig.titleImageNightOpacity,
                    onValueChange = { BackgroundConfig.setTitleImageNightOpacityValue(it) },
                    onValueChangeFinished = { BackgroundConfig.save(context) },
                )
            }

            item(key = "appearance_title_image_dim") {
                FolkSliderPreference(
                    title = stringResource(id = R.string.settings_title_image_dim),
                    value = BackgroundConfig.titleImageDim,
                    onValueChange = { BackgroundConfig.setTitleImageDimValue(it) },
                    onValueChangeFinished = { BackgroundConfig.save(context) },
                )
            }

            item(key = "appearance_title_image_offset_x") {
                FolkSliderPreference(
                    title = stringResource(id = R.string.settings_title_image_offset_x),
                    value = BackgroundConfig.titleImageOffsetX,
                    valueRange = -1f..1f,
                    onValueChange = { BackgroundConfig.setTitleImageOffsetXValue(it) },
                    onValueChangeFinished = { BackgroundConfig.save(context) },
                )
            }

            item(key = "appearance_select_title_image") {
                FolkValuePreference(
            icon = Icons.Outlined.Image,
            title = stringResource(id = R.string.settings_select_title_image),
            summary = if (!BackgroundConfig.titleImageUri.isNullOrEmpty()) stringResource(id = R.string.settings_title_image_selected) else null,
            onClick = {                            if (PermissionUtils.hasExternalStoragePermission(context)) {
                            try {
                                pickTitleImageLauncher.launch("image/*")
                            } catch (e: ActivityNotFoundException) {
                                showToast(context, e.message ?: "")
                            }
                        } else {
                            showToast(context, context.getString(R.string.settings_title_image_permission_required))
                        }
                    },
        )
            }

            if (!BackgroundConfig.titleImageUri.isNullOrEmpty()) {
                item(key = "appearance_clear_title_image") {
                    val clearTitleImageDialog = rememberConfirmDialog(
                        onConfirm = {
                            scope.launch {
                                loadingDialog.show()
                                BackgroundManager.clearTitleImage(context)
                                loadingDialog.hide()
                                snackBarHost.showSnackbar(message = context.getString(R.string.settings_title_image_cleared))
                                refreshTheme.value = true
                            }
                        }
                    )
                    FolkValuePreference(
            icon = Icons.Outlined.Delete,
            title = stringResource(id = R.string.settings_clear_title_image),
            onClick = {                                clearTitleImageDialog.showConfirm(
                                title = context.getString(R.string.settings_clear_title_image),
                                content = context.getString(R.string.settings_clear_title_image_confirm),
                                markdown = false,
                            )
                        },
        )
                }
            }
        }
    }
}
