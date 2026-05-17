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

package com.eulerity.todo.core.data.di

import com.eulerity.todo.core.data.OfflineFirstUserDataRepository
import com.eulerity.todo.core.data.OfflineTaskRepository
import com.eulerity.todo.core.data.TaskRepository
import com.eulerity.todo.core.data.UserDataRepository
import com.eulerity.todo.core.data.notification.AlarmManagerTaskExpiryScheduler
import com.eulerity.todo.core.data.notification.TaskExpiryScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: OfflineTaskRepository): TaskRepository

    @Binds
    @Singleton
    abstract fun bindUserDataRepository(impl: OfflineFirstUserDataRepository): UserDataRepository

    @Binds
    @Singleton
    abstract fun bindTaskExpiryScheduler(impl: AlarmManagerTaskExpiryScheduler): TaskExpiryScheduler
}
