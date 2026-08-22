package com.sunflower.utilityproxy.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunflower.utilityproxy.data.SettingsRepository
import com.sunflower.utilityproxy.data.SubscriptionRepository
import com.sunflower.utilityproxy.data.SubscriptionUpdateScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val killSwitch: Boolean = false,
    val allowLan: Boolean = false,
    val autoConnect: Boolean = false,
    val theme: String = "system",
    val autostartOnBoot: Boolean = false,
    val updateInterval: String = "never",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val updateScheduler: SubscriptionUpdateScheduler,
) : ViewModel() {

    // combine() с позиционными типами поддерживает максимум 5 потоков сразу,
    // а у нас 6 разнотипных (Boolean/String) — комбинируем в два уровня.
    private data class PartialSettings(
        val killSwitch: Boolean,
        val allowLan: Boolean,
        val autoConnect: Boolean,
        val theme: String,
        val autostartOnBoot: Boolean,
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            repository.killSwitch,
            repository.allowLan,
            repository.autoConnect,
            repository.theme,
            repository.autostartOnBoot,
        ) { killSwitch, allowLan, autoConnect, theme, autostartOnBoot ->
            PartialSettings(killSwitch, allowLan, autoConnect, theme, autostartOnBoot)
        },
        repository.updateInterval,
    ) { partial, updateInterval ->
        SettingsUiState(
            killSwitch = partial.killSwitch,
            allowLan = partial.allowLan,
            autoConnect = partial.autoConnect,
            theme = partial.theme,
            autostartOnBoot = partial.autostartOnBoot,
            updateInterval = updateInterval,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setKillSwitch(enabled: Boolean) = viewModelScope.launch { repository.setKillSwitch(enabled) }
    fun setAllowLan(enabled: Boolean) = viewModelScope.launch { repository.setAllowLan(enabled) }
    fun setAutoConnect(enabled: Boolean) = viewModelScope.launch { repository.setAutoConnect(enabled) }
    fun setTheme(value: String) = viewModelScope.launch { repository.setTheme(value) }
    fun setAutostartOnBoot(enabled: Boolean) = viewModelScope.launch { repository.setAutostartOnBoot(enabled) }
    fun setUpdateInterval(value: String) = viewModelScope.launch {
        repository.setUpdateInterval(value)
        updateScheduler.apply(value)
    }

    fun resetSettings() = viewModelScope.launch { repository.resetSettings() }
    fun deleteAllServers() = viewModelScope.launch { subscriptionRepository.deleteAllServers() }
    fun deleteAllSubscriptions() = viewModelScope.launch { subscriptionRepository.deleteAllSubscriptions() }
}
