package com.sunflower.utilityproxy.di

import com.sunflower.utilityproxy.parsing.ServerUriParser
import com.sunflower.utilityproxy.parsing.ShadowsocksUriParser
import com.sunflower.utilityproxy.parsing.TrojanUriParser
import com.sunflower.utilityproxy.parsing.VlessUriParser
import com.sunflower.utilityproxy.parsing.VmessUriParser
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class ParsingModule {
    @Binds
    @IntoSet
    abstract fun bindVlessParser(parser: VlessUriParser): ServerUriParser

    @Binds
    @IntoSet
    abstract fun bindVmessParser(parser: VmessUriParser): ServerUriParser

    @Binds
    @IntoSet
    abstract fun bindTrojanParser(parser: TrojanUriParser): ServerUriParser

    @Binds
    @IntoSet
    abstract fun bindShadowsocksParser(parser: ShadowsocksUriParser): ServerUriParser
}
