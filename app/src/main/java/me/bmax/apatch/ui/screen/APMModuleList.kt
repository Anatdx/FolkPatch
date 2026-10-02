package me.bmax.apatch.ui.screen

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Patterns
import me.bmax.apatch.util.ui.showToast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import me.bmax.apatch.ui.component.TwoColumnGrid
import me.bmax.apatch.ui.component.splicedLazyColumnGroup
import me.bmax.apatch.ui.component.WarningCard
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import me.bmax.apatch.ui.component.ConfirmResult
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.viewmodel.APModuleViewModel
import me.bmax.apatch.util.DownloadListener
import me.bmax.apatch.util.download
import me.bmax.apatch.util.isJailbreakMode
import me.bmax.apatch.util.reboot
import me.bmax.apatch.util.toggleModule
import me.bmax.apatch.util.uninstallModule
import me.bmax.apatch.util.undoUninstallModule
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.Icons
import me.bmax.apatch.ui.navigation.fabNavBottomClearance
import androidx.compose.ui.platform.LocalConfiguration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleList(
    navigator: DestinationsNavigator,
    viewModel: APModuleViewModel,
    modules: List<APModuleViewModel.ModuleInfo>,
    showMoreModuleInfo: Boolean,
    foldSystemModule: Boolean,
    simpleListBottomBar: Boolean,
    splicedCardGroup: Boolean,
    checkStrongBiometric: suspend () -> Boolean,
    modifier: Modifier = Modifier,
    state: LazyListState,
    onInstallModule: (Uri) -> Unit,
    onClickModule: (id: String, name: String, hasWebUi: Boolean) -> Unit,
    snackBarHost: SnackbarHostState,
    context: Context
) {
    var expandedModuleId by rememberSaveable { mutableStateOf<String?>(null) }

    // Warning Banner State
    val prefs = remember { APApplication.sharedPreferences }
    var showMountWarning by remember {
        mutableStateOf(!prefs.getBoolean("apm_mount_warning_shown", false))
    }
    val failedEnable = stringResource(R.string.apm_failed_to_enable)
    val failedDisable = stringResource(R.string.apm_failed_to_disable)
    val failedUninstall = stringResource(R.string.apm_uninstall_failed)
    val successUninstall = stringResource(R.string.apm_uninstall_success)
    val reboot = stringResource(id = R.string.reboot)
    val rebootToApply = stringResource(id = R.string.apm_reboot_to_apply)
    val moduleStr = stringResource(id = R.string.apm)
    val uninstall = stringResource(id = R.string.apm_remove)
    val cancel = stringResource(id = android.R.string.cancel)
    val moduleUninstallConfirm = stringResource(id = R.string.apm_uninstall_confirm)
    val updateText = stringResource(R.string.apm_update)
    val changelogText = stringResource(R.string.apm_changelog)
    val downloadingText = stringResource(R.string.apm_downloading)
    val startDownloadingText = stringResource(R.string.apm_start_downloading)

    // Enable Module Shortcut Add
    var enableModuleShortcutAdd by remember {
        mutableStateOf(prefs.getBoolean("enable_module_shortcut_add", true))
    }

    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "enable_module_shortcut_add") {
                enableModuleShortcutAdd = sharedPreferences.getBoolean("enable_module_shortcut_add", true)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val loadingDialog = rememberLoadingDialog()
    val confirmDialog = rememberConfirmDialog()

    suspend fun onModuleUpdate(
        module: APModuleViewModel.ModuleInfo,
        changelogUrl: String,
        downloadUrl: String,
        fileName: String
    ) {
        val changelog = loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                if (Patterns.WEB_URL.matcher(changelogUrl).matches()) {
                    apApp.okhttpClient.newCall(
                        okhttp3.Request.Builder().url(changelogUrl).build()
                    ).execute().body!!.string()
                } else {
                    changelogUrl
                }
            }
        }


        if (changelog.isNotEmpty()) {
            // changelog is not empty, show it and wait for confirm
            val confirmResult = confirmDialog.awaitConfirm(
                changelogText,
                content = changelog,
                markdown = true,
                confirm = updateText,
            )

            if (confirmResult != ConfirmResult.Confirmed) {
                return
            }
        }

        withContext(Dispatchers.Main) {
            showToast(context, startDownloadingText.format(module.name))
        }

        val downloading = downloadingText.format(module.name)
        withContext(Dispatchers.IO) {
            download(
                context,
                downloadUrl,
                fileName,
                downloading,
                onDownloaded = onInstallModule,
                onDownloading = {
                    launch(Dispatchers.Main) {
                        showToast(context, downloading)
                    }
                })
        }
    }

    suspend fun onModuleUninstall(module: APModuleViewModel.ModuleInfo) {
        if (!checkStrongBiometric()) return
        val confirmResult = confirmDialog.awaitConfirm(
            moduleStr,
            content = moduleUninstallConfirm.format(module.name),
            confirm = uninstall,
            dismiss = cancel
        )
        if (confirmResult != ConfirmResult.Confirmed) {
            return
        }

        val success = loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                uninstallModule(module.id)
            }
        }

        if (success) {
            viewModel.fetchModuleList()
        }
        val message = if (success) {
            successUninstall.format(module.name)
        } else {
            failedUninstall.format(module.name)
        }
        snackBarHost.showSnackbar(
            message = message, duration = SnackbarDuration.Short
        )
    }

    suspend fun onModuleUndoUninstall(module: APModuleViewModel.ModuleInfo) {
        if (!checkStrongBiometric()) return

        val success = loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                undoUninstallModule(module.id)
            }
        }

        if (success) {
            viewModel.fetchModuleList()
        }
        val message = if (success) {
            context.getString(R.string.apm_undo_uninstall_success).format(module.name)
        } else {
            context.getString(R.string.apm_undo_uninstall_failed).format(module.name)
        }
        snackBarHost.showSnackbar(
            message = message, duration = SnackbarDuration.Short
        )
    }

    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        modifier = modifier,
        onRefresh = { viewModel.fetchModuleList() },
        isRefreshing = viewModel.isRefreshing,
        state = pullToRefreshState,
        indicator = { PullToRefreshDefaults.LoadingIndicator(state = pullToRefreshState, isRefreshing = viewModel.isRefreshing, modifier = Modifier.align(Alignment.TopCenter)) }
    ) {
        val configuration = LocalConfiguration.current
        val isWideScreen = configuration.screenWidthDp >= 600

        if (isWideScreen) {
            TwoColumnGrid(
                modifier = Modifier.fillMaxSize(),
                items = if (modules.isEmpty()) emptyList() else modules,
                key = { module -> module.id },
                verticalSpacing = 16.dp,
                horizontalSpacing = 16.dp,
                contentPadding = run {
                    val bottomClearance = fabNavBottomClearance()
                    remember(bottomClearance) {
                        PaddingValues(
                            start = 16.dp,
                            top = 16.dp,
                            end = 16.dp,
                            bottom = bottomClearance
                        )
                    }
                },
                beforeItems = {
                    if (showMountWarning) {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            WarningCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                message = stringResource(R.string.apm_mount_warning_message),
                                onClose = {
                                    prefs.edit()
                                        .putBoolean("apm_mount_warning_shown", true)
                                        .apply()
                                    showMountWarning = false
                                }
                            )
                        }
                    }
                    if (modules.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (viewModel.errorMessage != null && !viewModel.isRefreshing) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = viewModel.errorMessage ?: stringResource(R.string.apm_load_failed),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Button(onClick = { viewModel.fetchModuleList() }) {
                                        Text(stringResource(R.string.retry))
                                    }
                                }
                            } else {
                                Text(
                                    stringResource(R.string.apm_empty), textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                },
                itemContent = { module ->
                    var isChecked by rememberSaveable(module) { mutableStateOf(module.enabled) }
                    val scope = rememberCoroutineScope()
                    val updatedModule = viewModel.getCachedUpdate(module.id)

                    ModuleItem(
                        navigator,
                        module,
                        isChecked,
                        updatedModule.first,
                        showMoreModuleInfo = showMoreModuleInfo,
                        foldSystemModule = foldSystemModule,
                        simpleListBottomBar = simpleListBottomBar,
                        enableModuleShortcutAdd = enableModuleShortcutAdd,
                        expanded = expandedModuleId == module.id,
                        onExpandToggle = {
                            expandedModuleId = if (expandedModuleId == module.id) null else module.id
                        },
                        onUninstall = {
                            scope.launch { onModuleUninstall(module) }
                        },
                        onUndoUninstall = {
                            scope.launch { onModuleUndoUninstall(module) }
                        },
                        onCheckChanged = { checked ->
                            scope.launch {
                                if (!checkStrongBiometric()) return@launch
                                val success = loadingDialog.withLoading {
                                    withContext(Dispatchers.IO) {
                                        toggleModule(module.id, !isChecked)
                                    }
                                }
                                if (success) {
                                    isChecked = checked
                                    viewModel.fetchModuleList()

                                    // In jailbreak mode a full reboot would unload the
                                    // runtime-loaded module, so apply without the prompt.
                                    if (!withContext(Dispatchers.IO) { isJailbreakMode() }) {
                                        val result = snackBarHost.showSnackbar(
                                            message = rebootToApply,
                                            actionLabel = reboot,
                                            duration = SnackbarDuration.Long
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            reboot()
                                        }
                                    }
                                } else {
                                    val message = if (isChecked) failedDisable else failedEnable
                                    snackBarHost.showSnackbar(message.format(module.name))
                                }
                            }
                        },
                        onUpdate = {
                            scope.launch {
                                onModuleUpdate(
                                    module,
                                    updatedModule.third,
                                    updatedModule.first,
                                    "${module.name}-${updatedModule.second}.zip"
                                )
                            }
                        },
                        onClick = { clickedModule ->
                            onClickModule(clickedModule.id, clickedModule.name, clickedModule.hasWebUi)
                        })
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = state,
                contentPadding = run {
                    val bottomClearance = fabNavBottomClearance()
                    remember(bottomClearance) {
                        PaddingValues(
                            start = 0.dp,
                            top = 16.dp,
                            end = 0.dp,
                            bottom = bottomClearance
                        )
                    }
                },
            ) {
                // Warning Banner
                if (showMountWarning) {
                    item {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            WarningCard(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                message = stringResource(R.string.apm_mount_warning_message),
                                onClose = {
                                    prefs.edit()
                                        .putBoolean("apm_mount_warning_shown", true)
                                        .apply()
                                    showMountWarning = false
                                }
                            )
                        }
                    }
                }

                when {
                    viewModel.errorMessage != null && !viewModel.isRefreshing -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillParentMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = viewModel.errorMessage ?: stringResource(R.string.apm_load_failed),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Button(onClick = { viewModel.fetchModuleList() }) {
                                        Text(stringResource(R.string.retry))
                                    }
                                }
                            }
                        }
                    }

                    modules.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillParentMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.apm_empty), textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    else -> {
                        if (splicedCardGroup) {
                            item { Spacer(Modifier.height(8.dp)) }
                            splicedLazyColumnGroup(
                                items = modules,
                                key = { _, module -> module.id },
                                contentType = { _, _ -> "ModuleItem" },
                            ) { _, module ->
                                var isChecked by rememberSaveable(module) { mutableStateOf(module.enabled) }
                                val scope = rememberCoroutineScope()
                                val updatedModule = viewModel.getCachedUpdate(module.id)

                                ModuleItem(
                                    navigator,
                                    module,
                                    isChecked,
                                    updatedModule.first,
                                    showMoreModuleInfo = showMoreModuleInfo,
                                    foldSystemModule = foldSystemModule,
                                    simpleListBottomBar = simpleListBottomBar,
                                    enableModuleShortcutAdd = enableModuleShortcutAdd,
                                    expanded = expandedModuleId == module.id,
                                    onExpandToggle = {
                                        expandedModuleId = if (expandedModuleId == module.id) null else module.id
                                    },
                                    onUninstall = {
                                        scope.launch { onModuleUninstall(module) }
                                    },
                                    onUndoUninstall = {
                                        scope.launch { onModuleUndoUninstall(module) }
                                    },
                                    onCheckChanged = { checked ->
                                        scope.launch {
                                            if (!checkStrongBiometric()) return@launch
                                            val success = loadingDialog.withLoading {
                                                withContext(Dispatchers.IO) {
                                                    toggleModule(module.id, !isChecked)
                                                }
                                            }
                                            if (success) {
                                                isChecked = checked
                                                viewModel.fetchModuleList()

                                                // In jailbreak mode a full reboot would unload the
                                                // runtime-loaded module, so apply without the prompt.
                                                if (!withContext(Dispatchers.IO) { isJailbreakMode() }) {
                                                    val result = snackBarHost.showSnackbar(
                                                        message = rebootToApply,
                                                        actionLabel = reboot,
                                                        duration = SnackbarDuration.Long
                                                    )
                                                    if (result == SnackbarResult.ActionPerformed) {
                                                        reboot()
                                                    }
                                                }
                                            } else {
                                                val message = if (isChecked) failedDisable else failedEnable
                                                snackBarHost.showSnackbar(message.format(module.name))
                                            }
                                        }
                                    },
                                    onUpdate = {
                                        scope.launch {
                                            onModuleUpdate(
                                                module,
                                                updatedModule.third,
                                                updatedModule.first,
                                                "${module.name}-${updatedModule.second}.zip"
                                            )
                                        }
                                    },
                                    onClick = { clickedModule ->
                                        onClickModule(clickedModule.id, clickedModule.name, clickedModule.hasWebUi)
                                    })
                            }
                            item { Spacer(Modifier.height(8.dp)) } // bottom clearance handled by contentPadding
                        } else {
                            item { Spacer(Modifier.height(8.dp)) }
                            itemsIndexed(modules, key = { _, module -> module.id }) { _, module ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                var isChecked by rememberSaveable(module) { mutableStateOf(module.enabled) }
                                val scope = rememberCoroutineScope()
                                val updatedModule = viewModel.getCachedUpdate(module.id)

                                ModuleItem(
                                    navigator,
                                    module,
                                    isChecked,
                                    updatedModule.first,
                                    showMoreModuleInfo = showMoreModuleInfo,
                                    foldSystemModule = foldSystemModule,
                                    simpleListBottomBar = simpleListBottomBar,
                                    enableModuleShortcutAdd = enableModuleShortcutAdd,
                                    expanded = expandedModuleId == module.id,
                                    onExpandToggle = {
                                        expandedModuleId = if (expandedModuleId == module.id) null else module.id
                                    },
                                    onUninstall = {
                                        scope.launch { onModuleUninstall(module) }
                                    },
                                    onUndoUninstall = {
                                        scope.launch { onModuleUndoUninstall(module) }
                                    },
                                    onCheckChanged = { checked ->
                                        scope.launch {
                                            if (!checkStrongBiometric()) return@launch
                                            val success = loadingDialog.withLoading {
                                                withContext(Dispatchers.IO) {
                                                    toggleModule(module.id, !isChecked)
                                                }
                                            }
                                            if (success) {
                                                isChecked = checked
                                                viewModel.fetchModuleList()

                                                // In jailbreak mode a full reboot would unload the
                                                // runtime-loaded module, so apply without the prompt.
                                                if (!withContext(Dispatchers.IO) { isJailbreakMode() }) {
                                                    val result = snackBarHost.showSnackbar(
                                                        message = rebootToApply,
                                                        actionLabel = reboot,
                                                        duration = SnackbarDuration.Long
                                                    )
                                                    if (result == SnackbarResult.ActionPerformed) {
                                                        reboot()
                                                    }
                                                }
                                            } else {
                                                val message = if (isChecked) failedDisable else failedEnable
                                                snackBarHost.showSnackbar(message.format(module.name))
                                            }
                                        }
                                    },
                                    onUpdate = {
                                        scope.launch {
                                            onModuleUpdate(
                                                module,
                                                updatedModule.third,
                                                updatedModule.first,
                                                "${module.name}-${updatedModule.second}.zip"
                                            )
                                        }
                                    },
                                    onClick = { clickedModule ->
                                        onClickModule(clickedModule.id, clickedModule.name, clickedModule.hasWebUi)
                                    })

                                }
                            }
                            item { Spacer(Modifier.height(8.dp)) } // bottom clearance handled by contentPadding
                        }
                    }
                }
            }
        }

        DownloadListener(context, onInstallModule)
    }

}



