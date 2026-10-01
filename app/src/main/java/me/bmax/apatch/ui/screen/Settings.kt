package me.bmax.apatch.ui.screen

import android.content.Intent
import android.os.Build
import android.system.Os
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.dropUnlessResumed
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AboutScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AppearanceSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.BackupSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.BehaviorSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FunctionSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.GeneralSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ModuleSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.MultimediaSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SecuritySettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsSearchScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.FolkNavigationPreference
import me.bmax.apatch.ui.component.folk.FolkSettingsGroup
import me.bmax.apatch.ui.component.folk.folkGroupColor
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.screen.settings.general.CleanStorageDialog
import me.bmax.apatch.util.BiometricUtils
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.getBugreportFile
import me.bmax.apatch.util.ui.NavigationBarsSpacer

private const val FEEDBACK_URL = "https://github.com/LyraVoid/FolkPatch/issues/new/choose"

private data class SecondaryEntry(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

@Destination<RootGraph>
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingScreen(navigator: DestinationsNavigator) {
    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val kPatchReady = state != APApplication.State.UNKNOWN_STATE
    val aPatchReady =
        (state == APApplication.State.ANDROIDPATCH_INSTALLING || state == APApplication.State.ANDROIDPATCH_INSTALLED || state == APApplication.State.ANDROIDPATCH_NEED_UPDATE)

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()
    val canAuthenticate = remember { BiometricUtils.isBiometricAvailable(context) }

    var showDevDialog by rememberSaveable { mutableStateOf(false) }
    DeveloperInfo(
        showDialog = showDevDialog
    ) {
        showDevDialog = false
    }

    val cleanStorageDialogState = remember { mutableStateOf(false) }

    // Device / patch identity shown in the header.
    val uname = remember { Os.uname() }
    val kernelShort = remember(uname.release) {
        Regex("^[0-9]+(\\.[0-9]+)*").find(uname.release)?.value ?: uname.release
    }
    val kpVersion = remember(state) {
        if (kPatchReady) runCatching { Version.installedKPVString() }.getOrDefault("") else ""
    }
    val kernelChip = if (kPatchReady && kpVersion.isNotBlank()) {
        "${stringResource(R.string.kernel_patch)} $kpVersion"
    } else {
        stringResource(R.string.home_install_unknown)
    }
    val systemChip = stringResource(R.string.android_patch) + " " + if (aPatchReady) {
        stringResource(R.string.kpm_installed)
    } else {
        stringResource(R.string.home_not_installed)
    }

    // The icon grid holds our secondary entries - the settings categories. The
    // bottom bar already covers Home / KPModule / SuperUser / APModule / Settings,
    // so nothing here duplicates it.
    val secondaryEntries = buildList {
        add(
            SecondaryEntry(
                icon = Icons.Outlined.Settings,
                label = stringResource(R.string.settings_category_general),
                onClick = { navigator.navigate(GeneralSettingsScreenDestination(null)) },
            )
        )
        add(
            SecondaryEntry(
                icon = Icons.Outlined.Palette,
                label = stringResource(R.string.settings_category_appearance),
                onClick = { navigator.navigate(AppearanceSettingsScreenDestination(null)) },
            )
        )
        add(
            SecondaryEntry(
                icon = Icons.Outlined.Visibility,
                label = stringResource(R.string.settings_category_behavior),
                onClick = { navigator.navigate(BehaviorSettingsScreenDestination(null)) },
            )
        )
        add(
            SecondaryEntry(
                icon = Icons.Outlined.Tune,
                label = stringResource(R.string.settings_category_function),
                onClick = { navigator.navigate(FunctionSettingsScreenDestination(null)) },
            )
        )
        if (canAuthenticate) {
            add(
                SecondaryEntry(
                    icon = Icons.Outlined.Security,
                    label = stringResource(R.string.settings_category_security),
                    onClick = { navigator.navigate(SecuritySettingsScreenDestination(null)) },
                )
            )
        }
        if (aPatchReady) {
            add(
                SecondaryEntry(
                    icon = Icons.Outlined.Cloud,
                    label = stringResource(R.string.settings_category_backup),
                    onClick = { navigator.navigate(BackupSettingsScreenDestination(null)) },
                )
            )
            add(
                SecondaryEntry(
                    icon = Icons.Outlined.Extension,
                    label = stringResource(R.string.settings_category_module),
                    onClick = { navigator.navigate(ModuleSettingsScreenDestination(null)) },
                )
            )
        }
        add(
            SecondaryEntry(
                icon = Icons.Outlined.MusicNote,
                label = stringResource(R.string.settings_category_multimedia),
                onClick = { navigator.navigate(MultimediaSettingsScreenDestination(null)) },
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
                actions = {
                    IconButton(onClick = dropUnlessResumed { navigator.navigate(SettingsSearchScreenDestination) }) {
                        Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.settings_search_title))
                    }
                    IconButton(onClick = { showDevDialog = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.about))
                    }
                }
            )
        },
        containerColor = Color.Transparent,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item(key = "identity_header") {
                SettingsIdentityHeader(
                    deviceName = getDeviceInfo().trim(),
                    kernelChip = kernelChip,
                    systemChip = systemChip,
                    systemActive = aPatchReady,
                    summary = "${stringResource(R.string.home_kernel)} $kernelShort",
                )
            }

            item(key = "secondary_entries") {
                Spacer(Modifier.height(14.dp))
                SettingsIconGrid(entries = secondaryEntries)
            }

            item(key = "utility_rows") {
                Spacer(Modifier.height(17.dp))
                FolkSettingsGroup(shape = RoundedCornerShape(8.dp)) {
                    item(key = "utility_send_log") {
                        FolkNavigationPreference(
                            icon = Icons.Outlined.Description,
                            title = stringResource(R.string.send_log),
                            onClick = {
                                scope.launch {
                                    val bugreport = loadingDialog.withLoading { getBugreportFile(context) }
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${BuildConfig.APPLICATION_ID}.fileprovider",
                                        bugreport,
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        type = "application/gzip"
                                        clipData = android.content.ClipData.newRawUri(null, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, context.getString(R.string.send_log))
                                    )
                                }
                            },
                        )
                    }
                    item(key = "utility_feedback") {
                        FolkNavigationPreference(
                            icon = Icons.Outlined.BugReport,
                            title = stringResource(R.string.home_more_menu_feedback_or_suggestion),
                            onClick = { uriHandler.openUri(FEEDBACK_URL) },
                        )
                    }
                    item(key = "utility_clean_storage") {
                        FolkNavigationPreference(
                            icon = Icons.Outlined.CleaningServices,
                            title = stringResource(R.string.settings_clean_storage),
                            onClick = { cleanStorageDialogState.value = true },
                        )
                    }
                    item(key = "utility_about") {
                        FolkNavigationPreference(
                            icon = Icons.Outlined.Info,
                            title = stringResource(R.string.about),
                            onClick = { navigator.navigate(AboutScreenDestination) },
                        )
                    }
                }
            }

            item(key = "settings_bottom_spacer") {
                Spacer(Modifier.height(16.dp))
                NavigationBarsSpacer()
            }
        }
    }

    if (cleanStorageDialogState.value) {
        CleanStorageDialog(cleanStorageDialogState)
    }
}

/**
 * Device / patch identity header - the FolkPatch equivalent of the reference
 * app's profile row. There is no account, so the "identity" is the device and
 * its patch state.
 */
@Composable
private fun SettingsIdentityHeader(
    deviceName: String,
    kernelChip: String,
    systemChip: String,
    systemActive: Boolean,
    summary: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = deviceName,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StateChip(text = kernelChip, accent = false)
                StateChip(text = systemChip, accent = systemActive)
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StateChip(text: String, accent: Boolean) {
    val container = if (accent) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else {
        folkGroupColor().copy(alpha = 1f)
    }
    val content = if (accent) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = container,
        contentColor = content,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/**
 * Four-column icon grid, mirroring the reference's shortcut block. Cells are
 * sized so an icon + label pair sits comfortably with generous vertical air.
 */
@Composable
private fun SettingsIconGrid(entries: List<SecondaryEntry>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(8.dp),
        color = folkGroupColor(),
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            entries.chunked(4).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEach { entry ->
                        GridEntry(
                            entry = entry,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(4 - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GridEntry(
    entry: SecondaryEntry,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = entry.onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = entry.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = entry.label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperInfo(
    showDialog: Boolean,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val githubUrl = "https://github.com/LyraVoid/FolkPatch"
    val telegramUrl = "https://t.me/FolkPatch"
    val sociabuzzUrl = "https://ifdian.net/a/matsuzaka_yuki"

    if (showDialog) {
        ModalBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            onDismissRequest = onDismissRequest
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data("http://q.qlogo.cn/headimg_dl?dst_uin=3231515355&spec=640&img_type=jpg")
                            .crossfade(true)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .build(),
                        contentDescription = "Developer Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Matsuzaka Yuki",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.developer_and_maintainer),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "\"美しい世界を見てきましょう\"",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FilledTonalButton(
                        onClick = { uriHandler.openUri(githubUrl) },
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.github),
                            contentDescription = stringResource(R.string.github),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.github))
                    }

                    FilledTonalButton(
                        onClick = { uriHandler.openUri(telegramUrl) },
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.telegram),
                            contentDescription = stringResource(R.string.telegram),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.telegram))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                FilledTonalButton(
                    onClick = { uriHandler.openUri(sociabuzzUrl) },
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Coffee,
                        contentDescription = stringResource(R.string.support_or_donate),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.support_or_donate))
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
