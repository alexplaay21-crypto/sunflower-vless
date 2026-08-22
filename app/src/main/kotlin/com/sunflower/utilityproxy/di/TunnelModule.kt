package com.sunflower.utilityproxy.di

import com.sunflower.utilityproxy.tunnel.TunnelEngine
import com.sunflower.utilityproxy.tunnel.XrayTunnelEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TunnelModule {
    @Binds
    abstract fun bindTunnelEngine(impl: XrayTunnelEngine): TunnelEngine
}
