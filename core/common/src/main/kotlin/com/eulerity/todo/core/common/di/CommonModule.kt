package com.eulerity.todo.core.common.di

import com.eulerity.todo.core.common.DateTimeProvider
import com.eulerity.todo.core.common.DefaultDateTimeProvider
import com.eulerity.todo.core.common.DefaultDispatcher
import com.eulerity.todo.core.common.IoDispatcher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CommonBindModule {
    @Binds
    @Singleton
    abstract fun bindDateTimeProvider(impl: DefaultDateTimeProvider): DateTimeProvider
}

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @Provides
    @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.System

    @Provides
    @Singleton
    fun provideTimeZone(): TimeZone = TimeZone.currentSystemDefault()
}
