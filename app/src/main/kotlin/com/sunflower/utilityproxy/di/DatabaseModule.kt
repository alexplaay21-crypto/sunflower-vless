package com.sunflower.utilityproxy.di

import android.content.Context
import androidx.room.Room
import com.sunflower.utilityproxy.data.local.ServerDao
import com.sunflower.utilityproxy.data.local.SubscriptionDao
import com.sunflower.utilityproxy.data.local.SunflowerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SunflowerDatabase =
        Room.databaseBuilder(context, SunflowerDatabase::class.java, "sunflower.db").build()

    @Provides
    fun provideSubscriptionDao(database: SunflowerDatabase): SubscriptionDao = database.subscriptionDao()

    @Provides
    fun provideServerDao(database: SunflowerDatabase): ServerDao = database.serverDao()
}
