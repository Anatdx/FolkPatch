package me.bmax.apatch.ui.screen

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.util.Patterns
import me.bmax.apatch.util.ui.showToast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import me.bmax.apatch.ui.component.TwoColumnGrid
import me.bmax.apatch.ui.component.splicedLazyColumnGroup
import me.bmax.apatch.ui.component.WarningCard
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.FolkScaffold
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.WebUIActivity
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import me.bmax.apatch.ui.component.ConfirmResult
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.viewmodel.APModuleViewModel
import me.bmax.apatch.util.DownloadListener
import me.bmax.apatch.util.download
import me.bmax.apatch.util.hasMagisk
import me.bmax.apatch.util.isJailbreakMode
import me.bmax.apatch.util.reboot
import me.bmax.apatch.util.toggleModule
import me.bmax.apatch.util.ui.LocalSnackbarHost
import me.bmax.apatch.util.uninstallModule
import me.bmax.apatch.util.undoUninstallModule

import com.ramcosta.composedestinations.generated.destinations.ApmBulkInstallScreenDestination
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import me.bmax.apatch.util.apmCustomModuleInfoStorage
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.navigation.LocalBottomBarVisible
import me.bmax.apatch.ui.navigation.LocalIsFloatingNavMode
import me.bmax.apatch.ui.navigation.fabNavBottomClearance
import androidx.compose.ui.platform.LocalConfiguration

import me.bmax.apatch.util.BiometricUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Destination<RootGraph>
@Composable
fun APModuleScreen(navigator: DestinationsNavigator) {
    val snackBarHost = LocalSnackbarHost.current
    val context = LocalContext.current

    // First use dialog state
    val prefs = remember { APApplication.sharedPreferences }
    var showFirstTimeDialog by remember { 
        mutableStateOf(!prefs.getBoolean("apm_first_use_shown", false)) 
    }
    var dontShowAgain by remember { mutableStateOf(false) }

    var showMoreModuleInfo by remember { mutableStateOf(prefs.getBoolean("show_more_module_info", true)) }
    var foldSystemModule by remember { mutableStateOf(prefs.getBoolean("fold_system_module", true)) }
    var simpleListBottomBar by remember { mutableStateOf(prefs.getBoolean("simple_list_bottom_bar", false)) }
    var splicedCardGroup by remember { mutableStateOf(prefs.getBoolean("spliced_card_group", true)) }

    val viewModel = viewModel<APModuleViewModel>()

    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
            if (key == "show_more_module_info") {
                showMoreModuleInfo = sharedPrefs.getBoolean("show_more_module_info", true)
            } else if (key == "fold_system_module") {
                foldSystemModule = sharedPrefs.getBoolean("fold_system_module", false)
            } else if (key == "simple_list_bottom_bar") {
                simpleListBottomBar = sharedPrefs.getBoolean("simple_list_bottom_bar", false)
            } else if (key == "spliced_card_group") {
                splicedCardGroup = sharedPrefs.getBoolean("spliced_card_group", true)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val scope = rememberCoroutineScope()

    suspend fun checkStrongBiometric(): Boolean {
        val prefs = APApplication.sharedPreferences
        if (prefs.getBoolean("strong_biometric", false) && prefs.getBoolean("biometric_login", false)) {
            val activity = context as? androidx.fragment.app.FragmentActivity
            return if (activity != null) {
                BiometricUtils.authenticate(activity)
            } else {
                true
            }
        }
        return true
    }

    if (state != APApplication.State.ANDROIDPATCH_INSTALLED && state != APApplication.State.ANDROIDPATCH_NEED_UPDATE) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                Text(
                    text = stringResource(id = R.string.apm_not_installed),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        return
    }

    LaunchedEffect(Unit) {
        if (viewModel.moduleList.isEmpty() || viewModel.isNeedRefresh) {
            viewModel.fetchModuleList()
        }
    }

    var pendingInstallUri by remember { mutableStateOf<Uri?>(null) }
    val installConfirmDialog = rememberConfirmDialog(
        onConfirm = {
            pendingInstallUri?.let { uri ->
                navigator.navigate(InstallScreenDestination(uri, MODULE_TYPE.APM))
                viewModel.markNeedRefresh()
            }
            pendingInstallUri = null
        },
        onDismiss = {
            pendingInstallUri = null
        }
    )

    val webUILauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { viewModel.fetchModuleList() }
    val hasMagisk by produceState(initialValue = false) {
        value = withContext(Dispatchers.IO) { hasMagisk() }
    }
    val hideInstallButton = hasMagisk

    val moduleListState = rememberLazyListState()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredModuleList = remember(viewModel.moduleList, searchQuery) {
        if (searchQuery.isEmpty()) {
            viewModel.moduleList
        } else {
            viewModel.moduleList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true) ||
                        it.author.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(viewModel.moduleList) {
        if (viewModel.moduleList.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                apmCustomModuleInfoStorage.prune(viewModel.moduleList.map { it.id }.toSet())
            }
        }
    }

    FolkScaffold(
        topBar = {
        TopBar(
            navigator,
            viewModel,
            snackBarHost,
            searchQuery,
            ::checkStrongBiometric,
            onSearchQueryChange = { searchQuery = it },
            onToggleModuleBanner = {
                val newValue = !BackgroundConfig.isBannerEnabled
                BackgroundConfig.setBannerEnabledState(newValue)
                BackgroundConfig.save(context)
            }
        )
    }, floatingActionButton = if (hideInstallButton) {
        { /* Empty */ }
    } else {
        {
            val selectZipLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                if (it.resultCode != RESULT_OK) {
                    return@rememberLauncherForActivityResult
                }
                val data = it.data ?: return@rememberLauncherForActivityResult
                val uri = data.data ?: return@rememberLauncherForActivityResult

                Log.i("ModuleScreen", "select zip result: $uri")

                val prefs = APApplication.sharedPreferences
                if (prefs.getBoolean("apm_install_confirm_enabled", true)) {
                    pendingInstallUri = uri
                    val fileName = try {
                        var name = uri.path ?: "Module"
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (cursor.moveToFirst() && nameIndex >= 0) {
                                name = cursor.getString(nameIndex)
                            }
                        }
                        name
                    } catch (e: Exception) {
                        "Module"
                    }
                    installConfirmDialog.showConfirm(
                        title = context.getString(R.string.apm_install_confirm_title),
                        content = context.getString(R.string.apm_install_confirm_content, fileName),
                        markdown = false
                    )
                } else {
                    navigator.navigate(InstallScreenDestination(uri, MODULE_TYPE.APM))
                    viewModel.markNeedRefresh()
                }
            }

            val isFloatingMode = LocalIsFloatingNavMode.current
            val bottomBarVisible = LocalBottomBarVisible.current.value
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val animatedOffset by animateDpAsState(
                targetValue = if (isFloatingMode && bottomBarVisible && !isLandscape) (-88).dp else 0.dp,
                animationSpec = tween(durationMillis = 300),
                label = "fabOffset"
            )

            var fabExpanded by remember { mutableStateOf(false) }

            val fabContent: @Composable () -> Unit = {
                FloatingActionButtonMenu(
                    expanded = fabExpanded,
                    button = {
                        FloatingActionButton(
                            onClick = { fabExpanded = !fabExpanded },
                            shape = CircleShape,
                            contentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 1f),
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                        ) {
                            Crossfade(
                                targetState = fabExpanded,
                                animationSpec = tween(durationMillis = 200),
                                label = "fabIconCrossfade"
                            ) { isExpanded ->
                                if (isExpanded) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = null,
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.package_import),
                                        contentDescription = null,
                                    )
                                }
                            }
                        }
                    },
                ) {
                    // 批量刷入 (Bulk Install)
                    FloatingActionButtonMenuItem(
                        onClick = dropUnlessResumed {
                            fabExpanded = false
                            navigator.navigate(ApmBulkInstallScreenDestination())
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        text = { Text(text = stringResource(R.string.apm_bulk_install_action), style = MaterialTheme.typography.bodyMedium) },
                    )
                    // 安装 (Install)
                    FloatingActionButtonMenuItem(
                        onClick = {
                            fabExpanded = false
                            scope.launch {
                                if (checkStrongBiometric()) {
                                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                                    intent.type = "application/zip"
                                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                                    selectZipLauncher.launch(intent)
                                }
                            }
                        },
                        icon = { Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        text = { Text(text = stringResource(R.string.apm_install), style = MaterialTheme.typography.bodyMedium) },
                    )
                }
            }
            if (isFloatingMode) {
                Box(modifier = Modifier.offset(y = animatedOffset)) {
                    fabContent()
                }
            } else {
                fabContent()
            }
        }
    },
        snackbarHostState = snackBarHost,
        // The module lists already reserve room for the FAB and the floating
        // bar via fabNavBottomClearance.
        addBottomClearance = false,
    ) { innerPadding ->
        when {
            hasMagisk -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.apm_magisk_conflict),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            else -> {
                ModuleList(
                    navigator,
                    viewModel = viewModel,
                    modules = filteredModuleList,
                    showMoreModuleInfo = showMoreModuleInfo,
                    foldSystemModule = foldSystemModule,
                    simpleListBottomBar = simpleListBottomBar,
                    splicedCardGroup = splicedCardGroup,
                    checkStrongBiometric = ::checkStrongBiometric,
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    state = moduleListState,
                    onInstallModule = {
                        navigator.navigate(InstallScreenDestination(it, MODULE_TYPE.APM))
                    },
                    onClickModule = { id, name, hasWebUi ->
                        if (hasWebUi) {
                            webUILauncher.launch(
                                Intent(
                                    context, WebUIActivity::class.java
                                ).setData("apatch://webui/$id".toUri()).putExtra("id", id)
                                    .putExtra("name", name)
                            )
                        }
                    },
                    snackBarHost = snackBarHost,
                    context = context
                )
            }
        }
    }

    // First Use Dialog
    if (showFirstTimeDialog) {
        BasicAlertDialog(
            onDismissRequest = {
                if (dontShowAgain) {
                    prefs.edit().putBoolean("apm_first_use_shown", true).apply()
                }
                showFirstTimeDialog = false
            },
            properties = DialogProperties(
                dismissOnClickOutside = false,
                dismissOnBackPress = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .width(350.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = AlertDialogDefaults.TonalElevation,
                color = AlertDialogDefaults.containerColor,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.apm_first_use_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = stringResource(R.string.apm_first_use_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = it }
                        )
                        Text(
                            text = stringResource(R.string.dont_show_again),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = {
                            if (dontShowAgain) {
                                prefs.edit().putBoolean("apm_first_use_shown", true).apply()
                            }
                            showFirstTimeDialog = false
                        }) {
                            Text(stringResource(R.string.got_it))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModuleList(
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



