package com.sunflower.utilityproxy.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sunflower.utilityproxy.R
import com.sunflower.utilityproxy.data.AppLogger
import com.sunflower.utilityproxy.ui.components.GlassCard

// @StringRes id вместо готовой строки — enum инициализируется вне
// composable-контекста, stringResource() там вызвать нельзя; резолвим
// в реальную строку прямо в месте использования (Button/AlertDialog).
private enum class ResetAction(val labelRes: Int, val messageRes: Int) {
    SETTINGS(R.string.reset_settings_label, R.string.reset_settings_message),
    SERVERS(R.string.reset_servers_label, R.string.reset_servers_message),
    SUBSCRIPTIONS(R.string.reset_subscriptions_label, R.string.reset_subscriptions_message),
    FULL(R.string.reset_full_label, R.string.reset_full_message),
}

/** Пункт 34 промта — каждое действие за отдельным подтверждением. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    var pendingAction by remember { mutableStateOf<ResetAction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_reset)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        GlassCard(modifier = Modifier.fillMaxWidth().padding(padding).padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                ResetAction.entries.forEach { action ->
                    OutlinedButton(
                        onClick = { pendingAction = action },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) { Text(stringResource(action.labelRes)) }
                }
            }
        }
    }

    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(stringResource(action.labelRes)) },
            text = { Text(stringResource(action.messageRes)) },
            confirmButton = {
                TextButton(onClick = {
                    when (action) {
                        ResetAction.SETTINGS -> viewModel.resetSettings()
                        ResetAction.SERVERS -> viewModel.deleteAllServers()
                        ResetAction.SUBSCRIPTIONS -> viewModel.deleteAllSubscriptions()
                        ResetAction.FULL -> {
                            viewModel.resetSettings()
                            viewModel.deleteAllServers()
                            viewModel.deleteAllSubscriptions()
                            AppLogger.clear()
                        }
                    }
                    pendingAction = null
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}
