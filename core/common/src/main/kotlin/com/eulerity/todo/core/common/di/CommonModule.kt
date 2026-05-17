/*
 * Copyright 2026 Eulerity, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eulerity.todo.core.common.di

import com.eulerity.todo.core.common.DateChangeBroadcaster
import com.eulerity.todo.core.common.DateTimeProvider
import com.eulerity.todo.core.common.DefaultDateTimeProvider
import com.eulerity.todo.core.common.DefaultDispatcher
import com.eulerity.todo.core.common.IoDispatcher
import com.eulerity.todo.core.common.SystemDateChangeBroadcaster
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

    @Binds
    @Singleton
    abstract fun bindDateChangeBroadcaster(impl: SystemDateChangeBroadcaster): DateChangeBroadcaster
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
