package com.sunflower.utilityproxy.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunflower.utilityproxy.R
import com.sunflower.utilityproxy.ui.components.GlassCard

@Composable
private fun updateIntervalLabels() = mapOf(
    "never" to stringResource(R.string.interval_never),
    "6h" to stringResource(R.string.interval_6h),
    "12h" to stringResource(R.string.interval_12h),
    "daily" to stringResource(R.string.interval_daily),
)

@Composable
private fun themeLabels() = mapOf(
    "system" to stringResource(R.string.theme_system),
    "light" to stringResource(R.string.theme_light),
    "dark" to stringResource(R.string.theme_dark),
)

/**
 * Реально работает: Kill Switch / Allow LAN / Auto connect / Автозапуск /
 * тема / интервал автообновления подписок (все — DataStore через
 * SettingsRepository). Разделы "Настройки туннеля", "Маршрутизация",
 * "Пинг", "Inbounds", "Прокси для выбранных приложений" сюда намеренно
 * не добавлены — они требуют либо непроверенной ещё Xray config-схемы
 * (fragment/mux/routing — см. ARCHITECTURE.md), либо экрана выбора
 * приложений, которого пока нет. Показывать переключатели, которые ничего
 * не генерируют, — ровно то, что промт просит не делать (пункт 85).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenLogs: () -> Unit,
    onOpenReset: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var intervalMenuExpanded by remember { mutableStateOf(false) }
    var themeMenuExpanded by remember { mutableStateOf(false) }

    val updateIntervalLabels = updateIntervalLabels()
    val themeLabels = themeLabels()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_settings)) }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SettingsSection(title = stringResource(R.string.section_vpn_settings)) {
                    SettingsSwitchRow(stringResource(R.string.label_kill_switch), state.killSwitch, viewModel::setKillSwitch)
                    SettingsSwitchRow(stringResource(R.string.label_allow_lan), state.allowLan, viewModel::setAllowLan)
                    SettingsSwitchRow(
                        stringResource(R.string.label_auto_connect),
                        state.autoConnect,
                        viewModel::setAutoConnect,
                    )
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.section_autostart)) {
                    SettingsSwitchRow(
                        stringResource(R.string.label_autostart_on_boot),
                        state.autostartOnBoot,
                        viewModel::setAutostartOnBoot,
                    )
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.section_appearance)) {
                    SettingsDropdownRow(
                        label = stringResource(R.string.label_theme),
                        selectedLabel = themeLabels[state.theme] ?: state.theme,
                        options = themeLabels,
                        expanded = themeMenuExpanded,
                        onExpandedChange = { themeMenuExpanded = it },
                        onSelect = viewModel::setTheme,
                    )
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.section_subscriptions)) {
                    SettingsDropdownRow(
                        label = stringResource(R.string.label_auto_update),
                        selectedLabel = updateIntervalLabels[state.updateInterval] ?: state.updateInterval,
                        options = updateIntervalLabels,
                        expanded = intervalMenuExpanded,
                        onExpandedChange = { intervalMenuExpanded = it },
                        onSelect = viewModel::setUpdateInterval,
                    )
                }
            }
            item {
                SettingsSection(title = null) {
                    TextButton(onClick = onOpenLogs) { Text(stringResource(R.string.nav_logs)) }
                    TextButton(onClick = onOpenReset) { Text(stringResource(R.string.nav_reset)) }
                    TextButton(onClick = onOpenAbout) { Text(stringResource(R.string.nav_about)) }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String?, content: @Composable () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            title?.let {
                Text(it, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            }
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsDropdownRow(
    label: String,
    selectedLabel: String,
    options: Map<String, String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Box {
            TextButton(onClick = { onExpandedChange(true) }) { Text(selectedLabel) }
            DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
                options.forEach { (value, optionLabel) ->
                    DropdownMenuItem(
                        text = { Text(optionLabel) },
                        onClick = {
                            onSelect(value)
                            onExpandedChange(false)
                        },
                    )
                }
            }
        }
    }
}
