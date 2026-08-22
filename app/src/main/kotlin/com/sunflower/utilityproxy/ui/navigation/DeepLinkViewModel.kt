package com.sunflower.utilityproxy.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunflower.utilityproxy.data.AppLogger
import com.sunflower.utilityproxy.data.SubscriptionRepository
import com.sunflower.utilityproxy.parsing.ParseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class DeepLinkViewModel @Inject constructor(
    private val repository: SubscriptionRepository,
) : ViewModel() {

    fun importServerLink(uri: String) {
        viewModelScope.launch {
            when (val result = repository.importSingleServerUri(uri)) {
                is ParseResult.Success -> AppLogger.log("DeepLink", "Сервер импортирован из deep link")
                is ParseResult.Failure -> AppLogger.log("DeepLink", "Не удалось импортировать: ${result.reason}")
            }
        }
    }
}
