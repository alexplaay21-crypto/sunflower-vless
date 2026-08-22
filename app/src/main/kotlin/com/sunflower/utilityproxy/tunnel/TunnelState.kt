package com.sunflower.utilityproxy.tunnel

sealed class TunnelState {
    data object Disconnected : TunnelState()
    data object Connecting : TunnelState()
    data class Connected(val serverId: Long, val since: Long) : TunnelState()
    data class Error(val message: String) : TunnelState()
}
