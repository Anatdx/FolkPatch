package me.bmax.apatch.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.folkGroupColor

/** Square size the avatar is decoded at, in pixels. */
private const val PROFILE_AVATAR_PX = 256

/**
 * Local profile editor: pick an avatar, set a nickname and a short signature,
 * or fall back to the defaults. No account, no network.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditSheet(
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
fun ProfileTextField(
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
fun ProfileHeader(
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
