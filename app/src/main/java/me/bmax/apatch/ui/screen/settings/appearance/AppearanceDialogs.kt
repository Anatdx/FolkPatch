package me.bmax.apatch.ui.screen.settings.appearance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.edit
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils

@Composable
fun homeLayoutStyleToString(style: String): Int {
    return when (style) {
        "kernelsu" -> R.string.settings_home_layout_grid
        "focus" -> R.string.settings_home_layout_focus
        "circle" -> R.string.settings_home_layout_circle
        "dashboard_ui" -> R.string.settings_home_layout_dashboard_pro
        "stats" -> R.string.settings_home_layout_stats
        else -> R.string.settings_home_layout_default
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeLayoutChooseDialog(showDialog: MutableState<Boolean>, onLayoutSelected: (String) -> Unit) {
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
                    text = stringResource(R.string.settings_home_layout_style),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val currentStyle = prefs.getString("home_layout_style", APApplication.HOME_LAYOUT_STYLE_DEFAULT)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AlertDialogDefaults.containerColor,
                    tonalElevation = 2.dp
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_default)) },
                            leadingContent = { RadioButton(selected = currentStyle == "default", onClick = null) },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "default").apply()
                                onLayoutSelected("default")
                                showDialog.value = false
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_grid)) },
                            leadingContent = { RadioButton(selected = currentStyle == "kernelsu", onClick = null) },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "kernelsu").apply()
                                onLayoutSelected("kernelsu")
                                showDialog.value = false
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_focus)) },
                            leadingContent = { RadioButton(selected = currentStyle == "focus", onClick = null) },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "focus").apply()
                                onLayoutSelected("focus")
                                showDialog.value = false
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_circle)) },
                            leadingContent = { RadioButton(selected = currentStyle == "circle", onClick = null) },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "circle").apply()
                                onLayoutSelected("circle")
                                showDialog.value = false
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_dashboard_pro)) },
                            leadingContent = { RadioButton(selected = currentStyle == "dashboard_ui", onClick = null) },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "dashboard_ui").apply()
                                onLayoutSelected("dashboard_ui")
                                showDialog.value = false
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_stats)) },
                            leadingContent = { RadioButton(selected = currentStyle == "stats", onClick = null) },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "stats").apply()
                                onLayoutSelected("stats")
                                showDialog.value = false
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
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

