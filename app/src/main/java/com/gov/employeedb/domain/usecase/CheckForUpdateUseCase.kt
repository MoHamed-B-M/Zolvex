package com.gov.employeedb.domain.usecase

import com.gov.employeedb.data.remote.UpdateChecker
import com.gov.employeedb.domain.model.UpdateCheckResult
import javax.inject.Inject

/**
 * Use case to check for application updates.
 */
class CheckForUpdateUseCase @Inject constructor(
    private val updateChecker: UpdateChecker,
) {
    suspend operator fun invoke(force: Boolean = false): UpdateCheckResult =
        updateChecker.checkForUpdate(force)
}
