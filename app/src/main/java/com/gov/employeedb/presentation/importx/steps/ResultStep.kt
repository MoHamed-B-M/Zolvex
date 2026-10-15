package com.gov.employeedb.presentation.importx.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.gov.employeedb.R
import com.gov.employeedb.data.excel.ImportResult
import com.gov.employeedb.presentation.common.components.AnimatedCheckmark
import com.gov.employeedb.presentation.common.components.LabeledValue
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants
import com.gov.employeedb.presentation.importx.ImportStep
import com.gov.employeedb.presentation.importx.ImportUiState
import com.gov.employeedb.presentation.importx.messageRes

/**
 * The wizard's last stop (PLAN.md Phase-7 checklist): a success mark or a warning, the three
 * counters, the failure report action when anything was rejected, and the way back to the list.
 *
 * A run that could not start at all (no result, only an [com.gov.employeedb.core.AppError]) shows
 * the same warning face with Retry, so a failure in the middle of the wizard never dead-ends.
 */
@Composable
fun ResultStep(
    state: ImportUiState,
    onRetry: () -> Unit,
    onDownloadReport: () -> Unit,
    onViewEmployees: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val result = state.result
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (result == null) {
            FailureFace(
                messageRes = state.error?.messageRes() ?: R.string.error_generic,
                onRetry = onRetry,
                onViewEmployees = onViewEmployees,
            )
            return@Column
        }
        if (result.failed > 0) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                modifier = Modifier.size(ICON_SIZE),
                tint = MaterialTheme.colorScheme.tertiary,
            )
        } else {
            AnimatedCheckmark()
        }
        Spacer(modifier = Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            LabeledValue(
                label = stringResource(R.string.rows_imported),
                value = result.imported,
            )
            LabeledValue(
                label = stringResource(R.string.rows_skipped),
                value = result.skippedDuplicates,
            )
            LabeledValue(
                label = stringResource(R.string.rows_failed),
                value = result.failed,
            )
        }
        if (result.errorsTruncated) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.error_report_truncated, result.errors.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        val reportError = state.error
        if (reportError != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(reportError.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        if (result.failed > 0) {
            Button(onClick = onDownloadReport, enabled = !state.isDownloadingReport) {
                Text(text = stringResource(R.string.download_error_report))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        OutlinedButton(onClick = onViewEmployees) {
            Text(text = stringResource(R.string.view_employees))
        }
    }
}

@Composable
private fun FailureFace(
    messageRes: Int,
    onRetry: () -> Unit,
    onViewEmployees: () -> Unit,
) {
    Icon(
        imageVector = Icons.Outlined.ErrorOutline,
        contentDescription = null,
        modifier = Modifier.size(ICON_SIZE),
        tint = MaterialTheme.colorScheme.error,
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = stringResource(messageRes),
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(32.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onViewEmployees) {
            Text(text = stringResource(R.string.view_employees))
        }
        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.retry))
        }
    }
}

private val ICON_SIZE = 72.dp

@Preview(name = "ResultStep")
@Composable
private fun ResultStepPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        ResultStep(
            state = ImportUiState(
                step = ImportStep.RESULT,
                result = ImportResult(
                    imported = 128,
                    skippedDuplicates = 4,
                    failed = 0,
                    errors = emptyList(),
                    batchId = "preview",
                    errorsTruncated = false,
                ),
            ),
            onRetry = {},
            onDownloadReport = {},
            onViewEmployees = {},
        )
    }
}
