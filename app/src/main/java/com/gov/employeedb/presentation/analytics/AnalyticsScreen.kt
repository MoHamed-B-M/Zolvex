package com.gov.employeedb.presentation.analytics

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gov.employeedb.R
import com.gov.employeedb.data.excel.ExcelMime

/**
 * Analytics: the Age / Service / Department / Grade tabs over the workforce, with the department
 * filter and the xlsx export (PLAN.md Phase-10 checklist).
 *
 * This is the stateful half of the screen. It owns the three things [AnalyticsContent] cannot: the
 * Hilt view model, the SAF `CreateDocument` launcher, and the snackbar the export reports through.
 * The document is created first and the workbook written into the returned uri, so the user picks the
 * name and the location and the app never needs a storage permission.
 *
 * The export always covers the whole workforce: [com.gov.employeedb.domain.usecase.ExportAnalyticsUseCase]
 * takes no department, so a filtered screen still writes the full analysis.
 */
@Composable
fun AnalyticsScreen(
    onImport: () -> Unit,
    onEmployeeClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val labels = analyticsLabels()
    val fileName = stringResource(R.string.analytics) + REPORT_EXTENSION
    val createDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ExcelMime.XLSX_MIME),
    ) { uri ->
        if (uri != null) {
            viewModel.onEvent(AnalyticsEvent.ExportAnalysis(uri, labels))
        }
    }
    val exportedLabel = stringResource(R.string.analysis_exported)
    val failedLabel = stringResource(R.string.error_generic)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            val message = when (effect) {
                AnalyticsEffect.AnalysisExported -> exportedLabel
                AnalyticsEffect.ExportFailed -> failedLabel
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    AnalyticsContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onSelectDepartment = { viewModel.onEvent(AnalyticsEvent.SelectDepartment(it)) },
        onExport = { createDocument.launch(fileName) },
        onImport = onImport,
        onRetry = { viewModel.onEvent(AnalyticsEvent.Retry) },
        onEmployeeClick = onEmployeeClick,
        modifier = modifier,
    )
}

/**
 * The analytics sheet's captions, positional to match [ExcelWriter]'s `ANALYTICS_*` contract, so the
 * workbook is written in the locale the screen is showing.
 */
@Composable
private fun analyticsLabels(): List<String> = listOf(
    stringResource(R.string.analytics),
    stringResource(R.string.analytics_age),
    stringResource(R.string.analytics_service),
    stringResource(R.string.analytics_department),
    stringResource(R.string.analytics_grade),
    stringResource(R.string.longest_serving),
    stringResource(R.string.label),
    stringResource(R.string.count),
    stringResource(R.string.total_employees),
    stringResource(R.string.average_age),
    stringResource(R.string.median_age),
    stringResource(R.string.average_service),
    stringResource(R.string.median_service),
    stringResource(R.string.retirement_soon),
)

private const val REPORT_EXTENSION = ".xlsx"
