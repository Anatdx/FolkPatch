package me.bmax.apatch.ui.screen.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import me.bmax.apatch.util.ui.showToast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.ramcosta.composedestinations.generated.destinations.LanguagePickerScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.APApplication
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.UpdateDialog
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.component.folk.FolkNavigationPreference
import me.bmax.apatch.ui.component.folk.FolkSettingsGroup
import me.bmax.apatch.ui.component.folk.FolkSettingsSection
import me.bmax.apatch.ui.component.folk.FolkSwitchPreference
import me.bmax.apatch.ui.component.folk.FolkValuePreference
import me.bmax.apatch.util.*
import me.bmax.apatch.ui.screen.settings.general.*
import androidx.compose.material.icons.outlined.*

@Composable
fun GeneralSettingsContent(
    kPatchReady: Boolean,
    aPatchReady: Boolean,
    currentSELinuxMode: String,
    onSELinuxModeChange: (String) -> Unit,
    isGlobalNamespaceEnabled: Boolean,
    namespaceLoaded: Boolean,
    onGlobalNamespaceChange: (Boolean) -> Unit,
    isMagicMountEnabled: Boolean,
    onMagicMountChange: (Boolean) -> Unit,
    snackBarHost: SnackbarHostState,
    flat: Boolean = false,
    highlightKey: String? = null,
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val prefs = APApplication.sharedPreferences
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()

    val languageTitle = stringResource(id = R.string.settings_app_language)
    val languageValue = remember {
        val locale = AppCompatDelegate.getApplicationLocales()[0]
        if (locale == null) {
            context.getString(R.string.system_default)
        } else {
            val languageTag = locale.toLanguageTag()
            val languages = context.resources.getStringArray(R.array.languages)
            val languagesValues = context.resources.getStringArray(R.array.languages_values)

            // Prefer an exact match, then a bare language-code match
            // (e.g. "id" for "id-ID"), otherwise fall back to the raw tag.
            var index = languagesValues.indexOf(languageTag)
            if (index < 0) {
                index = languagesValues.indexOf(languageTag.substringBefore('-'))
            }
            if (index >= 0) languages[index] else languageTag
        }
    }

    val updateTitle = stringResource(id = R.string.settings_check_update)

    val autoUpdateTitle = stringResource(id = R.string.settings_auto_update_check)
    val autoUpdateSummary = stringResource(id = R.string.settings_auto_update_check_summary)

    val globalNamespaceTitle = stringResource(id = R.string.settings_global_namespace_mode)
    val globalNamespaceSummary = stringResource(id = R.string.settings_global_namespace_mode_summary)

    val magicMountTitle = stringResource(id = R.string.settings_magic_mount)
    val magicMountSummary = stringResource(id = R.string.settings_magic_mount_summary)

    val selinuxModeTitle = stringResource(id = R.string.settings_selinux_mode)
    val selinuxModeSummary = stringResource(id = R.string.settings_selinux_mode_summary)
    val selinuxModeValue = when (currentSELinuxMode) {
        "Enforcing" -> stringResource(R.string.settings_selinux_mode_enforcing)
        "Permissive" -> stringResource(R.string.settings_selinux_mode_permissive)
        else -> stringResource(R.string.home_selinux_status_unknown)
    }

    val resetSuPathTitle = stringResource(id = R.string.setting_reset_su_path)

    val launcherIconTitle = stringResource(id = R.string.settings_alt_icon)
    val launcherIconSummary = stringResource(id = R.string.alt_icon_summary)

    val appTitleTitle = stringResource(id = R.string.settings_app_title)
    var currentAppTitle by remember { mutableStateOf(prefs.getString("app_title", "folkpatch") ?: "folkpatch") }
    val appTitleLabel = when (currentAppTitle) {
        "custom" -> remember { prefs.getString("custom_app_title", "FolkPatch") } ?: stringResource(R.string.app_title_custom)
        "fpatch" -> stringResource(R.string.app_title_fpatch)
        "apatch_folk" -> stringResource(R.string.app_title_apatch_folk)
        "apatchx" -> stringResource(R.string.app_title_apatchx)
        "apatch" -> stringResource(R.string.app_title_apatch)
        "kernelpatch" -> stringResource(R.string.app_title_kernelpatch)
        "kernelsu" -> stringResource(R.string.app_title_kernelsu)
        "supersu" -> stringResource(R.string.app_title_supersu)
        "folksu" -> stringResource(R.string.app_title_fpatch)
        "superuser" -> stringResource(R.string.app_title_superuser)
        "superpatch" -> stringResource(R.string.app_title_superpatch)
        "magicpatch" -> stringResource(R.string.app_title_magicpatch)
        else -> stringResource(R.string.app_title_folkpatch)
    }

    val customAppTitleTitle = stringResource(id = R.string.settings_custom_app_title)
    var currentCustomAppTitle by remember { mutableStateOf(prefs.getString("custom_app_title", "FolkPatch") ?: "FolkPatch") }

    val desktopAppNameTitle = stringResource(id = R.string.desktop_app_name)
    var currentDesktopAppName by remember { mutableStateOf(prefs.getString("desktop_app_name", "FolkPatch") ?: "FolkPatch") }

    val dpiTitle = stringResource(id = R.string.settings_app_dpi)
    val currentDpiVal = DPIUtils.currentDpi
    val dpiValue = if (currentDpiVal == DPIUtils.DEFAULT_DPI) stringResource(id = R.string.system_default) else "${DPIUtils.getDpiFriendlyName(currentDpiVal)} ($currentDpiVal DPI)"

    val logTitle = stringResource(id = R.string.send_log)

    val cleanStorageTitle = stringResource(id = R.string.settings_clean_storage)
    val cleanStorageSummary = stringResource(id = R.string.settings_clean_storage_summary)

    val folkXEngineTitle = stringResource(id = R.string.settings_folkx_engine_title)
    val folkXEngineSummary = stringResource(id = R.string.settings_folkx_engine_summary)

    val predictiveBackTitle = stringResource(id = R.string.settings_predictive_back)
    val predictiveBackSummary = stringResource(id = R.string.settings_predictive_back_summary)

    val appListLoadingSchemeTitle = stringResource(id = R.string.settings_app_list_loading_scheme)
    var currentScheme by remember { mutableStateOf(prefs.getString("app_list_loading_scheme", "root_service") ?: "root_service") }
    val currentSchemeLabel = if (currentScheme == "root_service") stringResource(R.string.app_list_loading_scheme_root_service) else stringResource(R.string.app_list_loading_scheme_package_manager)
    val newAppProfileTitle = stringResource(id = R.string.settings_new_app_profile_mode)

    val blockUpdateTitle = stringResource(id = R.string.settings_block_kernelpatch_update)
    val blockUpdateSummary = stringResource(id = R.string.settings_block_kernelpatch_update_summary)

    val blockApUpdateTitle = stringResource(id = R.string.settings_block_androidpatch_update)
    val blockApUpdateSummary = stringResource(id = R.string.settings_block_androidpatch_update_summary)

    val showUpdateDialog = remember { mutableStateOf(false) }
    val showResetSuPathDialog = remember { mutableStateOf(false) }
    val showCleanStorageDialog = remember { mutableStateOf(false) }
    val showAppTitleDialog = remember { mutableStateOf(false) }
    val showCustomAppTitleDialog = remember { mutableStateOf(false) }
    val showDesktopAppNameDialog = remember { mutableStateOf(false) }
    val showDpiDialog = remember { mutableStateOf(false) }
    val showFolkXAnimationTypeDialog = remember { mutableStateOf(false) }
    val showFolkXAnimationSpeedDialog = remember { mutableStateOf(false) }
    val showAppListLoadingSchemeDialog = remember { mutableStateOf(false) }
    val showNewAppProfileModeDialog = remember { mutableStateOf(false) }
    val showSELinuxModeDialog = remember { mutableStateOf(false) }

    val useAltIcon = remember { mutableStateOf(prefs.getBoolean("use_alt_icon", false)) }
    var autoUpdateCheck by remember { mutableStateOf(prefs.getBoolean("auto_update_check", true)) }
    var blockUpdateChecked by remember { mutableStateOf(prefs.getBoolean(APApplication.PREF_BLOCK_KERNELPATCH_UPDATE, false)) }
    var blockApUpdateChecked by remember { mutableStateOf(prefs.getBoolean(APApplication.PREF_BLOCK_ANDROIDPATCH_UPDATE, false)) }
    var folkXEngineEnabled by remember { mutableStateOf(prefs.getBoolean("folkx_engine_enabled", true)) }
    var currentType by remember { mutableStateOf(prefs.getString("folkx_animation_type", "linear") ?: "linear") }
    var currentSpeed by remember { mutableStateOf(prefs.getFloat("folkx_animation_speed", 1.0f)) }
    var predictiveBackEnabled by remember { mutableStateOf(prefs.getBoolean("predictive_back_enabled", true)) }

    val newAppProfileEnabledTitle = stringResource(id = R.string.settings_new_app_profile_enabled)
    val newAppProfileEnabledSummary = stringResource(id = R.string.settings_new_app_profile_enabled_summary)
    var newAppProfileEnabled by remember {
        mutableStateOf(prefs.getBoolean(APApplication.PREF_NEW_APP_PROFILE_ENABLED, false))
    }
    var newAppProfileMode by remember {
        mutableIntStateOf(prefs.getInt(APApplication.PREF_AUTO_EXCLUDE_NEW_APPS, 0))
    }
    LaunchedEffect(Unit) {
        newAppProfileMode = loadNewAppProfileMode(prefs)
    }
    val currentNewAppProfileLabel = when (newAppProfileMode) {
        1 -> stringResource(R.string.settings_new_app_profile_root)
        2 -> stringResource(R.string.settings_new_app_profile_exclude)
        else -> stringResource(R.string.settings_new_app_profile_normal)
    }

    FolkSettingsSection(title = stringResource(R.string.settings_section_general_basics)) {
     FolkSettingsGroup(flat = flat, highlightKey = highlightKey) {

        item(key = "general_language") {
            FolkValuePreference(
                icon = Icons.Outlined.Translate,
                title = languageTitle,
                summary = languageValue,
                onClick = { navigator.navigate(LanguagePickerScreenDestination) },
            )
        }

        item(key = "general_check_update") {
            FolkNavigationPreference(
                icon = Icons.Outlined.Update,
                title = updateTitle,
                onClick = {
                    scope.launch {
                        loadingDialog.show()
                        val hasUpdate = UpdateChecker.checkUpdate()
                        loadingDialog.hide()
                        if (hasUpdate) {
                            showUpdateDialog.value = true
                        } else {
                            showToast(context, R.string.update_latest)
                        }
                    }
                },
            )
        }

        item(key = "general_auto_update") {
            FolkSwitchPreference(
                icon = Icons.Outlined.Autorenew,
                title = autoUpdateTitle,
                summary = autoUpdateSummary,
                checked = autoUpdateCheck,
                onCheckedChange = {
                    autoUpdateCheck = it
                    prefs.edit { putBoolean("auto_update_check", it) }
                },
            )
        }

        item(key = "general_block_kp_update") {
            FolkSwitchPreference(
                icon = Icons.Outlined.Block,
                title = blockUpdateTitle,
                summary = blockUpdateSummary,
                checked = blockUpdateChecked,
                onCheckedChange = {
                    blockUpdateChecked = it
                    prefs.edit { putBoolean(APApplication.PREF_BLOCK_KERNELPATCH_UPDATE, it) }
                },
            )
        }

        item(key = "general_block_ap_update") {
            FolkSwitchPreference(
                icon = Icons.Outlined.Block,
                title = blockApUpdateTitle,
                summary = blockApUpdateSummary,
                checked = blockApUpdateChecked,
                onCheckedChange = {
                    blockApUpdateChecked = it
                    prefs.edit { putBoolean(APApplication.PREF_BLOCK_ANDROIDPATCH_UPDATE, it) }
                },
            )
        }

     }
    }

    FolkSettingsSection(title = stringResource(R.string.settings_section_general_interface)) {
     FolkSettingsGroup(flat = flat, highlightKey = highlightKey) {

        item(key = "general_folkx_engine") {
            FolkSwitchPreference(
                icon = Icons.Outlined.AutoAwesome,
                title = folkXEngineTitle,
                summary = folkXEngineSummary,
                checked = folkXEngineEnabled,
                onCheckedChange = {
                    folkXEngineEnabled = it
                    prefs.edit().putBoolean("folkx_engine_enabled", it).apply()
                },
            )
        }

        item(key = "general_folkx_animation_type", visible = folkXEngineEnabled) {
            val animationTypeLabel = when (currentType) {
                "linear" -> R.string.settings_folkx_animation_linear
                "spatial" -> R.string.settings_folkx_animation_spatial
                "fade" -> R.string.settings_folkx_animation_fade
                "vertical" -> R.string.settings_folkx_animation_vertical
                "diagonal" -> R.string.settings_folkx_animation_diagonal
                else -> R.string.settings_folkx_animation_linear
            }

            FolkValuePreference(
                icon = Icons.Outlined.Animation,
                title = stringResource(R.string.settings_folkx_animation_type),
                summary = stringResource(animationTypeLabel),
                onClick = { showFolkXAnimationTypeDialog.value = true },
            )
        }

        item(key = "general_folkx_animation_speed", visible = folkXEngineEnabled) {
            FolkValuePreference(
                icon = Icons.Outlined.Speed,
                title = stringResource(R.string.settings_folkx_animation_speed),
                summary = "${currentSpeed}x",
                onClick = { showFolkXAnimationSpeedDialog.value = true },
            )
        }

        item(key = "general_predictive_back", visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            FolkSwitchPreference(
                icon = Icons.Outlined.ArrowBack,
                title = predictiveBackTitle,
                summary = predictiveBackSummary,
                checked = predictiveBackEnabled,
                onCheckedChange = {
                    predictiveBackEnabled = it
                    prefs.edit { putBoolean("predictive_back_enabled", it) }
                    (context as? Activity)?.recreate()
                },
            )
        }

     }
    }

    if (kPatchReady) {
    FolkSettingsSection(title = stringResource(R.string.settings_section_general_root)) {
     FolkSettingsGroup(flat = flat, highlightKey = highlightKey) {

        item(key = "general_new_app_profile_enabled", visible = kPatchReady) {
            FolkSwitchPreference(
                icon = Icons.Outlined.AppRegistration,
                title = newAppProfileEnabledTitle,
                summary = newAppProfileEnabledSummary,
                checked = newAppProfileEnabled,
                onCheckedChange = {
                    if (it) {
                        val targetMode = prefs.getInt(APApplication.PREF_AUTO_EXCLUDE_NEW_APPS, 0)
                        val result = runCatching { Natives.setNewAppProfileMode(targetMode) }.getOrDefault(-1L)
                        if (result == 0L) {
                            newAppProfileEnabled = true
                            prefs.edit { putBoolean(APApplication.PREF_NEW_APP_PROFILE_ENABLED, true) }
                        } else {
                            newAppProfileEnabled = false
                            showToast(
                                context,
                                context.getString(R.string.settings_new_app_profile_update_failed, result.toString())
                            )
                        }
                    } else {
                        runCatching { Natives.setNewAppProfileMode(0) }
                        newAppProfileMode = 0
                        newAppProfileEnabled = false
                        prefs.edit {
                            putBoolean(APApplication.PREF_NEW_APP_PROFILE_ENABLED, false)
                            putInt(APApplication.PREF_AUTO_EXCLUDE_NEW_APPS, 0)
                        }
                    }
                },
            )
        }

        item(key = "general_new_app_profile", visible = kPatchReady && newAppProfileEnabled) {
            FolkValuePreference(
                icon = Icons.Outlined.SettingsApplications,
                title = newAppProfileTitle,
                summary = currentNewAppProfileLabel,
                onClick = { showNewAppProfileModeDialog.value = true },
            )
        }

        item(key = "general_app_list_scheme", visible = kPatchReady) {
            FolkValuePreference(
                icon = Icons.Outlined.List,
                title = appListLoadingSchemeTitle,
                summary = currentSchemeLabel,
                onClick = { showAppListLoadingSchemeDialog.value = true },
            )
        }

        item(key = "general_selinux_mode", visible = kPatchReady && aPatchReady) {
            FolkValuePreference(
                icon = Icons.Outlined.Security,
                title = selinuxModeTitle,
                summary = stringResource(R.string.settings_selinux_current_mode, selinuxModeValue),
                onClick = { showSELinuxModeDialog.value = true },
            )
        }

        item(key = "general_global_namespace", visible = kPatchReady && aPatchReady) {
            FolkSwitchPreference(
                icon = Icons.Outlined.Public,
                title = globalNamespaceTitle,
                summary = globalNamespaceSummary,
                checked = isGlobalNamespaceEnabled,
                enabled = namespaceLoaded,
                onCheckedChange = {
                    setGlobalNamespaceEnabled(if (isGlobalNamespaceEnabled) "0" else "1")
                    onGlobalNamespaceChange(it)
                },
            )
        }

     }
    }

    if (kPatchReady && aPatchReady) {
    FolkSettingsSection(title = stringResource(R.string.settings_section_general_mount)) {
     FolkSettingsGroup(flat = flat, highlightKey = highlightKey) {

        item(key = "general_magic_mount", visible = kPatchReady && aPatchReady) {
            FolkSwitchPreference(
                icon = Icons.Outlined.FolderSpecial,
                title = magicMountTitle,
                summary = magicMountSummary,
                checked = isMagicMountEnabled,
                onCheckedChange = {
                    setMagicMountEnabled(it)
                    onMagicMountChange(it)
                },
            )
        }

        item(key = "general_sucompat", visible = kPatchReady && aPatchReady) {
            var sucompatEnabled by remember { mutableStateOf(prefs.getBoolean("sucompat_enabled", false)) }
            FolkSwitchPreference(
                icon = Icons.Outlined.FeaturedPlayList,
                title = stringResource(id = R.string.settings_sucompat),
                summary = stringResource(id = R.string.settings_sucompat_summary),
                checked = sucompatEnabled,
                onCheckedChange = { enabled ->
                    scope.launch {
                        val result = if (enabled) {
                            // Enable: create marker file and register hooks via supercall
                            rootShellForResult("touch ${APApplication.SUCOMPAT_FILE}")
                            Natives.controlFeature("sucompat_extra", true)
                        } else {
                            // Disable: remove marker file and unregister hooks via supercall
                            rootShellForResult("rm -f ${APApplication.SUCOMPAT_FILE}")
                            Natives.controlFeature("sucompat_extra", false)
                        }
                        if (result == 0L) {
                            sucompatEnabled = enabled
                            prefs.edit().putBoolean("sucompat_enabled", enabled).apply()
                        }
                    }
                }
            )
        }
        item(key = "general_selinux_hide", visible = kPatchReady && aPatchReady) {
            val kernelVersion = remember { getKernelVersionCode() }
            val kernelSupported = (kernelVersion ?: 0) >= 419
            val isGki = remember { isGkiKernel() }
            var selinuxHideEnabled by rememberSaveable {
                mutableStateOf(prefs.getBoolean("selinux_hide_enabled", false))
            }
            val showSelinuxHideWarning = remember { mutableStateOf(false) }

            fun applySelinuxHide(enabled: Boolean) {
                scope.launch(Dispatchers.IO) {
                    val command = if (enabled) {
                        "touch ${APApplication.SELINUX_HIDE_FILE}"
                    } else {
                        "rm -f ${APApplication.SELINUX_HIDE_FILE}"
                    }
                    val result = rootShellForResult(command)
                    if (result.isSuccess) {
                        selinuxHideEnabled = enabled
                        prefs.edit().putBoolean("selinux_hide_enabled", enabled).apply()
                    }
                }
            }

            FolkSwitchPreference(
                icon = Icons.Outlined.Security,
                title = stringResource(id = R.string.settings_selinux_hide),
                summary = stringResource(id = R.string.settings_selinux_hide_summary),
                checked = selinuxHideEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        // Only tested on 5.10+, and non-GKI carries a bigger risk, so warn first.
                        val below510 = (kernelVersion ?: 0) < 510
                        if (below510 || !isGki) {
                            showSelinuxHideWarning.value = true
                        } else {
                            applySelinuxHide(true)
                        }
                    } else {
                        applySelinuxHide(false)
                    }
                },
                enabled = kernelSupported,
            )

            if (showSelinuxHideWarning.value) {
                SelinuxHideWarningDialog(
                    showDialog = showSelinuxHideWarning,
                    kernelVersion = kernelVersion,
                    isGki = isGki,
                    onConfirm = { applySelinuxHide(true) },
                )
            }
        }
        item(key = "general_reset_su_path", visible = kPatchReady) {
            FolkNavigationPreference(
                icon = Icons.Outlined.LinkOff,
                title = resetSuPathTitle,
                onClick = { showResetSuPathDialog.value = true },
            )
        }

     }
    }

    }
    }
    FolkSettingsSection(title = stringResource(R.string.settings_section_general_identity)) {
     FolkSettingsGroup(flat = flat, highlightKey = highlightKey) {

        item(key = "general_alt_icon") {
            FolkSwitchPreference(
                icon = Icons.Outlined.Android,
                title = launcherIconTitle,
                summary = launcherIconSummary,
                checked = useAltIcon.value,
                onCheckedChange = {
                    prefs.edit { putBoolean("use_alt_icon", it) }
                    LauncherIconUtils.updateLauncherState(context)
                    useAltIcon.value = it
                },
            )
        }

        item(key = "general_app_title") {
            FolkValuePreference(
                icon = Icons.Outlined.Label,
                title = appTitleTitle,
                summary = appTitleLabel,
                onClick = { showAppTitleDialog.value = true },
            )
        }

        item(key = "general_custom_app_title", visible = currentAppTitle == "custom") {
            FolkValuePreference(
                icon = Icons.Outlined.Edit,
                title = customAppTitleTitle,
                summary = currentCustomAppTitle,
                onClick = { showCustomAppTitleDialog.value = true },
            )
        }

        item(key = "general_desktop_app_name") {
            FolkValuePreference(
                icon = Icons.Outlined.PhoneAndroid,
                title = desktopAppNameTitle,
                summary = currentDesktopAppName,
                onClick = { showDesktopAppNameDialog.value = true },
            )
        }

        item(key = "general_dpi") {
            FolkValuePreference(
                icon = Icons.Outlined.FormatSize,
                title = dpiTitle,
                summary = dpiValue,
                onClick = { showDpiDialog.value = true },
            )
        }

     }
    }

    FolkSettingsSection(title = stringResource(R.string.settings_section_general_maintenance)) {
     FolkSettingsGroup(flat = flat, highlightKey = highlightKey) {

        item(key = "general_send_log") {
            FolkNavigationPreference(
                icon = Icons.Outlined.BugReport,
                title = logTitle,
                onClick = {
                    scope.launch {
                        val bugreport = loadingDialog.withLoading {
                            withContext(Dispatchers.IO) {
                                getBugreportFile(context)
                            }
                        }

                        val uri: Uri = FileProvider.getUriForFile(
                            context,
                            "${BuildConfig.APPLICATION_ID}.fileprovider",
                            bugreport
                        )

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_STREAM, uri)
                            type = "application/gzip"
                            clipData = android.content.ClipData.newRawUri(null, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        context.startActivity(
                            Intent.createChooser(
                                shareIntent,
                                context.getString(R.string.send_log)
                            )
                        )
                    }
                },
            )
        }

        item(key = "general_clean_storage") {
            FolkValuePreference(
                icon = Icons.Outlined.CleaningServices,
                title = cleanStorageTitle,
                summary = cleanStorageSummary,
                onClick = { showCleanStorageDialog.value = true },
            )
        }

     }
    }

    if (showUpdateDialog.value) {
        UpdateDialog(
            onDismiss = { showUpdateDialog.value = false },
            onUpdate = {
                showUpdateDialog.value = false
                UpdateChecker.openUpdateUrl(context)
            }
        )
    }

    if (showResetSuPathDialog.value) {
        ResetSUPathDialog(showResetSuPathDialog)
    }

    if (showCleanStorageDialog.value) {
        CleanStorageDialog(showCleanStorageDialog)
    }

    if (showSELinuxModeDialog.value) {
        SELinuxModeDialog(
            showDialog = showSELinuxModeDialog,
            currentMode = currentSELinuxMode,
            onModeChanged = onSELinuxModeChange
        )
    }

    if (showAppTitleDialog.value) {
        AppTitleChooseDialog(showAppTitleDialog) { newTitle ->
            currentAppTitle = newTitle
        }
    }

    if (showCustomAppTitleDialog.value) {
        CustomAppTitleDialog(showCustomAppTitleDialog, snackBarHost) { newTitle ->
            currentCustomAppTitle = newTitle
        }
    }

    if (showDesktopAppNameDialog.value) {
        DesktopAppNameChooseDialog(showDesktopAppNameDialog) { newName ->
            currentDesktopAppName = newName
        }
    }

    if (showDpiDialog.value) {
        DpiChooseDialog(showDpiDialog)
    }

    if (showFolkXAnimationTypeDialog.value) {
        FolkXAnimationTypeDialog(showFolkXAnimationTypeDialog) { newType ->
            currentType = newType
        }
    }

    if (showFolkXAnimationSpeedDialog.value) {
        FolkXAnimationSpeedDialog(showFolkXAnimationSpeedDialog) { newSpeed ->
            currentSpeed = newSpeed
        }
    }

    if (showAppListLoadingSchemeDialog.value) {
        AppListLoadingSchemeDialog(showAppListLoadingSchemeDialog) { newScheme ->
            currentScheme = newScheme
        }
    }

    if (showNewAppProfileModeDialog.value) {
        NewAppProfileModeDialog(showNewAppProfileModeDialog, newAppProfileMode) { mode ->
            newAppProfileMode = mode
            prefs.edit { putInt(APApplication.PREF_AUTO_EXCLUDE_NEW_APPS, mode) }
        }
    }
}

