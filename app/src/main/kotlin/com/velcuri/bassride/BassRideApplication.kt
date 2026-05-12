package com.velcuri.bassride

import android.app.Application
import com.velcuri.bassride.billing.domain.usecase.VerifyPurchaseUseCase
import com.velcuri.bassride.data.seeder.DatabaseSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class BassRideApplication : Application() {

    @Inject lateinit var databaseSeeder: DatabaseSeeder
    @Inject lateinit var verifyPurchaseUseCase: VerifyPurchaseUseCase

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { databaseSeeder.seedIfNeeded() }
        // Re-verify Pro purchase state on every cold start against Play servers.
        // The cached Room value is shown immediately by BillingRepositoryImpl.init
        // while this async verification runs in the background.
        applicationScope.launch { verifyPurchaseUseCase() }
    }
}

