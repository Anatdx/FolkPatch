package me.bmax.apatch.ui.screen

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.topjohnwu.superuser.io.SuFile
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.automirrored.outlined.Wysiwyg
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import me.bmax.apatch.ui.component.ExpressiveSwitch
import me.bmax.apatch.ui.component.LocalInsideSplicedGroup
import me.bmax.apatch.ui.component.ModuleLabel
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.generated.destinations.ExecuteAPMActionScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.folkPressScale
import me.bmax.apatch.ui.component.AdaptiveModuleButtonRow
import me.bmax.apatch.ui.component.ModuleButtonConfig
import me.bmax.apatch.ui.component.BackgroundOptionsDialog
import me.bmax.apatch.ui.component.ModuleInfoData
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.viewmodel.APModuleViewModel
import me.bmax.apatch.util.ModuleShortcut
import me.bmax.apatch.util.getRootShell
import me.bmax.apatch.util.ui.LocalSnackbarHost
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import me.bmax.apatch.util.apmBannerStorage
import me.bmax.apatch.util.resolveModuleDir
import me.bmax.apatch.util.readModulePropBanner
import me.bmax.apatch.util.clearLegacyFolkBanner
import me.bmax.apatch.util.CustomModuleInfo
import me.bmax.apatch.util.apmCustomModuleInfoStorage
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.bannerFadeColor

@Composable
fun ModuleItem(
    navigator: DestinationsNavigator,
    module: APModuleViewModel.ModuleInfo,
    isChecked: Boolean,
    updateUrl: String,
    showMoreModuleInfo: Boolean,
    foldSystemModule: Boolean,
    simpleListBottomBar: Boolean,
    enableModuleShortcutAdd: Boolean,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onUninstall: (APModuleViewModel.ModuleInfo) -> Unit,
    onUndoUninstall: (APModuleViewModel.ModuleInfo) -> Unit,
    onCheckChanged: (Boolean) -> Unit,
    onUpdate: (APModuleViewModel.ModuleInfo) -> Unit,
    onClick: (APModuleViewModel.ModuleInfo) -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    val context = LocalContext.current
    val viewModel = viewModel<APModuleViewModel>()
    val snackBarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()
    val shortcutAdd = stringResource(id = R.string.module_shortcut_add)
    val folkBannerTitle = stringResource(R.string.apm_folk_banner_title)
    val folkBannerSelect = stringResource(R.string.apm_folk_banner_select)
    val folkBannerClear = stringResource(R.string.apm_folk_banner_clear)
    val folkBannerSaved = stringResource(R.string.apm_folk_banner_saved)
    val folkBannerCleared = stringResource(R.string.apm_folk_banner_cleared)
    val folkBannerFailed = stringResource(R.string.apm_folk_banner_failed)
    
    var showFolkBannerDialog by remember { mutableStateOf(false) }
    var hasFolkBanner by remember { mutableStateOf(false) }
    var bannerReloadKey by rememberSaveable(module.id) { mutableStateOf(0) }
    val customInfoReloadKeyState = remember { mutableStateOf(0) }
    var customInfoReloadKey by customInfoReloadKeyState
    
    LaunchedEffect(showFolkBannerDialog) {
        if (showFolkBannerDialog) {
            hasFolkBanner = withContext(Dispatchers.IO) {
                apmBannerStorage.read(module.id) != null
            }
        }
    }
    
    val pickFolkBannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                loadingDialog.show()
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val data = apmBannerStorage.write(context, module.id, it)
                        if (data != null) {
                            runCatching {
                                val rootShell = getRootShell(true)
                                val resolvedDir = resolveModuleDir(rootShell, module.id)
                                clearLegacyFolkBanner(rootShell, resolvedDir)
                            }
                        }
                        data
                    }.getOrNull()
                }
                loadingDialog.hide()
                if (result != null) {
                    viewModel.putBannerInfo(module.id, APModuleViewModel.BannerInfo(result, null))
                    bannerReloadKey++
                    snackBarHost.showSnackbar(folkBannerSaved.format(module.name))
                } else {
                    snackBarHost.showSnackbar(folkBannerFailed.format(module.name))
                }
            }
        }
    }

    val isWallpaperMode = BackgroundConfig.isCustomBackgroundEnabled
    val opacity = if (isWallpaperMode) {
        BackgroundConfig.customBackgroundOpacity.coerceAtLeast(0.35f)
    } else {
        1f
    }

    val bannerImageAlpha = if (BackgroundConfig.isBannerCustomOpacityEnabled) {
        BackgroundConfig.bannerCustomOpacity
    } else {
        if (isWallpaperMode) {
            (0.35f + (opacity - 0.2f) * 0.5f).coerceIn(0.25f, 0.6f)
        } else {
            0.18f
        }
    }
    
    var showShortcutDialog by remember { mutableStateOf(false) }
    var shortcutName by rememberSaveable(module.id) { mutableStateOf(module.name) }
    var shortcutIconUri by remember { mutableStateOf<String?>(null) }
    var shortcutType by rememberSaveable(module.id) { mutableStateOf(if (module.hasWebUi) "webui" else "action") }
    val appIcon = remember(context) { context.packageManager.getApplicationIcon(context.packageName) }
    val pickShortcutIconLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        shortcutIconUri = uri?.toString()
    }

    fun toSuIconUri(path: String?): String? {
        val trimmed = path?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        return if (trimmed.startsWith("su://", true)) trimmed else "su://$trimmed"
    }

    val moduleDefaultIconPath = remember(
        module.id,
        shortcutType,
        module.webuiIcon,
        module.actionIcon
    ) {
        val preferred = if (shortcutType == "webui") module.webuiIcon else module.actionIcon
        preferred?.takeIf { it.isNotBlank() }
            ?: module.webuiIcon?.takeIf { it.isNotBlank() }
            ?: module.actionIcon?.takeIf { it.isNotBlank() }
    }
    val moduleDefaultIconUri = remember(moduleDefaultIconPath) { toSuIconUri(moduleDefaultIconPath) }
    val effectiveShortcutIconUri = shortcutIconUri ?: moduleDefaultIconUri

    val shortcutPreviewBitmap by produceState<Bitmap?>(initialValue = null, key1 = if (showShortcutDialog) effectiveShortcutIconUri else null) {
        if (!showShortcutDialog || effectiveShortcutIconUri.isNullOrBlank()) {
            value = null
        } else {
            value = withContext(Dispatchers.IO) {
                ModuleShortcut.loadShortcutBitmap(context, effectiveShortcutIconUri)
            }
        }
    }
    
    val customInfo by produceState(initialValue = null as CustomModuleInfo?, key1 = module.id, key2 = customInfoReloadKey) {
        value = withContext(Dispatchers.IO) {
            apmCustomModuleInfoStorage.read(module.id)
        }
    }

    val sizeStr = if (showMoreModuleInfo) viewModel.getModuleSize(module.id) else "0 KB"

    val bannerInfo by produceState<APModuleViewModel.BannerInfo?>(
        initialValue = viewModel.getBannerInfo(module.id),
        module.id,
        BackgroundConfig.isBannerEnabled,
        BackgroundConfig.isFolkBannerEnabled,
        BackgroundConfig.isBannerApiModeEnabled,
        BackgroundConfig.bannerApiSource,
        bannerReloadKey
    ) {
        if (!BackgroundConfig.isBannerEnabled) {
            value = null
            return@produceState
        }

        viewModel.bannerSemaphore.withPermit {
            val effectiveApiSource = BackgroundConfig.getEffectiveBannerApiSource()

        if (BackgroundConfig.isBannerApiModeEnabled && effectiveApiSource.isNotBlank()) {
            val apiBanner = withContext(Dispatchers.IO) {
                BannerApiService.getModuleBanner(
                    context = context,
                    moduleId = module.id,
                    source = effectiveApiSource
                )
            }
            if (apiBanner != null) {
                viewModel.putBannerInfo(module.id, APModuleViewModel.BannerInfo(apiBanner, null))
                value = APModuleViewModel.BannerInfo(apiBanner, null)
                return@produceState
            }
        }

        val cached = viewModel.getBannerInfo(module.id)
        if (cached != null && (cached.bytes != null || cached.url != null)) {
            value = cached
            return@produceState
        }

        val loaded = withContext(Dispatchers.IO) {
            try {
                val folkBanner = if (BackgroundConfig.isFolkBannerEnabled) apmBannerStorage.read(module.id) else null
                if (folkBanner != null) {
                    return@withContext APModuleViewModel.BannerInfo(folkBanner, null)
                }
                val rootShell = getRootShell(true)
                val suFile = { path: String ->
                    SuFile(path).apply { shell = rootShell }
                }
                val resolvedDir = resolveModuleDir(rootShell, module.id)
                val propBanner = readModulePropBanner(rootShell, resolvedDir)

                if (!propBanner.isNullOrEmpty() && propBanner.startsWith("http", true)) {
                    return@withContext APModuleViewModel.BannerInfo(null, propBanner)
                }

                val candidates = buildList {
                    if (!propBanner.isNullOrEmpty()) {
                        add(propBanner)
                    }
                    addAll(listOf("banner", "banner.png", "banner.jpg", "banner.jpeg", "banner.webp"))
                }.distinct()

                for (name in candidates) {
                    val file = if (name.startsWith("/")) {
                        suFile(name)
                    } else {
                        suFile("$resolvedDir/$name")
                    }
                    if (file.exists()) {
                        return@withContext APModuleViewModel.BannerInfo(file.newInputStream().use { it.readBytes() }, null)
                    }
                }
                null
            } catch (e: Exception) {
                null
            }
        }

        if (loaded != null) {
            viewModel.putBannerInfo(module.id, loaded)
            value = loaded
        } else if (cached != null) {
            value = cached
        } else {
            viewModel.putBannerInfo(module.id, APModuleViewModel.BannerInfo(null, null))
            value = APModuleViewModel.BannerInfo(null, null)
        }
        }
    }

    val insideSplicedGroup = LocalInsideSplicedGroup.current

    val cardColor = if (isWallpaperMode) {
        MaterialTheme.colorScheme.surface.copy(alpha = opacity)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
    }

    val cardShape = RoundedCornerShape(20.dp)

    val cardInteractionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current

    val clickModifier = Modifier
        .fillMaxWidth()
        .animateContentSize()
        .folkPressScale(cardInteractionSource)
        .combinedClickable(
            interactionSource = cardInteractionSource,
            indication = null,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (foldSystemModule) {
                    onExpandToggle()
                } else {
                    onClick(module)
                }
            },
            onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showFolkBannerDialog = true
            }
        )

    val contentBlock: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxWidth()) {
            val bannerUrl = bannerInfo?.url
            val bannerData = bannerInfo?.bytes
            val hasBannerUrl = !bannerUrl.isNullOrEmpty()
            if (bannerData != null || hasBannerUrl) {
                val fadeColor = bannerFadeColor()

                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = if (hasBannerUrl) {
                            bannerUrl
                        } else {
                            ImageRequest.Builder(context)
                                .data(bannerData)
                                .build()
                         },
                         contentDescription = null,
                         modifier = Modifier.fillMaxSize(),
                         contentScale = ContentScale.Crop,
                         alpha = bannerImageAlpha
                     )
                    val gradientAlpha = if (isWallpaperMode) 0.5f else 0.8f
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        fadeColor.copy(alpha = 0.0f),
                                        fadeColor.copy(alpha = gradientAlpha)
                                    ),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                )
                            )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val hasAnyLabel = showMoreModuleInfo || module.remove || (updateUrl.isNotEmpty() && !module.update) || module.update
                        if (hasAnyLabel) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                val labelOpacity = (opacity + 0.1f).coerceAtMost(1f)
                                if (showMoreModuleInfo) {
                                    ModuleLabel(
                                        text = sizeStr,
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = labelOpacity),
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    ModuleLabel(
                                        text = module.id,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = labelOpacity),
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                if (module.remove) {
                                    ModuleLabel(
                                        text = stringResource(R.string.apm_remove),
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = labelOpacity),
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                } else if (updateUrl.isNotEmpty() && !module.update) {
                                    ModuleLabel(
                                        text = stringResource(R.string.apm_update),
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = labelOpacity),
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                } else if (module.update) {
                                    ModuleLabel(
                                        text = "Updated",
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = labelOpacity),
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                                
                                if (showMoreModuleInfo && module.hasWebUi && module.enabled && !module.remove) {
                                    ModuleLabel(
                                        text = "WebUI",
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = labelOpacity),
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                if (showMoreModuleInfo && module.hasActionScript && module.enabled && !module.remove) {
                                    ModuleLabel(
                                        text = "Action",
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = labelOpacity),
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                
                                if (module.isMetamodule && !module.remove) {
                                    ModuleLabel(
                                        text = "META",
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = labelOpacity),
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        Text(
                            text = customInfo?.name?.takeIf { it.isNotBlank() } ?: module.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textDecoration = if (module.remove) TextDecoration.LineThrough else TextDecoration.None
                        )

                        Text(
                            text = customInfo?.version?.takeIf { it.isNotBlank() } ?: module.version,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = if (module.remove) TextDecoration.LineThrough else TextDecoration.None
                        )

                        Text(
                            text = customInfo?.author?.takeIf { it.isNotBlank() } ?: module.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = if (module.remove) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }

                    ExpressiveSwitch(
                        enabled = !module.update,
                        checked = isChecked,
                        onCheckedChange = onCheckChanged
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = customInfo?.description?.takeIf { it.isNotBlank() } ?: module.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = !foldSystemModule || expanded,
                    enter = fadeIn() + expandVertically(),
                    exit = shrinkVertically() + fadeOut()
                ) {
           
                    val buttons = mutableListOf<ModuleButtonConfig>()
                    
                    if (module.hasWebUi && module.enabled && !module.remove) {
                        buttons.add(ModuleButtonConfig(
                            icon = Icons.AutoMirrored.Outlined.Wysiwyg,
                            text = stringResource(R.string.apm_webui_open),
                            contentDescription = stringResource(R.string.apm_webui_open),
                            onClick = { onClick(module) }
                        ))
                    }
                    
                    if (module.hasActionScript && module.enabled && !module.remove) {
                        buttons.add(ModuleButtonConfig(
                            icon = Icons.Outlined.Terminal,
                            text = stringResource(R.string.apm_action),
                            contentDescription = stringResource(R.string.apm_action),
                            onClick = {
                                navigator.navigate(ExecuteAPMActionScreenDestination(module.id))
                                viewModel.markNeedRefresh()
                            }
                        ))
                    }

                    val hasUpdateButton = updateUrl.isNotEmpty() && !module.remove && !module.update
                    
                    if (enableModuleShortcutAdd && module.enabled && !module.remove && (module.hasWebUi || module.hasActionScript) && !hasUpdateButton) {
                        buttons.add(ModuleButtonConfig(
                            icon = Icons.Outlined.Add,
                            text = shortcutAdd,
                            contentDescription = shortcutAdd,
                            onClick = { 
                                shortcutName = module.name
                                shortcutIconUri = null
                                shortcutType = if (module.hasWebUi) "webui" else "action"
                                showShortcutDialog = true 
                            }
                        ))
                    }
                    
                    if (hasUpdateButton) {
                        buttons.add(ModuleButtonConfig(
                            icon = Icons.Outlined.Download,
                            text = stringResource(R.string.apm_update),
                            contentDescription = stringResource(R.string.apm_update),
                            onClick = { onUpdate(module) }
                        ))
                    }
                    

                    val deleteButton = ModuleButtonConfig(
                        icon = if (module.remove) Icons.Outlined.Restore else Icons.Outlined.Delete,
                        text = if (module.remove) stringResource(R.string.apm_undo) else stringResource(R.string.apm_remove),
                        contentDescription = if (module.remove) stringResource(R.string.apm_undo) else stringResource(R.string.apm_remove),
                        onClick = {
                            if (module.remove) {
                                onUndoUninstall(module)
                            } else {
                                onUninstall(module)
                            }
                        },
                        colors = if (simpleListBottomBar) ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = (opacity + 0.3f).coerceAtMost(1f))
                        ) else if (module.remove) ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = (opacity + 0.3f).coerceAtMost(1f)),
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ) else ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = (opacity + 0.3f).coerceAtMost(1f)),
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                    
                    AdaptiveModuleButtonRow(
                        buttons = buttons,
                        trailingButton = deleteButton,
                        simpleListBottomBar = simpleListBottomBar,
                        spacing = if (simpleListBottomBar) 12 else 8,
                        opacity = opacity
                    )
                }
            }
        }
    }

    // Render: inside spliced group → no Surface wrapper; standalone → Surface card
    if (insideSplicedGroup) {
        Box(modifier = modifier.then(clickModifier)) {
            contentBlock()
        }
    } else {
        Surface(
            modifier = modifier
                .clip(cardShape)
                .then(clickModifier),
            shape = cardShape,
            color = cardColor,
            tonalElevation = 0.dp
        ) {
            contentBlock()
        }
    }

    if (showShortcutDialog) {
        AlertDialog(
            onDismissRequest = { showShortcutDialog = false },
            title = { Text(stringResource(R.string.module_shortcut_add)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = shortcutName,
                        onValueChange = { shortcutName = it },
                        label = { Text(stringResource(R.string.module_shortcut_name)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.module_shortcut_icon))
                        Spacer(Modifier.width(12.dp))
                        if (shortcutPreviewBitmap != null) {
                            Image(
                                bitmap = shortcutPreviewBitmap!!.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                        } else if (shortcutIconUri != null) {
                            AsyncImage(
                                model = shortcutIconUri,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                        } else {
                            AsyncImage(
                                model = appIcon,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { pickShortcutIconLauncher.launch("image/*") }) {
                            Text(stringResource(R.string.module_shortcut_icon_select))
                        }
                        TextButton(onClick = { shortcutIconUri = null }) {
                            Text(stringResource(R.string.module_shortcut_icon_default))
                        }
                    }
                    if (module.hasWebUi && module.hasActionScript) {
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.module_shortcut_type))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = shortcutType == "webui",
                                onClick = { shortcutType = "webui" }
                            )
                            Text(stringResource(R.string.module_shortcut_type_webui))
                            Spacer(Modifier.width(24.dp))
                            RadioButton(
                                selected = shortcutType == "action",
                                onClick = { shortcutType = "action" }
                            )
                            Text(stringResource(R.string.module_shortcut_type_action))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (shortcutType == "webui" && module.hasWebUi) {
                        ModuleShortcut.createModuleWebUiShortcut(context, module.id, shortcutName.ifEmpty { module.name }, effectiveShortcutIconUri)
                    } else if (module.hasActionScript) {
                        ModuleShortcut.createModuleActionShortcut(context, module.id, shortcutName.ifEmpty { module.name }, effectiveShortcutIconUri)
                    }
                    showShortcutDialog = false
                }) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showShortcutDialog = false }) {
                    Text(text = stringResource(id = android.R.string.cancel))
                }
            }
        )
    }

    // 自定义模块信息状态
    var customName by remember { mutableStateOf("") }
    var customVersion by remember { mutableStateOf("") }
    var customAuthor by remember { mutableStateOf("") }
    var customDescription by remember { mutableStateOf("") }

    // 弹窗打开时加载自定义信息
    LaunchedEffect(showFolkBannerDialog) {
        if (showFolkBannerDialog) {
            val info = withContext(Dispatchers.IO) {
                apmCustomModuleInfoStorage.read(module.id)
            }
            customName = info?.name?.takeIf { it.isNotBlank() } ?: module.name
            customVersion = info?.version?.takeIf { it.isNotBlank() } ?: module.version
            customAuthor = info?.author?.takeIf { it.isNotBlank() } ?: module.author
            customDescription = info?.description?.takeIf { it.isNotBlank() } ?: module.description
        }
    }

    val customInfoTitle = stringResource(R.string.folk_banner_custom_info_title)
    val customInfoNameLabel = stringResource(R.string.folk_banner_custom_info_name)
    val customInfoVersionLabel = stringResource(R.string.folk_banner_custom_info_version)
    val customInfoAuthorLabel = stringResource(R.string.folk_banner_custom_info_author)
    val customInfoDescriptionLabel = stringResource(R.string.folk_banner_custom_info_description)
    val customInfoSaveLabel = stringResource(R.string.folk_banner_custom_info_save)
    val customInfoResetLabel = stringResource(R.string.folk_banner_custom_info_reset)
    val customInfoSavedMsg = stringResource(R.string.folk_banner_custom_info_saved)
    val customInfoResetMsg = stringResource(R.string.folk_banner_custom_info_reset_done)

    BackgroundOptionsDialog(
        showDialog = showFolkBannerDialog,
        onDismiss = { showFolkBannerDialog = false },
        title = folkBannerTitle,
        showBannerSection = BackgroundConfig.isBannerEnabled && BackgroundConfig.isFolkBannerEnabled,
        selectLabel = folkBannerSelect,
        clearLabel = folkBannerClear,
        hasExisting = hasFolkBanner,
        onSelectImage = {
            pickFolkBannerLauncher.launch("image/*")
        },
        onClearImage = {
            scope.launch {
                loadingDialog.show()
                val success = withContext(Dispatchers.IO) {
                    runCatching {
                        val localCleared = apmBannerStorage.clear(module.id)
                        val legacyCleared = runCatching {
                            val rootShell = getRootShell(true)
                            val resolvedDir = resolveModuleDir(rootShell, module.id)
                            clearLegacyFolkBanner(rootShell, resolvedDir)
                        }.getOrDefault(false)
                        localCleared || legacyCleared
                    }.getOrDefault(false)
                }
                loadingDialog.hide()
                if (success) {
                    viewModel.removeBannerInfo(module.id)
                    bannerReloadKey++
                    snackBarHost.showSnackbar(folkBannerCleared.format(module.name))
                } else {
                    snackBarHost.showSnackbar(folkBannerFailed.format(module.name))
                }
            }
        },
        customInfoTitle = customInfoTitle,
        customInfoNameLabel = customInfoNameLabel,
        customInfoVersionLabel = customInfoVersionLabel,
        customInfoAuthorLabel = customInfoAuthorLabel,
        customInfoDescriptionLabel = customInfoDescriptionLabel,
        saveLabel = customInfoSaveLabel,
        resetLabel = customInfoResetLabel,
        initialModuleInfo = ModuleInfoData(
            name = customName,
            version = customVersion,
            author = customAuthor,
            description = customDescription
        ),
        hasSavedCustomInfo = customInfo?.hasAnyInfo() == true,
        customInfoReloadKey = customInfoReloadKeyState,
        onSaveModuleInfo = { info ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    apmCustomModuleInfoStorage.write(module.id, CustomModuleInfo(
                        name = info.name.takeIf { it.isNotBlank() },
                        version = info.version.takeIf { it.isNotBlank() },
                        author = info.author.takeIf { it.isNotBlank() },
                        description = info.description.takeIf { it.isNotBlank() },
                    ))
                }
                snackBarHost.showSnackbar(
                    customInfoSavedMsg.format(module.name)
                )
            }
        },
        onResetModuleInfo = {
            scope.launch {
                withContext(Dispatchers.IO) {
                    apmCustomModuleInfoStorage.clear(module.id)
                }
                snackBarHost.showSnackbar(
                    customInfoResetMsg.format(module.name)
                )
            }
        }
    )
}
