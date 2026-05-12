package com.velcuri.bassride.billing.domain.usecase

import com.velcuri.bassride.billing.domain.BillingRepository
import javax.inject.Inject

class VerifyPurchaseUseCase @Inject constructor(
    private val billingRepository: BillingRepository
) {
    suspend operator fun invoke() {
        billingRepository.verifyAndSyncProStatus()
    }
}
