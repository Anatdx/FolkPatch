package me.bmax.apatch.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.util.SystemInfoCollector

@Composable
fun HomeV4DeviceStatusCard(isWallpaperMode: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var deviceStatus by remember { mutableStateOf(SystemInfoCollector.DeviceStatus()) }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            deviceStatus = SystemInfoCollector.collectDeviceStatus(context)
            delay(10000)
        }
    }

    HomeV4MagiskStyleCard(
        title = stringResource(R.string.home_device_status_title),
        icon = Icons.Outlined.Settings,
        actionText = "",
        showAction = false,
        isWallpaperMode = isWallpaperMode,
        onActionClick = {},
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            HomeV4StatusCircle(
                value = "${deviceStatus.batteryTemp}°C",
                label = stringResource(R.string.home_device_status_battery_temp),
                progress = (deviceStatus.batteryTemp / 50f).coerceIn(0f, 1f),
                color = MaterialTheme.colorScheme.primary
            )
            HomeV4StatusCircle(
                value = "${deviceStatus.cpuUsage}%",
                label = stringResource(R.string.home_device_status_cpu_load),
                progress = (deviceStatus.cpuUsage / 100f).coerceIn(0f, 1f),
                color = MaterialTheme.colorScheme.secondary
            )
            HomeV4StatusCircle(
                value = "${deviceStatus.batteryLevel}%",
                label = stringResource(R.string.home_device_status_battery_level),
                progress = (deviceStatus.batteryLevel / 100f).coerceIn(0f, 1f),
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
fun HomeV4StatusCircle(
    value: String,
    label: String,
    progress: Float,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(80.dp)
        ) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = color.copy(alpha = 0.2f),
                strokeWidth = 8.dp,
            )
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = color,
                strokeWidth = 8.dp,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HomeV4MagiskStyleCard(
    title: String,
    icon: ImageVector,
    actionText: String,
    showAction: Boolean,
    actionEnabled: Boolean = true,
    isWallpaperMode: Boolean,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    TonalCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                if (showAction) {
                    Button(
                        onClick = onActionClick,
                        enabled = actionEnabled,
                        contentPadding = PaddingValues(horizontal = 24.dp)
                    ) {
                        Text(text = actionText)
                    }
                }
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * 模式标签芯片
 */
@Composable
fun ModeLabelChip(label: String, contentColor: Color) {
    Surface(
        color = contentColor.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

/**
 * 版本信息列
 */
@Composable
fun VersionInfoColumn(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Android补丁状态卡片
 */
@Composable
fun AndroidPatchCard(
    apState: APApplication.State,
    kpState: APApplication.State,
    showInstallDialog: MutableState<Boolean>,
    isWallpaperMode: Boolean
) {
    val containerColor = when {
        BackgroundConfig.isCustomBackgroundEnabled -> {
            MaterialTheme.colorScheme.surface.copy(alpha = BackgroundConfig.customBackgroundOpacity)
        }
        else -> {
            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            when (apState) {
                APApplication.State.ANDROIDPATCH_INSTALLED -> {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                APApplication.State.ANDROIDPATCH_INSTALLING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                APApplication.State.ANDROIDPATCH_NEED_UPDATE -> {
                    Icon(
                        imageVector = Icons.Outlined.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Outlined.Android,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // 状态文字
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.android_patch),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            // 操作按钮
            FilledTonalButton(
                onClick = {
                    when (apState) {
                        APApplication.State.ANDROIDPATCH_NOT_INSTALLED,
                        APApplication.State.ANDROIDPATCH_NEED_UPDATE -> {
                            APApplication.installApatch()
                        }
                        APApplication.State.ANDROIDPATCH_INSTALLED -> {
                            APApplication.uninstallApatch()
                        }
                        else -> {}
                    }
                },
                enabled = apState != APApplication.State.ANDROIDPATCH_INSTALLING &&
                    apState != APApplication.State.ANDROIDPATCH_UNINSTALLING &&
                    apState != APApplication.State.UNKNOWN_STATE
            ) {
                when (apState) {
                    APApplication.State.ANDROIDPATCH_NOT_INSTALLED -> 
                        Text(stringResource(R.string.home_ap_cando_install))
                    APApplication.State.ANDROIDPATCH_NEED_UPDATE -> 
                        Text(stringResource(R.string.home_kp_cando_update))
                    APApplication.State.ANDROIDPATCH_INSTALLING,
                    APApplication.State.ANDROIDPATCH_UNINSTALLING -> 
                        Icon(Icons.Outlined.Cached, contentDescription = "busy")
                    else -> 
                        Text(stringResource(R.string.home_ap_cando_uninstall))
                }
            }
        }
    }
}

