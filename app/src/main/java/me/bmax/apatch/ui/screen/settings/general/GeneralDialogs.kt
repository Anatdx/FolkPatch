@file:OptIn(ExperimentalMaterial3Api::class)

package me.bmax.apatch.ui.screen.settings.general

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.FolkButtonDefaults
import me.bmax.apatch.util.*
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils
import me.bmax.apatch.util.ui.showToast
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTitleChooseDialog(showDialog: MutableState<Boolean>, onTitleChanged: (String) -> Unit = {}) {
    val prefs = APApplication.sharedPreferences
    val currentTitle = remember { prefs.getString("app_title", "folkpatch") }
    val titles = listOf(
        "custom" to stringResource(R.string.app_title_custom),
        "fpatch" to stringResource(R.string.app_title_fpatch),
        "apatch_folk" to stringResource(R.string.app_title_apatch_folk),
        "apatchx" to stringResource(R.string.app_title_apatchx),
        "apatch" to stringResource(R.string.app_title_apatch),
        "folkpatch" to stringResource(R.string.app_title_folkpatch),
        "kernelpatch" to stringResource(R.string.app_title_kernelpatch),
        "kernelsu" to stringResource(R.string.app_title_kernelsu),
        "supersu" to stringResource(R.string.app_title_supersu),
        "folksu" to stringResource(R.string.app_title_fpatch),
        "superuser" to stringResource(R.string.app_title_superuser),
        "superpatch" to stringResource(R.string.app_title_superpatch),
        "magicpatch" to stringResource(R.string.app_title_magicpatch)
    )

    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            LazyColumn {
                items(titles.size, key = { it }) { index ->
                    val (key, displayName) = titles[index]
                    ListItem(
                        headlineContent = { Text(text = displayName) },
                        modifier = Modifier.clickable {
                            showDialog.value = false
                            prefs.edit { putString("app_title", key) }
                            onTitleChanged(key)
                        },
                        trailingContent = {
                            if (currentTitle == key) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        }
                    )
                }
            }

            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAppTitleDialog(showDialog: MutableState<Boolean>, snackBarHost: SnackbarHostState, onTitleChanged: (String) -> Unit = {}) {
    val prefs = APApplication.sharedPreferences
    var customTitle by remember {
        mutableStateOf(prefs.getString("custom_app_title", "FolkPatch") ?: "FolkPatch")
    }

    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight()
                .padding(24.dp),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.custom_app_title_dialog_title),
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = customTitle,
                    onValueChange = { customTitle = it },
                    placeholder = { Text(stringResource(R.string.custom_app_title_dialog_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(onClick = {
                        val trimmed = customTitle.trim()
                        if (trimmed.isEmpty()) {
                            showDialog.value = false
                            return@TextButton
                        }
                        prefs.edit { putString("custom_app_title", trimmed) }
                        onTitleChanged(trimmed)
                        showDialog.value = false
                    }) {
                        Text(stringResource(R.string.custom_app_title_dialog_confirm))
                    }
                }
            }

            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopAppNameChooseDialog(showDialog: MutableState<Boolean>, onNameChanged: (String) -> Unit = {}) {
    val prefs = APApplication.sharedPreferences
    val context = LocalContext.current
    val currentName = remember { prefs.getString("desktop_app_name", "FolkPatch") }
    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            LazyColumn {
                item {
                    ListItem(
                        headlineContent = { Text(text = "FolkPatch") },
                        modifier = Modifier.clickable {
                            showDialog.value = false
                            prefs.edit {
                                putString("desktop_app_name", "FolkPatch")
                            }
                            onNameChanged("FolkPatch")
                            LauncherIconUtils.applySaved(context)
                        },
                        trailingContent = {
                            if (currentName == "FolkPatch" || currentName == null) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text(text = "FPatch") },
                        modifier = Modifier.clickable {
                            showDialog.value = false
                            prefs.edit {
                                putString("desktop_app_name", "FPatch")
                            }
                            onNameChanged("FPatch")
                            LauncherIconUtils.applySaved(context)
                        },
                        trailingContent = {
                            if (currentName == "FPatch") {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        }
                    )
                }
            }

            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolkXAnimationTypeDialog(showDialog: MutableState<Boolean>, onTypeChanged: (String) -> Unit = {}) {
    val prefs = APApplication.sharedPreferences

    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.settings_folkx_animation_type),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val currentType = remember { prefs.getString("folkx_animation_type", "linear") }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AlertDialogDefaults.containerColor,
                    tonalElevation = 2.dp
                ) {
                    Column {
                        listOf("linear", "spatial", "fade", "vertical", "diagonal").forEach { type ->
                            val labelId = when (type) {
                                "linear" -> R.string.settings_folkx_animation_linear
                                "spatial" -> R.string.settings_folkx_animation_spatial
                                "fade" -> R.string.settings_folkx_animation_fade
                                "vertical" -> R.string.settings_folkx_animation_vertical
                                "diagonal" -> R.string.settings_folkx_animation_diagonal
                                else -> R.string.settings_folkx_animation_linear
                            }
                            ListItem(
                                headlineContent = { Text(stringResource(labelId)) },
                                leadingContent = {
                                    RadioButton(
                                        selected = currentType == type,
                                        onClick = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    prefs.edit().putString("folkx_animation_type", type).apply()
                                    onTypeChanged(type)
                                    showDialog.value = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text(stringResource(id = android.R.string.cancel))
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListLoadingSchemeDialog(showDialog: MutableState<Boolean>, onSchemeChanged: (String) -> Unit = {}) {
    val prefs = APApplication.sharedPreferences

    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.settings_app_list_loading_scheme),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val currentScheme = remember { prefs.getString("app_list_loading_scheme", "root_service") }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AlertDialogDefaults.containerColor,
                    tonalElevation = 2.dp
                ) {
                    Column {
                        val schemes = listOf(
                            "root_service" to R.string.app_list_loading_scheme_root_service,
                            "package_manager" to R.string.app_list_loading_scheme_package_manager
                        )

                        schemes.forEach { (scheme, labelId) ->
                            ListItem(
                                headlineContent = { Text(stringResource(labelId)) },
                                leadingContent = {
                                    RadioButton(
                                        selected = currentScheme == scheme,
                                        onClick = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    prefs.edit { putString("app_list_loading_scheme", scheme) }
                                    onSchemeChanged(scheme)
                                    showDialog.value = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text(stringResource(id = android.R.string.cancel))
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetSUPathDialog(showDialog: MutableState<Boolean>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var suPath by remember { mutableStateOf("/system/bin/su") }
    LaunchedEffect(Unit) {
        suPath = withContext(Dispatchers.IO) {
            runCatching { me.bmax.apatch.Natives.suPath() }.getOrDefault("/system/bin/su")
        }
    }
    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(PaddingValues(all = 24.dp))) {
                Box(
                    Modifier
                        .padding(PaddingValues(bottom = 16.dp))
                        .align(Alignment.Start)
                ) {
                    Text(
                        text = stringResource(id = R.string.setting_reset_su_path),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                Box(
                    Modifier
                        .weight(weight = 1f, fill = false)
                        .padding(PaddingValues(bottom = 12.dp))
                        .align(Alignment.Start)
                ) {
                    OutlinedTextField(
                        value = suPath,
                        onValueChange = {
                            suPath = it
                        },
                        label = { Text(stringResource(id = R.string.setting_reset_su_new_path)) },
                        visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {

                        Text(stringResource(id = android.R.string.cancel))
                    }

                    Button(enabled = suPath.startsWith("/") && suPath.trim().length > 1, onClick = {
                        showDialog.value = false
                        val newPath = suPath.trim()
                        scope.launch {
                            val success = withContext(Dispatchers.IO) {
                                runCatching {
                                    val reset = me.bmax.apatch.Natives.resetSuPath(newPath)
                                    if (reset) {
                                        rootShellForResult(
                                            "printf %s ${newPath.shellSingleQuoted()} > ${APApplication.SU_PATH_FILE}"
                                        )
                                    }
                                    reset
                                }.getOrDefault(false)
                            }
                            showToast(context, if (success) R.string.success else R.string.failure)
                        }
                    }, colors = FolkButtonDefaults.filledColors()) {
                        Text(stringResource(id = android.R.string.ok))
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

private fun String.shellSingleQuoted(): String {
    return "'" + replace("'", "'\\''") + "'"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanStorageDialog(showDialog: MutableState<Boolean>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(PaddingValues(all = 24.dp))) {
                Text(
                    text = stringResource(id = R.string.settings_clean_storage),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = stringResource(id = R.string.settings_clean_storage_confirm),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text(stringResource(id = android.R.string.cancel))
                    }

                    Button(onClick = {
                        showDialog.value = false
                        scope.launch {
                            val success = withContext(Dispatchers.IO) {
                                runCatching {
                                    // 删除容易因残留/占位文件导致下载或应用失败的目录，随后重建空目录
                                    listOf("themes", "music", "sound_effects").forEach { name ->
                                        val dir = File(context.filesDir, name)
                                        if (dir.exists()) dir.deleteRecursively()
                                        dir.mkdirs()
                                    }
                                    context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
                                }.isSuccess
                            }
                            showToast(context, if (success) R.string.settings_clean_storage_done else R.string.failure)
                        }
                    }, colors = FolkButtonDefaults.filledColors()) {
                        Text(stringResource(id = android.R.string.ok))
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolkXAnimationSpeedDialog(showDialog: MutableState<Boolean>, onSpeedChanged: (Float) -> Unit = {}) {
    val prefs = APApplication.sharedPreferences

    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.settings_folkx_animation_speed),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val currentSpeed = remember { prefs.getFloat("folkx_animation_speed", 1.0f) }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AlertDialogDefaults.containerColor,
                    tonalElevation = 2.dp
                ) {
                    Column {
                        val speeds = listOf(
                            0.5f to "0.5x",
                            0.75f to "0.75x",
                            1.0f to "1.0x",
                            1.25f to "1.25x",
                            1.5f to "1.5x",
                            2.0f to "2.0x"
                        )

                        speeds.forEach { (speed, label) ->
                            ListItem(
                                headlineContent = { Text(label) },
                                leadingContent = {
                                    RadioButton(
                                        selected = currentSpeed == speed,
                                        onClick = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    prefs.edit().putFloat("folkx_animation_speed", speed).apply()
                                    onSpeedChanged(speed)
                                    showDialog.value = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text(stringResource(id = android.R.string.cancel))
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}
