package com.eulerity.todo.core.data.di

import com.eulerity.todo.core.data.OfflineFirstUserDataRepository
import com.eulerity.todo.core.data.OfflineTaskRepository
import com.eulerity.todo.core.data.TaskRepository
import com.eulerity.todo.core.data.UserDataRepository
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
}
