package me.bmax.apatch.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.FolkSettingsGroup
import me.bmax.apatch.ui.component.folk.FolkSettingsSection
import me.bmax.apatch.ui.component.folk.FolkSwitchPreference
import me.bmax.apatch.ui.component.folk.FolkValuePreference
import me.bmax.apatch.util.LauncherIconUtils

@Composable
fun GeneralIdentitySection(
    flat: Boolean,
    highlightKey: String?,
    launcherIconTitle: String,
    launcherIconSummary: String,
    useAltIcon: MutableState<Boolean>,
    appTitleTitle: String,
    appTitleLabel: String,
    currentAppTitle: String,
    showAppTitleDialog: MutableState<Boolean>,
    customAppTitleTitle: String,
    currentCustomAppTitle: String,
    showCustomAppTitleDialog: MutableState<Boolean>,
    desktopAppNameTitle: String,
    currentDesktopAppName: String,
    showDesktopAppNameDialog: MutableState<Boolean>,
    dpiTitle: String,
    dpiValue: String,
    showDpiDialog: MutableState<Boolean>,
) {
    val context = LocalContext.current
    val prefs = APApplication.sharedPreferences

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
}
