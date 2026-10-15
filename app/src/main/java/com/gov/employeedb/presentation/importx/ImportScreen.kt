package com.gov.employeedb.presentation.importx

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gov.employeedb.R
import com.gov.employeedb.data.excel.ExcelMime
import com.gov.employeedb.data.excel.ExcelWriter.ErrorReportLabels
import com.gov.employeedb.presentation.importx.steps.ImportingStep
import com.gov.employeedb.presentation.importx.steps.MappingStep
import com.gov.employeedb.presentation.importx.steps.PreviewStep
import com.gov.employeedb.presentation.importx.steps.ResultStep
import com.gov.employeedb.presentation.importx.steps.SelectFileStep
import com.gov.employeedb.presentation.theme.AppMotion

private const val SELECT_PAGE = 0
private const val MAPPING_PAGE = 1
private const val PREVIEW_PAGE = 2

/**
 * The four-step import wizard (PLAN.md Phase 7): pick, map, preview, stream, report.
 *
 * The pager is the visible frame, but [ImportViewModel] owns the position: a swipe could otherwise
 * land on the preview page with an unconfirmed mapping, so the pages are driven from `state.step`
 * instead of from the user's finger. The top bar's arrow leaves the wizard from the first and last
 * pages and steps back everywhere else, and becomes a Cancel action while rows are streaming.
 */
@Composable
fun ImportScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImportViewModel = hiltViewModel(),
    initialUri: android.net.Uri? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(pageCount = { ImportStep.DOTS.size })

    // Auto-trigger file selection if initialUri is provided
    LaunchedEffect(initialUri) {
        initialUri?.let { uri ->
            viewModel.onEvent(ImportEvent.FileSelected(uri))
        }
    }
    val labels = errorReportLabels(state)
    val reportName = stringResource(R.string.error_report) + REPORT_EXTENSION
    val createReport = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ExcelMime.XLSX_MIME),
    ) { uri ->
        if (uri != null) {
            viewModel.onEvent(ImportEvent.DownloadErrorReport(uri, labels))
        }
    }
    val savedLabel = stringResource(R.string.error_report_saved)
    val pagerSpec = AppMotion.spatial<Float>()

    LaunchedEffect(state.step) {
        pagerState.animateScrollToPage(state.step.pageIndex, animationSpec = pagerSpec)
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ImportEffect.NavigateToEmployees -> onNavigateUp()
                ImportEffect.ErrorReportSaved -> snackbarHostState.showSnackbar(savedLabel)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ImportTopBar(
                step = state.step,
                onNavigateUp = onNavigateUp,
                onBack = { viewModel.onEvent(ImportEvent.Back) },
                onCancelImport = { viewModel.onEvent(ImportEvent.CancelImport) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.systemBars.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            StepDots(current = state.step)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false,
            ) { page ->
                when (page) {
                    SELECT_PAGE -> SelectFileStep(
                        fileName = state.fileName,
                        isReading = state.isReadingFile,
                        error = state.error,
                        onFileSelected = { viewModel.onEvent(ImportEvent.FileSelected(it)) },
                    )

                    MAPPING_PAGE -> MappingStep(
                        state = state,
                        onFieldChange = { column, field ->
                            viewModel.onEvent(ImportEvent.MappingChanged(column, field))
                        },
                        onSkipDuplicatesChange = {
                            viewModel.onEvent(ImportEvent.SkipDuplicatesChanged(it))
                        },
                        onNext = { viewModel.onEvent(ImportEvent.Next) },
                    )

                    PREVIEW_PAGE -> if (state.step == ImportStep.IMPORTING) {
                        ImportingStep(
                            fraction = state.progressFraction,
                            processed = state.processed,
                            total = state.totalRows,
                            onCancel = { viewModel.onEvent(ImportEvent.CancelImport) },
                        )
                    } else {
                        PreviewStep(
                            state = state,
                            onImport = { viewModel.onEvent(ImportEvent.Import) },
                        )
                    }

                    else -> ResultStep(
                        state = state,
                        onRetry = { viewModel.onEvent(ImportEvent.Import) },
                        onDownloadReport = { createReport.launch(reportName) },
                        onViewEmployees = { viewModel.onEvent(ImportEvent.ViewEmployees) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportTopBar(
    step: ImportStep,
    onNavigateUp: () -> Unit,
    onBack: () -> Unit,
    onCancelImport: () -> Unit,
) {
    val leavesWizard = step == ImportStep.SELECT || step == ImportStep.RESULT
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.import_excel),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = if (leavesWizard) onNavigateUp else onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                )
            }
        },
        actions = {
            if (step == ImportStep.IMPORTING) {
                TextButton(onClick = onCancelImport) {
                    Text(text = stringResource(R.string.cancel_import))
                }
            }
        },
    )
}

/** The animated progress dots; the active one widens, so the step reads without a label. */
@Composable
private fun StepDots(current: ImportStep, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ImportStep.DOTS is emitted start-to-end and Row mirrors that in an RTL layout, so the
        // first step sits on the right where an Arabic reader starts (PLAN.md §8 row 16).
        ImportStep.DOTS.forEach { step ->
            val selected = step == current
            val width by animateDpAsState(
                targetValue = if (selected) DOT_ACTIVE_WIDTH else DOT_WIDTH,
                animationSpec = AppMotion.spatial(),
                label = "stepDotWidth",
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(DOT_HEIGHT)
                    .width(width)
                    .clip(CircleShape)
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
            )
        }
    }
}

/** The error report's sheet name and column captions, localized in the screen's locale. */
@Composable
private fun errorReportLabels(state: ImportUiState): ErrorReportLabels {
    val result = state.result
    return ErrorReportLabels(
        sheetName = stringResource(R.string.error_report),
        sourceRowColumn = stringResource(R.string.error_report_row),
        columnColumn = stringResource(R.string.error_report_column),
        rawValueColumn = stringResource(R.string.error_report_value),
        reasonColumn = stringResource(R.string.error_report_reason),
        truncationNote = if (result?.errorsTruncated == true) {
            stringResource(R.string.error_report_truncated, result.errors.size)
        } else {
            null
        },
    )
}

private const val REPORT_EXTENSION = ".xlsx"
private val DOT_WIDTH = 8.dp
private val DOT_ACTIVE_WIDTH = 24.dp
private val DOT_HEIGHT = 8.dp
