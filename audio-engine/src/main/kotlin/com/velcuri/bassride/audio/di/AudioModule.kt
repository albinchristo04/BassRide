package com.velcuri.bassride.audio.di

import com.velcuri.bassride.audio.data.PresetRepositoryImpl
import com.velcuri.bassride.audio.domain.repository.PresetRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    @Binds
    @Singleton
    abstract fun bindPresetRepository(impl: PresetRepositoryImpl): PresetRepository
}
