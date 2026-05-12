package com.velcuri.bassride.bluetooth.di

import com.velcuri.bassride.bluetooth.data.BluetoothRepositoryImpl
import com.velcuri.bassride.bluetooth.domain.repository.BluetoothRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BluetoothModule {

    @Binds
    @Singleton
    abstract fun bindBluetoothRepository(impl: BluetoothRepositoryImpl): BluetoothRepository
}
