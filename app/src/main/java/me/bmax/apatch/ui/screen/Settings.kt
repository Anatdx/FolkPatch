package me.bmax.apatch.ui.screen

import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.FolkNavigationPreference
import me.bmax.apatch.ui.component.folk.FolkSettingsGroup
import me.bmax.apatch.ui.component.folk.folkGroupColor
import me.bmax.apatch.ui.component.folk.folkPressScale
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.component.rememberSystemCropLauncher
import me.bmax.apatch.ui.screen.settings.general.CleanStorageDialog
import me.bmax.apatch.util.ui.showToast
import me.bmax.apatch.util.BiometricUtils
import me.bmax.apatch.util.SafeUriResolver
import me.bmax.apatch.util.getBugreportFile
import me.bmax.apatch.util.ui.NavigationBarsSpacer
import java.io.File
import java.io.FileOutputStream

private const val FEEDBACK_URL = "https://github.com/LyraVoid/FolkPatch/issues/new/choose"

private const val PROFILE_AVATAR_FILE = "profile_avatar"

/** Square size the avatar is decoded at, in pixels. */
private const val PROFILE_AVATAR_PX = 256

/**
 * Copies the picked image into app storage and returns a cache-busted URI.
 *
 * The square crop itself is not re-implemented here: [me.bmax.apatch.util.BottomBarIconConfig]
 * already centre-crops custom nav icons, and the avatar reuses that.
 */
private suspend fun persistProfileAvatar(context: android.content.Context, uri: Uri): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val target = File(context.filesDir, PROFILE_AVATAR_FILE)
            SafeUriResolver.openInputStream(context, uri).use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            }
            Uri.fromFile(target).buildUpon()
                .appendQueryParameter("t", System.currentTimeMillis().toString())
                .build()
                .toString()
        }.getOrNull()
    }

private data class SecondaryEntry(
    val icon: ImageVector,
    val label: String,
    val iconSize: Dp = 24.dp,
    val onClick: () -> Unit,
)

@Destination<RootGraph>
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingScreen(navigator: DestinationsNavigator) {
    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val aPatchReady =
        (state == APApplication.State.ANDROIDPATCH_INSTALLING || state == APApplication.State.ANDROIDPATCH_INSTALLED || state == APApplication.State.ANDROIDPATCH_NEED_UPDATE)

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()
    val canAuthenticate = remember { BiometricUtils.isBiometricAvailable(context) }

    // Local-only personalisation. Nothing here needs an account: the avatar,
    // nickname and signature are stored in shared preferences and the picked
    // image is copied into app storage.
    val prefs = APApplication.sharedPreferences
    var profileNickname by remember { mutableStateOf(prefs.getString("profile_nickname", "").orEmpty()) }
    var profileSignature by remember { mutableStateOf(prefs.getString("profile_signature", "").orEmpty()) }
    var profileAvatar by remember { mutableStateOf(prefs.getString("profile_avatar", "").orEmpty()) }
    var showProfileEditor by rememberSaveable { mutableStateOf(false) }
    var pendingAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var showCropChoice by remember { mutableStateOf(false) }

    fun applyAvatar(uri: Uri) {
        scope.launch {
            persistProfileAvatar(context, uri)?.let { stored ->
                profileAvatar = stored
                prefs.edit { putString("profile_avatar", stored) }
            }
        }
    }

    // Same platform crop flow the appearance settings use, locked to a square.
    val avatarCropLauncher = rememberSystemCropLauncher(
        cacheName = "profile_avatar_crop_cache",
        aspectX = 1,
        aspectY = 1,
        outputSize = 512,
    ) { uri -> applyAvatar(uri) }

    val pickAvatarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingAvatarUri = uri
            showCropChoice = true
        }
    }

    var showDevDialog by rememberSaveable { mutableStateOf(false) }
    DeveloperInfo(
        showDialog = showDevDialog
    ) {
        showDevDialog = false
    }

    val cleanStorageDialogState = remember { mutableStateOf(false) }

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
                // The music note's glyph is optically narrower than the others,
                // so it needs a touch more size to carry the same weight.
                iconSize = 26.dp,
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
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            lineHeight = 28.sp,
                        ),
                        fontWeight = FontWeight.Medium,
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
                ProfileHeader(
                    nickname = profileNickname.ifBlank { "FolkPatch" },
                    signature = profileSignature,
                    deviceName = getDeviceInfo().trim(),
                    avatarUri = profileAvatar,
                    onAvatarClick = { showProfileEditor = true },
                )
            }

            item(key = "secondary_entries") {
                Spacer(Modifier.height(20.dp))
                SettingsIconGrid(entries = secondaryEntries)
            }

            item(key = "utility_rows") {
                Spacer(Modifier.height(16.dp))
                FolkSettingsGroup(shape = RoundedCornerShape(12.dp)) {
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

    val pendingCrop = pendingAvatarUri
    if (showCropChoice && pendingCrop != null) {
        AlertDialog(
            onDismissRequest = {
                showCropChoice = false
                pendingAvatarUri = null
            },
            title = { Text(stringResource(R.string.profile_crop_avatar)) },
            text = { Text(stringResource(R.string.settings_crop_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showCropChoice = false
                    pendingAvatarUri = null
                    try {
                        avatarCropLauncher.launch(pendingCrop)
                    } catch (e: ActivityNotFoundException) {
                        showToast(context, context.getString(R.string.settings_crop_not_supported))
                        applyAvatar(pendingCrop)
                    }
                }) {
                    Text(stringResource(R.string.settings_crop_dialog_crop))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCropChoice = false
                    pendingAvatarUri = null
                    applyAvatar(pendingCrop)
                }) {
                    Text(stringResource(R.string.settings_crop_dialog_direct))
                }
            },
        )
    }

    if (showProfileEditor) {
        ProfileEditSheet(
            nickname = profileNickname.ifBlank { "FolkPatch" },
            signature = profileSignature,
            avatarUri = profileAvatar,
            onPickAvatar = { pickAvatarLauncher.launch("image/*") },
            onRestoreDefault = {
                profileNickname = ""
                profileSignature = ""
                profileAvatar = ""
                runCatching { File(context.filesDir, PROFILE_AVATAR_FILE).delete() }
                prefs.edit {
                    remove("profile_nickname")
                    remove("profile_signature")
                    remove("profile_avatar")
                }
            },
            onDismiss = { showProfileEditor = false },
            onSave = { name, sign ->
                profileNickname = name
                profileSignature = sign
                prefs.edit {
                    if (name.isBlank()) remove("profile_nickname") else putString("profile_nickname", name)
                    if (sign.isBlank()) remove("profile_signature") else putString("profile_signature", sign)
                }
                showProfileEditor = false
            },
        )
    }
}

/**
 * Local profile editor: pick an avatar, set a nickname and a short signature,
 * or fall back to the defaults. No account, no network.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditSheet(
    nickname: String,
    signature: String,
    avatarUri: String,
    onPickAvatar: () -> Unit,
    onRestoreDefault: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf(nickname) }
    var sign by remember { mutableStateOf(signature) }
    val context = LocalContext.current
    val avatarBitmap = remember(avatarUri) {
        if (avatarUri.isBlank()) {
            null
        } else {
            runCatching {
                me.bmax.apatch.util.BottomBarIconConfig
                    .loadIconBitmap(context, avatarUri, PROFILE_AVATAR_PX)
            }.getOrNull()?.asImageBitmap()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = stringResource(R.string.profile_edit_title),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, lineHeight = 24.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(82.dp),
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                FilledTonalButton(
                    onClick = onPickAvatar,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(stringResource(R.string.profile_choose_avatar))
                }
            }

            Spacer(Modifier.height(18.dp))

            ProfileTextField(
                value = name,
                onValueChange = { if (it.length <= 24) name = it },
                label = stringResource(R.string.profile_nickname_label),
                singleLine = true,
            )

            Spacer(Modifier.height(12.dp))

            ProfileTextField(
                value = sign,
                onValueChange = { if (it.length <= 60) sign = it },
                label = stringResource(R.string.profile_signature_label),
                singleLine = false,
                minLines = 2,
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onRestoreDefault) {
                    Text(stringResource(R.string.profile_restore_default))
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onSave(name.trim(), sign.trim()) },
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

/**
 * Rounded, filled text field without the Material underline, so the editor
 * reads as part of FolkPatch rather than a stock Material form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean,
    minLines: Int = 1,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
/**
 * Personal space header.
 *
 * The top of the page leads with a person-like block (avatar + nickname +
 * optional signature) and only then shows the device facts, so the technical
 * text no longer occupies the most expressive spot on the page.
 *
 * The nickname and signature read from local preferences; with no signature
 * set the second line simply does not exist. The avatar falls back to the app
 * mark until the user picks an image.
 */
@Composable
private fun ProfileHeader(
    nickname: String,
    signature: String,
    deviceName: String,
    avatarUri: String,
    onAvatarClick: () -> Unit,
) {
    val context = LocalContext.current
    val avatarBitmap = remember(avatarUri) {
        if (avatarUri.isBlank()) {
            null
        } else {
            runCatching {
                me.bmax.apatch.util.BottomBarIconConfig
                    .loadIconBitmap(context, avatarUri, PROFILE_AVATAR_PX)
            }.getOrNull()?.asImageBitmap()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    // Plain circle, no shadow and no coloured ring.
                    .clip(CircleShape)
                    .background(folkGroupColor().copy(alpha = 1f))
                    .clickable(onClick = onAvatarClick),
                contentAlignment = Alignment.Center,
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    // The launcher vector carries a lot of transparent margin, so
                    // it is drawn oversized and clipped by the circle: the visible
                    // mark ends up ~29dp inside the 68dp avatar.
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(86.dp),
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nickname,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, lineHeight = 26.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(5.dp))
                // The device stands in for the email other apps put here.
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // The signature sits below the whole block.
        // Nothing is rendered when the user has not written one.
        if (signature.isNotBlank()) {
            Spacer(Modifier.height(18.dp))
            Text(
                text = signature,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Four-column icon grid, the shortcut block of the settings hub. Cells are
 * sized so an icon + label pair sits comfortably with generous vertical air.
 */
@Composable
private fun SettingsIconGrid(entries: List<SecondaryEntry>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
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
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .folkPressScale(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    entry.onClick()
                },
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = entry.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(entry.iconSize),
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
