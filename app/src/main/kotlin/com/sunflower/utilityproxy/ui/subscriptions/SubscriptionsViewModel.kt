package com.sunflower.utilityproxy.ui.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunflower.utilityproxy.data.SubscriptionRepository
import com.sunflower.utilityproxy.data.local.SubscriptionEntity
import com.sunflower.utilityproxy.parsing.ParseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val repository: SubscriptionRepository,
) : ViewModel() {

    val subscriptions: StateFlow<List<SubscriptionEntity>> = repository.observeSubscriptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState

    fun importFromUrl(name: String, url: String, allowInsecure: Boolean, sendHwidCookie: Boolean) {
        viewModelScope.launch {
            _importState.value = ImportState.Loading
            val result = repository.importFromUrl(
                name = name,
                url = url,
                allowInsecure = allowInsecure,
                sendHwidCookie = sendHwidCookie,
                hwid = null, // TODO: подключить SettingsRepository.getOrCreateDeviceId()
                encrypted = false,
            )
            _importState.value = when (result) {
                is ParseResult.Success -> ImportState.Success
                is ParseResult.Failure -> ImportState.Error(result.reason)
            }
        }
    }

    fun refresh(subscription: SubscriptionEntity) {
        viewModelScope.launch { repository.refresh(subscription) }
    }

    fun delete(subscription: SubscriptionEntity) {
        viewModelScope.launch { repository.delete(subscription) }
    }

    fun resetImportState() {
        _importState.value = ImportState.Idle
    }
}

sealed class ImportState {
    data object Idle : ImportState()
    data object Loading : ImportState()
    data object Success : ImportState()
    data class Error(val message: String) : ImportState()
}
