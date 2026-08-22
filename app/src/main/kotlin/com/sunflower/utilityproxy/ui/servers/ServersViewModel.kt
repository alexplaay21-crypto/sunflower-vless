package com.sunflower.utilityproxy.ui.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunflower.utilityproxy.data.local.ServerDao
import com.sunflower.utilityproxy.data.local.ServerEntity
import com.sunflower.utilityproxy.tunnel.TunnelManager
import com.sunflower.utilityproxy.tunnel.TunnelState
import com.sunflower.utilityproxy.tunnel.XrayConfigBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ServersViewModel @Inject constructor(
    private val serverDao: ServerDao,
    tunnelManager: TunnelManager,
) : ViewModel() {

    val servers: StateFlow<List<ServerEntity>> = serverDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tunnelState: StateFlow<TunnelState> = tunnelManager.state

    /** Может бросить IllegalArgumentException для неизвестного протокола — вызывающая сторона обязана поймать. */
    fun buildConfigFor(server: ServerEntity): String = XrayConfigBuilder.build(server)

    fun toggleFavorite(server: ServerEntity) {
        viewModelScope.launch { serverDao.setFavorite(server.id, !server.isFavorite) }
    }
}
