package com.sunflower.utilityproxy.tunnel

interface TunnelEngine {
    suspend fun start(tunFd: Int, xrayConfigJson: String): LibXrayResult
    suspend fun stop(): LibXrayResult
    suspend fun isRunning(): Boolean
    suspend fun version(): String?
}
