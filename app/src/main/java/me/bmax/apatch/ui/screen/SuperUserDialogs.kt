package me.bmax.apatch.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.SecureFlagPolicy
import me.bmax.apatch.R
import me.bmax.apatch.ui.viewmodel.SuperUserViewModel
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils.Companion.setupWindowBlurListener

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchExcludeDialog(
    onDismiss: () -> Unit,
    onExclude: () -> Unit,
    onReverseExclude: () -> Unit
) {
    val title = stringResource(R.string.su_batch_exclude_title)
    val content = stringResource(R.string.su_batch_exclude_content)
    val excludeText = stringResource(R.string.su_exclude_btn)
    val reverseText = stringResource(R.string.su_exclude_reverse_btn)
    val cancelText = stringResource(android.R.string.cancel)

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
            securePolicy = SecureFlagPolicy.SecureOff,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(PaddingValues(all = 24.dp))) {
                Box(
                    Modifier
                        .padding(PaddingValues(bottom = 16.dp))
                        .align(Alignment.Start)
                ) {
                    Text(text = title, style = MaterialTheme.typography.headlineSmall)
                }
                Box(
                    Modifier
                        .weight(weight = 1f, fill = false)
                        .padding(PaddingValues(bottom = 24.dp))
                        .align(Alignment.Start)
                ) {
                    Text(text = content, style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = cancelText)
                    }
                    TextButton(onClick = onExclude) {
                        Text(text = excludeText)
                    }
                    TextButton(onClick = onReverseExclude) {
                        Text(text = reverseText)
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchActionConfirmDialog(
    action: SuperUserViewModel.BatchAction,
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val title = stringResource(R.string.su_multi_select_title)
    val content = when (action) {
        SuperUserViewModel.BatchAction.GRANT_ROOT -> stringResource(R.string.su_multi_select_confirm_grant, count)
        SuperUserViewModel.BatchAction.REVOKE_ROOT -> stringResource(R.string.su_multi_select_confirm_revoke, count)
        SuperUserViewModel.BatchAction.EXCLUDE -> stringResource(R.string.su_multi_select_confirm_exclude, count)
    }
    val confirmText = when (action) {
        SuperUserViewModel.BatchAction.GRANT_ROOT -> stringResource(R.string.su_multi_select_grant_root)
        SuperUserViewModel.BatchAction.REVOKE_ROOT -> stringResource(R.string.su_multi_select_revoke_root)
        SuperUserViewModel.BatchAction.EXCLUDE -> stringResource(R.string.su_multi_select_exclude)
    }
    val cancelText = stringResource(android.R.string.cancel)

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
            securePolicy = SecureFlagPolicy.SecureOff,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(PaddingValues(all = 24.dp))) {
                Box(
                    Modifier
                        .padding(PaddingValues(bottom = 16.dp))
                        .align(Alignment.Start)
                ) {
                    Text(text = title, style = MaterialTheme.typography.headlineSmall)
                }
                Box(
                    Modifier
                        .weight(weight = 1f, fill = false)
                        .padding(PaddingValues(bottom = 24.dp))
                        .align(Alignment.Start)
                ) {
                    Text(text = content, style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = cancelText)
                    }
                    TextButton(onClick = onConfirm) {
                        Text(text = confirmText)
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionDialog(
    app: SuperUserViewModel.AppInfo,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onForceStop: () -> Unit
) {
    val title = stringResource(R.string.su_app_action_title)
    val content = stringResource(R.string.su_app_action_content)
    val launchText = stringResource(R.string.su_app_action_launch)
    val forceStopText = stringResource(R.string.su_app_action_force_stop)
    val cancelText = stringResource(android.R.string.cancel)

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
            securePolicy = SecureFlagPolicy.SecureOff,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(PaddingValues(all = 24.dp))) {
                Box(
                    Modifier
                        .padding(PaddingValues(bottom = 16.dp))
                        .align(Alignment.Start)
                ) {
                    Text(text = title, style = MaterialTheme.typography.headlineSmall)
                }
                Box(
                    Modifier
                        .weight(weight = 1f, fill = false)
                        .padding(PaddingValues(bottom = 24.dp))
                        .align(Alignment.Start)
                ) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = cancelText)
                    }
                    TextButton(onClick = onLaunch) {
                        Text(text = launchText)
                    }
                    TextButton(onClick = onForceStop) {
                        Text(text = forceStopText)
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}
