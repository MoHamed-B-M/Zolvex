package com.gov.employeedb.presentation.settings

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
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants

/**
 * Settings: language, theme, export/import/delete-all with SAF, and the About block (PLAN.md Phase-11).
 *
 * This is the stateful half of the screen. It owns the four things [SettingsContent] cannot: the
 * Hilt view model, the SAF `CreateDocument` launcher, the SAF `OpenDocument` launcher, and the
 * snackbar that confirms exports/imports/deletes.
 *
 * The export format ([ExcelMime.ExportFormat]) is chosen in the UI and passed to the export use case.
 * The import picks `.xlsx` or `.csv` from the SAF picker.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val createDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ExcelMime.OPEN_TYPES),
    ) { uri ->
        if (uri != null) {
            viewModel.onEvent(SettingsEvent.ExportAll(uri, state.exportFormat))
        }
    }
    val exportedLabel = stringResource(R.string.export_all)
    val failedLabel = stringResource(R.string.error_generic)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            val message = when (effect) {
                SettingsEffect.Exported -> exportedLabel
                SettingsEffect.ExportFailed -> failedLabel
                SettingsEffect.ImportStarted -> stringResource(R.string.import_replace)
                SettingsEffect.ImportFailed -> failedLabel
                SettingsEffect.DataDeleted -> stringResource(R.string.delete_all)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    SettingsContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onSetLanguage = { viewModel.onEvent(SettingsEvent.SetLanguage(it)) },
        onSetThemeMode = { viewModel.onEvent(SettingsEvent.SetThemeMode(it)) },
        onSetExportFormat = { viewModel.onEvent(SettingsEvent.SetExportFormat(it)) },
        onExportAll = { createDocument.launch(ExcelMime.mimeForFormat(state.exportFormat)) },
        onImportReplace = { viewModel.onEvent(SettingsEvent.ImportReplace(it)) },
        onDeleteAllRequested = { viewModel.onEvent(SettingsEvent.DeleteAllRequested) },
        onDeleteAllConfirmed = { viewModel.onEvent(SettingsEvent.DeleteAllConfirmed) },
        onDialogDismissed = { viewModel.onEvent(SettingsEvent.DialogDismissed) },
        onRetry = { viewModel.onEvent(SettingsEvent.Retry) },
        modifier = modifier,
    )
}

@Preview(name = "SettingsScreen")
@Composable
private fun SettingsScreenPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        SettingsScreen()
    }
}