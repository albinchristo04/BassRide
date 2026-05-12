package com.velcuri.bassride.billing.domain

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/**
 * All billing logic is isolated in this interface.
 * Other modules depend only on this interface — never on implementation details.
 */
interface BillingRepository {
    val isProUnlocked: StateFlow<Boolean>
    suspend fun getProPrice(): String?
    suspend fun launchPurchaseFlow()
    suspend fun restorePurchases()
    suspend fun verifyAndSyncProStatus()
    fun setActivity(activity: Activity)
}
