package me.bmax.apatch.ui.screen.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import me.bmax.apatch.ui.component.ColorGenerationModeSelector
import me.bmax.apatch.ui.component.SliderStyleConfig
import me.bmax.apatch.ui.component.ColorStandardSelector
import me.bmax.apatch.ui.component.ColorStylePicker
import me.bmax.apatch.ui.theme.ColorGenerationMode
import me.bmax.apatch.ui.theme.ColorStandard
import me.bmax.apatch.ui.theme.ColorStyle
import me.bmax.apatch.util.ui.showToast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ViewQuilt
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ExpressiveCard
import me.bmax.apatch.ui.component.ExpressiveSwitch
import me.bmax.apatch.ui.component.SwitchIconState
import me.bmax.apatch.ui.component.FilePickerDialog

import me.bmax.apatch.ui.component.ThemeColorPicker
import me.bmax.apatch.ui.component.ThemeMode
import me.bmax.apatch.ui.component.ThemeModeSelector
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.component.LoadingDialogHandle
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.BackgroundManager
import me.bmax.apatch.ui.theme.FontConfig
import me.bmax.apatch.ui.theme.ThemeManager
import me.bmax.apatch.ui.theme.refreshTheme
import me.bmax.apatch.ui.screen.settings.appearance.AppearanceFontSection
import me.bmax.apatch.ui.screen.settings.appearance.AppearanceThemeSection
import me.bmax.apatch.ui.screen.settings.appearance.AppearanceBannerSection
import me.bmax.apatch.ui.screen.settings.appearance.AppearanceBackgroundSection
import me.bmax.apatch.ui.screen.settings.appearance.AppearanceFocusCardSection
import me.bmax.apatch.ui.screen.settings.appearance.AppearanceDashboardCardSection
import me.bmax.apatch.ui.screen.settings.appearance.HomeLayoutChooseDialog
import me.bmax.apatch.ui.screen.settings.appearance.NavModeChooseDialog
import me.bmax.apatch.ui.screen.settings.appearance.StatsTopLayoutChooseDialog
import me.bmax.apatch.ui.screen.settings.appearance.ThemeExportDialog
import me.bmax.apatch.ui.screen.settings.appearance.ThemeImportDialog
import me.bmax.apatch.ui.screen.settings.appearance.ThemeChooseDialog
import me.bmax.apatch.ui.screen.settings.appearance.colorNameToString
import me.bmax.apatch.ui.screen.settings.appearance.homeLayoutStyleToString
import me.bmax.apatch.util.PermissionUtils
import me.bmax.apatch.util.BottomBarIconConfig
import me.bmax.apatch.util.ui.FloatingBarConfig
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils
import me.bmax.apatch.util.ui.NavigationBarsSpacer
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import me.bmax.apatch.ui.component.folk.FolkSettingsSectionGroup
import me.bmax.apatch.ui.component.folk.FolkSliderPreference
import me.bmax.apatch.ui.component.folk.FolkSwitchPreference
import me.bmax.apatch.ui.component.folk.FolkValuePreference
import androidx.compose.material.icons.outlined.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsContent(
    snackBarHost: SnackbarHostState,
    kPatchReady: Boolean,
    onNavigateToThemeStore: () -> Unit,
    onNavigateToApiMarketplace: () -> Unit,
    flat: Boolean = false,
    highlightKey: String? = null,
    themeStoreMode: String? = null,
    onThemeStoreModeChanged: ((String) -> Unit)? = null,
) {
    val prefs = APApplication.sharedPreferences
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()


    val pickFontLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                loadingDialog.show()
                val success = FontConfig.saveFontFile(context, it)
                loadingDialog.hide()
                if (success) {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_custom_font_saved))
                    refreshTheme.value = true
                } else {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_custom_font_error))
                }
            }
        }
    }

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

    var pendingExportMetadata by remember { mutableStateOf<ThemeManager.ThemeMetadata?>(null) }
    val showExportDialog = remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var pendingImportMetadata by remember { mutableStateOf<ThemeManager.ThemeMetadata?>(null) }
    val showImportDialog = remember { mutableStateOf(false) }
    val showFilePicker = remember { mutableStateOf(false) }

    val importThemeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                loadingDialog.show()
                val metadata = ThemeManager.readThemeMetadata(context, uri)
                loadingDialog.hide()
                if (metadata != null) {
                    pendingImportUri = uri
                    pendingImportMetadata = metadata
                    showImportDialog.value = true
                } else {
                    loadingDialog.show()
                    val success = ThemeManager.importTheme(context, uri)
                    loadingDialog.hide()
                    snackBarHost.showSnackbar(
                        message = if (success) context.getString(R.string.settings_theme_imported) else context.getString(R.string.settings_theme_import_failed)
                    )
                }
            }
        }
    }

    val isNightModeSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    var nightModeFollowSys by remember { mutableStateOf(prefs.getBoolean("night_mode_follow_sys", true)) }
    var nightModeEnabled by remember { mutableStateOf(prefs.getBoolean("night_mode_enabled", true)) }
    val isDynamicColorSupport = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var useSystemDynamicColor by remember { mutableStateOf(prefs.getBoolean("use_system_color_theme", false)) }
    var customFontEnabled by remember { mutableStateOf(FontConfig.isCustomFontEnabled) }

    val refreshThemeObserver by refreshTheme.observeAsState(false)

    var customColorScheme by remember { mutableStateOf(prefs.getString("custom_color", "indigo")) }
    var amoledTheme by remember { mutableStateOf(prefs.getBoolean("amoled_theme", false)) }
    var colorGenerationMode by remember { mutableStateOf(ColorGenerationMode.fromKey(prefs.getString("color_generation_mode", "classic"))) }
    var colorStandard by remember { mutableStateOf(ColorStandard.fromName(prefs.getString("color_standard", "MD3_2021"))) }
    var colorStyle by remember { mutableStateOf(ColorStyle.fromName(prefs.getString("color_style", "TONAL_SPOT"))) }

    var currentStyle by remember { mutableStateOf(prefs.getString("home_layout_style", APApplication.HOME_LAYOUT_STYLE_DEFAULT)) }

    if (refreshThemeObserver) {
        nightModeFollowSys = prefs.getBoolean("night_mode_follow_sys", false)
        nightModeEnabled = prefs.getBoolean("night_mode_enabled", true)
        useSystemDynamicColor = prefs.getBoolean("use_system_color_theme", true)
        customFontEnabled = FontConfig.isCustomFontEnabled
        customColorScheme = prefs.getString("custom_color", "indigo")
        amoledTheme = prefs.getBoolean("amoled_theme", false)
        colorGenerationMode = ColorGenerationMode.fromKey(prefs.getString("color_generation_mode", "classic"))
        colorStandard = ColorStandard.fromName(prefs.getString("color_standard", "MD3_2021"))
        colorStyle = ColorStyle.fromName(prefs.getString("color_style", "TONAL_SPOT"))
        currentStyle = prefs.getString("home_layout_style", APApplication.HOME_LAYOUT_STYLE_DEFAULT)
    }

    val isDarkTheme = if (nightModeFollowSys) isSystemInDarkTheme() else nightModeEnabled
    val themeMode = if (nightModeFollowSys) ThemeMode.SYSTEM else if (nightModeEnabled) ThemeMode.DARK else ThemeMode.LIGHT
    val isStatsLayout = currentStyle == "stats"
    var statsTopLayout by remember { mutableStateOf(prefs.getString("stats_top_layout", "list") ?: "list") }
    val statsTopLayoutListLabel = stringResource(id = R.string.settings_stats_top_layout_list)
    val statsTopLayoutGridLabel = stringResource(id = R.string.settings_stats_top_layout_grid)
    val statsTopLayoutValue = if (statsTopLayout == "grid") statsTopLayoutGridLabel else statsTopLayoutListLabel
    var showStatsTopLayoutDialog by remember { mutableStateOf(false) }

    var showNavApm by remember { mutableStateOf(prefs.getBoolean("show_nav_apm", true)) }
    var showNavKpm by remember { mutableStateOf(prefs.getBoolean("show_nav_kpm", true)) }
    var showNavSuperUser by remember { mutableStateOf(prefs.getBoolean("show_nav_superuser", true)) }

    var currentNavMode by remember { mutableStateOf(prefs.getString("nav_mode", "floating") ?: "floating") }
    val navSchemeLabel = when (currentNavMode) {
        "rail" -> stringResource(R.string.settings_nav_mode_rail)
        "bottom" -> stringResource(R.string.settings_nav_mode_bottom)
        "floating" -> stringResource(R.string.settings_nav_mode_floating)
        else -> stringResource(R.string.settings_nav_mode_auto)
    }
    var showNavSchemeDialog by remember { mutableStateOf(false) }

    val isFloatingNav = currentNavMode == "floating"
    var floatingAutoHide by remember { mutableStateOf(prefs.getBoolean("floating_auto_hide", true)) }
    var floatingSwipeHide by remember { mutableStateOf(prefs.getBoolean("floating_swipe_hide", true)) }

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "night_mode_follow_sys" -> nightModeFollowSys = prefs.getBoolean(key, true)
                "night_mode_enabled" -> nightModeEnabled = prefs.getBoolean(key, true)
                "use_system_color_theme" -> useSystemDynamicColor = prefs.getBoolean(key, false)
                "custom_color" -> customColorScheme = prefs.getString(key, "indigo")
                "amoled_theme" -> amoledTheme = prefs.getBoolean(key, false)
                "color_generation_mode" -> colorGenerationMode = ColorGenerationMode.fromKey(prefs.getString(key, "classic"))
                "color_standard" -> colorStandard = ColorStandard.fromName(prefs.getString(key, "MD3_2021"))
                "color_style" -> colorStyle = ColorStyle.fromName(prefs.getString(key, "TONAL_SPOT"))
                "home_layout_style" -> currentStyle = prefs.getString(key, APApplication.HOME_LAYOUT_STYLE_DEFAULT)
                "stats_top_layout" -> statsTopLayout = prefs.getString(key, "list") ?: "list"
                "show_nav_apm" -> showNavApm = prefs.getBoolean(key, true)
                "show_nav_kpm" -> showNavKpm = prefs.getBoolean(key, true)
                "show_nav_superuser" -> showNavSuperUser = prefs.getBoolean(key, true)
                "nav_mode" -> currentNavMode = prefs.getString(key, "floating") ?: "floating"
                "floating_auto_hide" -> floatingAutoHide = prefs.getBoolean(key, true)
                "floating_swipe_hide" -> floatingSwipeHide = prefs.getBoolean(key, true)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val isKernelSuStyle = currentStyle == "kernelsu"
    val showGridCardSettings = isKernelSuStyle || (isStatsLayout && statsTopLayout == "grid")
    val isListStyle = currentStyle != "kernelsu" && currentStyle != "focus" && !(isStatsLayout && statsTopLayout == "grid")
    // Focus布局样式（FocusUI），该样式下4个卡片支持独立壁纸
    val isFocusStyle = currentStyle == "focus"
    val isDashboardStyle = currentStyle == "dashboard_ui"
    // 默认ListUI布局（对应HomeScreen的else分支）
    val isDefaultStyle = currentStyle !in listOf("kernelsu", "focus", "circle", "dashboard_ui", "stats")

    val badgeTextModes = listOf(
        stringResource(R.string.settings_custom_badge_text_full_half),
        stringResource(R.string.settings_custom_badge_text_lkm),
        stringResource(R.string.settings_custom_badge_text_gki),
        stringResource(R.string.settings_custom_badge_text_n_gki),
        stringResource(R.string.settings_custom_badge_text_oki),
        stringResource(R.string.settings_custom_badge_text_built_in)
    )
    val currentBadgeTextModeIndex = BackgroundConfig.customBadgeTextMode
    val currentBadgeTextMode = badgeTextModes.getOrElse(currentBadgeTextModeIndex) { badgeTextModes[0] }
    val showCustomBadgeTextDialog = remember { mutableStateOf(false) }

    val showHomeLayoutChooseDialog = remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {

        FolkSettingsSectionGroup(title = stringResource(R.string.settings_appearance_night_mode), flat = flat, highlightKey = highlightKey) {
            if (isNightModeSupported) {
                item(key = "appearance_theme_mode") {
                    ThemeModeSelector(
                        selectedMode = themeMode,
                        onModeSelected = { mode ->
                            when (mode) {
                                ThemeMode.LIGHT -> {
                                    nightModeFollowSys = false
                                    nightModeEnabled = false
                                    prefs.edit().putBoolean("night_mode_follow_sys", false).putBoolean("night_mode_enabled", false).apply()
                                }
                                ThemeMode.DARK -> {
                                    nightModeFollowSys = false
                                    nightModeEnabled = true
                                    prefs.edit().putBoolean("night_mode_follow_sys", false).putBoolean("night_mode_enabled", true).apply()
                                }
                                ThemeMode.SYSTEM -> {
                                    nightModeFollowSys = true
                                    prefs.edit().putBoolean("night_mode_follow_sys", true).apply()
                                }
                            }
                            refreshTheme.value = true
                        },
                        flat = flat,
                        bare = true,
                    )
                }
            }

            item(key = "appearance_theme_color") {
                ThemeColorPicker(
                    selectedColorKey = customColorScheme ?: "indigo",
                    onColorSelected = { key ->
                        prefs.edit().putString("custom_color", key).putBoolean("use_system_color_theme", false).apply()
                        customColorScheme = key
                        useSystemDynamicColor = false
                        refreshTheme.value = true
                    },
                    isDarkTheme = isDarkTheme,
                    flat = flat,
                    isDynamicColorSupported = isDynamicColorSupport,
                    isDynamicColorEnabled = useSystemDynamicColor,
                    onDynamicColorSelected = {
                        prefs.edit().putBoolean("use_system_color_theme", true).apply()
                        useSystemDynamicColor = true
                        refreshTheme.value = true
                    },
                    bare = true,
                )
            }

            // Color generation mode & style pickers
            item(key = "appearance_color_generation_mode") {
                ColorGenerationModeSelector(
                    selectedMode = colorGenerationMode,
                    onModeSelected = { mode ->
                        colorGenerationMode = mode
                        prefs.edit().putString("color_generation_mode", mode.key).apply()
                        refreshTheme.value = true
                    },
                    flat = flat,
                    bare = true,
                )
            }

            if (colorGenerationMode == ColorGenerationMode.CUSTOM) {
                item(key = "appearance_color_standard") {
                    ColorStandardSelector(
                        selectedStandard = colorStandard,
                        onStandardSelected = { standard ->
                            colorStandard = standard
                            prefs.edit().putString("color_standard", standard.name).apply()
                            refreshTheme.value = true
                        },
                        flat = flat,
                        bare = true,
                    )
                }

                item(key = "appearance_color_style") {
                    ColorStylePicker(
                        selectedStyle = colorStyle,
                        onStyleSelected = { style ->
                            colorStyle = style
                            prefs.edit().putString("color_style", style.name).apply()
                            refreshTheme.value = true
                        },
                        flat = flat,
                        bare = true,
                    )
                }
            }

            if (isDarkTheme) {
                item(key = "appearance_amoled_theme") {
                    val isWallpaperEnabled = BackgroundConfig.isCustomBackgroundEnabled
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = amoledTheme,
                                onValueChange = {
                                    if (!isWallpaperEnabled) {
                                        amoledTheme = it
                                        prefs.edit().putBoolean("amoled_theme", it).apply()
                                        refreshTheme.value = true
                                    }
                                },
                                role = Role.Switch,
                                enabled = !isWallpaperEnabled,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(),
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = Icons.Filled.DarkMode, contentDescription = null, tint = if (!isWallpaperEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(
                                text = stringResource(R.string.settings_amoled_theme),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (!isWallpaperEnabled) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.settings_amoled_theme_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (!isWallpaperEnabled) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                            )
                        }
                        ExpressiveSwitch(
                            checked = amoledTheme,
                            onCheckedChange = null,
                            enabled = !isWallpaperEnabled,
                        )
                    }
                }
            }

            item(key = "appearance_switch_icon") {
                var showSwitchIcon by remember { mutableStateOf(SwitchIconState.showIcon) }
                FolkSwitchPreference(
                    icon = Icons.Outlined.ToggleOn,
                    title = stringResource(R.string.settings_switch_icon),
                    summary = stringResource(R.string.settings_switch_icon_desc),
                    checked = showSwitchIcon,
                    onCheckedChange = {
                        showSwitchIcon = it
                        SwitchIconState.showIcon = it
                        prefs.edit().putBoolean("show_switch_icon", it).apply()
                    },
                )
            }

            item(key = "appearance_discrete_slider") {
                var isDiscreteSlider by remember { mutableStateOf(SliderStyleConfig.isDiscrete) }
                FolkSwitchPreference(
                    icon = Icons.Outlined.Segment,
                    title = stringResource(R.string.settings_discrete_slider),
                    summary = stringResource(R.string.settings_discrete_slider_desc),
                    checked = isDiscreteSlider,
                    onCheckedChange = {
                        isDiscreteSlider = it
                        SliderStyleConfig.isDiscrete = it
                        prefs.edit().putBoolean("discrete_slider", it).apply()
                    },
                )
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
                onClick = { showStatsTopLayoutDialog = true },
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
                                    showNavApm = it
                                    prefs.edit().putBoolean("show_nav_apm", it).apply()
                                },
                            )
                            me.bmax.apatch.ui.component.CheckboxItem(
                                icon = null,
                                title = stringResource(id = R.string.settings_show_kpm),
                                summary = null,
                                checked = showNavKpm,
                                onCheckedChange = {
                                    showNavKpm = it
                                    prefs.edit().putBoolean("show_nav_kpm", it).apply()
                                },
                            )
                            me.bmax.apatch.ui.component.CheckboxItem(
                                icon = null,
                                title = stringResource(id = R.string.settings_show_superuser),
                                summary = null,
                                checked = showNavSuperUser,
                                onCheckedChange = {
                                    showNavSuperUser = it
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
                onClick = { showNavSchemeDialog = true },
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
                            floatingAutoHide = it
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
                            floatingSwipeHide = it
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

        AppearanceBackgroundSection(
            flat = flat,
            highlightKey = highlightKey,
            snackBarHost = snackBarHost,
            loadingDialog = loadingDialog,
            showGridCardSettings = showGridCardSettings,
            currentBadgeTextMode = currentBadgeTextMode,
            showCustomBadgeTextDialog = showCustomBadgeTextDialog,
        )

        // FocusUI card wallpapers are separate from page-level and multi-background settings.
        if (isFocusStyle) {
            AppearanceFocusCardSection(
                flat = flat,
                highlightKey = highlightKey,
                snackBarHost = snackBarHost,
                loadingDialog = loadingDialog,
            )
        }

        if (isDashboardStyle) {
            AppearanceDashboardCardSection(
                flat = flat,
                highlightKey = highlightKey,
                snackBarHost = snackBarHost,
                loadingDialog = loadingDialog,
            )
        }

        if (showCustomBadgeTextDialog.value) {
            AlertDialog(
                onDismissRequest = { showCustomBadgeTextDialog.value = false },
                title = { Text(stringResource(id = R.string.settings_custom_badge_text)) },
                text = {
                    Column {
                        Text(
                            stringResource(id = R.string.settings_custom_badge_text_summary),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                        badgeTextModes.forEachIndexed { index, mode ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        BackgroundConfig.setCustomBadgeTextModeValue(index)
                                        BackgroundConfig.save(context)
                                        showCustomBadgeTextDialog.value = false
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                RadioButton(
                                    selected = index == currentBadgeTextModeIndex,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = mode)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCustomBadgeTextDialog.value = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            )
        }

        AppearanceBannerSection(
            flat = flat,
            highlightKey = highlightKey,
            onNavigateToApiMarketplace = onNavigateToApiMarketplace,
            loadingDialog = loadingDialog,
        )

        AppearanceFontSection(
            flat = flat,
            highlightKey = highlightKey,
            customFontEnabled = customFontEnabled,
            onCustomFontEnabledChange = { customFontEnabled = it },
            pickFontLauncher = pickFontLauncher,
            snackBarHost = snackBarHost,
        )

        AppearanceThemeSection(
            flat = flat,
            highlightKey = highlightKey,
            onNavigateToThemeStore = onNavigateToThemeStore,
            themeStoreMode = themeStoreMode,
            onThemeStoreModeChanged = onThemeStoreModeChanged,
            showExportDialog = showExportDialog,
            showFilePicker = showFilePicker,
            snackBarHost = snackBarHost,
            loadingDialog = loadingDialog,
        )
        }

    if (showHomeLayoutChooseDialog.value) {
        HomeLayoutChooseDialog(showHomeLayoutChooseDialog) { selectedLayout ->
            currentStyle = selectedLayout
            refreshTheme.value = true
        }
    }

    if (showNavSchemeDialog) {
        NavModeChooseDialog(
            showDialog = remember { mutableStateOf(true) }.apply { value = showNavSchemeDialog },
            currentMode = currentNavMode,
            onModeSelected = { mode ->
                currentNavMode = mode
                prefs.edit().putString("nav_mode", mode).apply()
                showNavSchemeDialog = false
            },
            onDismiss = { showNavSchemeDialog = false }
        )
    }

    if (showStatsTopLayoutDialog) {
        StatsTopLayoutChooseDialog(
            showDialog = remember { mutableStateOf(true) }.apply { value = showStatsTopLayoutDialog },
            currentMode = statsTopLayout,
            onModeSelected = { mode ->
                statsTopLayout = mode
                prefs.edit().putString("stats_top_layout", mode).apply()
                showStatsTopLayoutDialog = false
            },
            onDismiss = { showStatsTopLayoutDialog = false }
        )
    }

    if (showExportDialog.value) {
        ThemeExportDialog(
            showDialog = showExportDialog,
            onConfirm = { metadata ->
                pendingExportMetadata = metadata
                scope.launch {
                    loadingDialog.show()
                    try {
                        val exportDir = java.io.File("/storage/emulated/0/Download/FolkPatch/Themes/")
                        if (!exportDir.exists()) {
                            exportDir.mkdirs()
                        }
                        val safeName = metadata.name.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
                        val fileName = "$safeName.fpt"
                        val file = java.io.File(exportDir, fileName)
                        val uri = Uri.fromFile(file)
                        val success = ThemeManager.exportTheme(context, uri, metadata)
                        loadingDialog.hide()
                        snackBarHost.showSnackbar(
                            message = if (success) context.getString(R.string.settings_theme_saved) + ": ${file.absolutePath}" else context.getString(R.string.settings_theme_save_failed)
                        )
                    } catch (e: Exception) {
                        loadingDialog.hide()
                        snackBarHost.showSnackbar(message = context.getString(R.string.settings_theme_save_failed) + ": ${e.message}")
                    }
                    pendingExportMetadata = null
                }
            }
        )
    }

    if (showImportDialog.value && pendingImportMetadata != null) {
        ThemeImportDialog(
            showDialog = showImportDialog,
            metadata = pendingImportMetadata!!,
            onConfirm = {
                pendingImportUri?.let { uri ->
                    scope.launch {
                        loadingDialog.show()
                        val success = ThemeManager.importTheme(context, uri)
                        loadingDialog.hide()
                        snackBarHost.showSnackbar(
                            message = if (success) context.getString(R.string.settings_theme_imported) else context.getString(R.string.settings_theme_import_failed)
                        )
                        pendingImportUri = null
                        pendingImportMetadata = null
                    }
                }
            }
        )
    }

    if (showFilePicker.value) {
        FilePickerDialog(
            onDismissRequest = { showFilePicker.value = false },
            onFileSelected = { file ->
                showFilePicker.value = false
                val uri = Uri.fromFile(file)
                scope.launch {
                    loadingDialog.show()
                    val metadata = ThemeManager.readThemeMetadata(context, uri)
                    loadingDialog.hide()
                    if (metadata != null) {
                        pendingImportUri = uri
                        pendingImportMetadata = metadata
                        showImportDialog.value = true
                    } else {
                        loadingDialog.show()
                        val success = ThemeManager.importTheme(context, uri)
                        loadingDialog.hide()
                        snackBarHost.showSnackbar(
                            message = if (success) context.getString(R.string.settings_theme_imported) else context.getString(R.string.settings_theme_import_failed)
                        )
                    }
                }
            }
        )
    }
}
