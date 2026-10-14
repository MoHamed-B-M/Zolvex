package com.gov.employeedb.presentation.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.BrightnessHigh
import androidx.compose.material.icons.outlined.BrightnessLow
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gov.employeedb.R
import com.gov.employeedb.core.AppLanguage
import com.gov.employeedb.core.AppThemeMode
import com.gov.employeedb.data.excel.ExcelMime
import com.gov.employeedb.data.excel.ExportFormat
import com.gov.employeedb.presentation.common.components.ScreenScaffold
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants

/**
 * The settings screen without its ViewModel (PLAN.md Phase-11 checklist): language picker, theme
 * picker, export all (XLSX/CSV), import replace, delete all (double confirm), about block.
 *
 * The four outcomes are branched: error shows retry, busy disables actions, otherwise the sections
 * render. The two confirm dialogs (delete 1, delete 2) are state-driven via [SettingsUiState.dialog].
 */
@Composable
fun SettingsContent(
    state: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onSetExportFormat: (ExportFormat) -> Unit,
    onExportAll: () -> Unit,
    onImportReplace: () -> Unit,
    onDeleteAllRequested: () -> Unit,
    onDeleteAllConfirmed: () -> Unit,
    onDialogDismissed: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        titleRes = R.string.settings,
        modifier = modifier,
        snackbarHost = { androidx.compose.material3.SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)

        when {
            state.error -> androidx.compose.foundation.layout.Column(
                modifier = contentModifier,
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                androidx.compose.material3.Text(
                    text = stringResource(R.string.error_generic),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.Modifier.height(16.dp))
                androidx.compose.material3.Button(onClick = onRetry) {
                    androidx.compose.material3.Text(text = stringResource(R.string.retry))
                }
            }
            else -> androidx.compose.foundation.lazy.LazyColumn(
                modifier = contentModifier,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp),
            ) {
                item(key = "language") { LanguageSection(state.language, onSetLanguage) }
                item(key = "theme") { ThemeSection(state.themeMode, onSetThemeMode) }
                item(key = "data") { DataSection(state, onSetExportFormat, onExportAll, onImportReplace, onDeleteAllRequested, onDeleteAllConfirmed) }
                item(key = "about") { AboutSection() }
            }
        }

        // Confirm dialogs
        if (state.hasDialog) {
            ConfirmDialog(
                dialog = state.dialog!!,
                onConfirm = { onDeleteAllConfirmed() },
                onDismiss = { onDialogDismissed() },
                employeeCount = state.employeeCount,
            )
        }
    }
}

/** Language: three FilterChips with native-name strings; selection writes DataStore instantly. */
@Composable
private fun LanguageSection(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
) {
    androidx.compose.material3.Card(modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier.padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.material3.Text(
                text = stringResource(R.string.language),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            androidx.compose.foundation.layout.Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                AppLanguage.entries.forEach { lang ->
                    androidx.compose.material3.FilterChip(
                        selected = lang == selected,
                        onClick = { onSelect(lang) },
                        modifier = androidx.compose.ui.Modifier.weight(1f),
                        label = {
                            androidx.compose.material3.Text(
                                text = stringResource(lang.nativeNameRes),
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        leadingIcon = {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Outlined.Language,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }
    }
}

/** Theme: Light / Dark / System as FilterChips. */
@Composable
private fun ThemeSection(
    selected: AppThemeMode,
    onSelect: (AppThemeMode) -> Unit,
) {
    androidx.compose.material3.Card(modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier.padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.material3.Text(
                text = stringResource(R.string.theme),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            androidx.compose.foundation.layout.Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                AppThemeMode.entries.forEach { mode ->
                    val (icon, label) = when (mode) {
                        AppThemeMode.LIGHT -> androidx.compose.material.icons.Icons.Outlined.BrightnessHigh to R.string.theme_light
                        AppThemeMode.DARK -> androidx.compose.material.icons.Icons.Outlined.BrightnessLow to R.string.theme_dark
                        AppThemeMode.SYSTEM -> androidx.compose.material.icons.Icons.Outlined.BrightnessAuto to R.string.theme_system
                    }
                    androidx.compose.material3.FilterChip(
                        selected = mode == selected,
                        onClick = { onSelect(mode) },
                        modifier = androidx.compose.ui.Modifier.weight(1f),
                        label = {
                            androidx.compose.material3.Text(
                                text = stringResource(label),
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        leadingIcon = {
                            androidx.compose.material3.Icon(
                                imageVector = icon,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }
    }
}

/** Data: Export format picker, Export all, Import replace, Delete all (double confirm). */
@Composable
private fun DataSection(
    state: SettingsUiState,
    onSetExportFormat: (ExportFormat) -> Unit,
    onExportAll: () -> Unit,
    onImportReplace: () -> Unit,
    onDeleteAllRequested: () -> Unit,
    onDeleteAllConfirmed: () -> Unit,
) {
    androidx.compose.material3.Card(modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier.padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            androidx.compose.material3.Text(
                text = stringResource(R.string.data),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Export format picker
            androidx.compose.foundation.layout.Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.FilterChip(
                    selected = state.exportFormat == ExportFormat.XLSX,
                    onClick = { onSetExportFormat(ExportFormat.XLSX) },
                    modifier = androidx.compose.ui.Modifier.weight(1f),
                    label = { androidx.compose.material3.Text(text = stringResource(R.string.format_xlsx)) },
                    leadingIcon = {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Outlined.FileDownload,
                            contentDescription = null
                        )
                    }
                )
                androidx.compose.material3.FilterChip(
                    selected = state.exportFormat == ExportFormat.CSV,
                    onClick = { onSetExportFormat(ExportFormat.CSV) },
                    modifier = androidx.compose.ui.Modifier.weight(1f),
                    label = { androidx.compose.material3.Text(text = stringResource(R.string.format_csv)) },
                    leadingIcon = {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Outlined.FileDownload,
                            contentDescription = null
                        )
                    }
                )
            }

            // Export all
            androidx.compose.material3.Button(
                onClick = onExportAll,
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                enabled = !state.isBusy,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    if (state.isBusy) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = androidx.compose.ui.Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Outlined.Download,
                            contentDescription = null
                        )
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                    androidx.compose.material3.Text(text = stringResource(R.string.export_all), style = MaterialTheme.typography.labelLarge)
                }
            }

            // Import replace
            androidx.compose.material3.Button(
                onClick = onImportReplace,
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                enabled = !state.isBusy,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Outlined.UploadFile,
                        contentDescription = null
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                    androidx.compose.material3.Text(text = stringResource(R.string.import_replace), style = MaterialTheme.typography.labelLarge)
                }
            }

            // Delete all
            androidx.compose.material3.Button(
                onClick = onDeleteAllRequested,
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                enabled = !state.isBusy && state.employeeCount > 0,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Outlined.DeleteForever,
                        contentDescription = null
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                    androidx.compose.material3.Text(text = stringResource(R.string.delete_all), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** About: name, version, licenses link, backup note. */
@Composable
private fun AboutSection() {
    androidx.compose.material3.Card(modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier.padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.material3.Text(text = stringResource(R.string.about), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            androidx.compose.material3.Text(text = stringResource(R.string.version, com.gov.employeedb.BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
            androidx.compose.material3.Text(text = stringResource(R.string.licenses), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
            androidx.compose.material3.Text(text = stringResource(R.string.backup_disabled_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

/** Two-step confirm dialog for delete-all and import-replace. */
@Composable
private fun ConfirmDialog(
    dialog: SettingsDialog,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    employeeCount: Int,
) {
    val (titleRes, messageRes, confirmText) = when (dialog) {
        is SettingsDialog.DeleteAll1 -> R.string.delete_all to R.string.delete_all_message to R.string.delete_all_confirm
        is SettingsDialog.DeleteAll2 -> R.string.delete_all to R.string.delete_all_message to R.string.delete_all_confirm
        is SettingsDialog.ImportReplace -> R.string.import_replace to R.string.import_replace_message to R.string.import_replace_confirm
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = { onConfirm() },
                modifier = androidx.compose.ui.Modifier.fillMaxWidth()
            ) {
                androidx.compose.material3.Text(text = stringResource(confirmText))
            }
        },
        dismissButton = {
            androidx.compose.material3.Button(
                onClick = onDismiss,
                modifier = androidx.compose.ui.Modifier.fillMaxWidth()
            ) {
                androidx.compose.material3.Text(text = stringResource(R.string.cancel))
            }
        },
        title = { androidx.compose.material3.Text(text = stringResource(titleRes), style = MaterialTheme.typography.titleMedium) },
        text = { androidx.compose.material3.Text(text = stringResource(messageRes, employeeCount), style = MaterialTheme.typography.bodyMedium) }
    )
}

@Preview(name = "SettingsContent")
@Composable
private fun SettingsContentPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        SettingsContent(
            state = SettingsUiState(
                language = AppLanguage.EN,
                themeMode = AppThemeMode.SYSTEM,
                employeeCount = 42,
                isBusy = false,
                exportFormat = ExportFormat.XLSX,
            ),
            snackbarHostState = androidx.compose.material3.SnackbarHostState(),
            onSetLanguage = {},
            onSetThemeMode = {},
            onSetExportFormat = {},
            onExportAll = {},
            onImportReplace = {},
            onDeleteAllRequested = {},
            onDeleteAllConfirmed = {},
            onDialogDismissed = {},
            onRetry = {},
        )
    }
}