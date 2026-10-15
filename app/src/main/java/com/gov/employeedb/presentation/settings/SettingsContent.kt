package com.gov.employeedb.presentation.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.gov.employeedb.domain.model.UpdateCheckResult
import com.gov.employeedb.presentation.common.components.LabeledValue
import com.gov.employeedb.presentation.common.components.ScreenScaffold
import com.gov.employeedb.presentation.common.components.SectionCard
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants
import com.gov.employeedb.presentation.common.rememberDateTimeFormatter
import java.time.Instant
import java.time.ZoneId

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
    onCheckForUpdates: () -> Unit,
    onSetAutoCheck: (Boolean) -> Unit,
    onSetIncludePrereleases: (Boolean) -> Unit,
    onViewRelease: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        titleRes = R.string.settings,
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)

        when {
            state.error -> Column(
                modifier = contentModifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.error_generic),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry) { Text(text = stringResource(R.string.retry)) }
            }
            else -> LazyColumn(
                modifier = contentModifier,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item(key = "language") { LanguageSection(state.language, onSetLanguage) }
                item(key = "theme") { ThemeSection(state.themeMode, onSetThemeMode) }
                item(key = "data") { DataSection(state, onSetExportFormat, onExportAll, onImportReplace, onDeleteAllRequested, onDeleteAllConfirmed) }
                item(key = "updates") {
                    UpdatesSection(
                        updateState = state.updateState,
                        autoCheckEnabled = state.autoCheckEnabled,
                        includePrereleases = state.includePrereleases,
                        lastCheckTime = state.lastCheckTime,
                        onCheckForUpdates = onCheckForUpdates,
                        onSetAutoCheck = onSetAutoCheck,
                        onSetIncludePrereleases = onSetIncludePrereleases,
                        onViewRelease = onViewRelease,
                    )
                }
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.language),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppLanguage.entries.forEach { lang ->
                    FilterChip(
                        selected = lang == selected,
                        onClick = { onSelect(lang) },
                        modifier = Modifier.weight(1f),
                        label = { Text(text = stringResource(lang.nativeNameRes), style = MaterialTheme.typography.labelLarge) },
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Language, contentDescription = null) },
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = stringResource(R.string.theme), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppThemeMode.entries.forEach { mode ->
                    val (icon, label) = when (mode) {
                        AppThemeMode.LIGHT -> Icons.Outlined.BrightnessHigh to R.string.theme_light
                        AppThemeMode.DARK -> Icons.Outlined.BrightnessLow to R.string.theme_dark
                        AppThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto to R.string.theme_system
                    }
                    FilterChip(
                        selected = mode == selected,
                        onClick = { onSelect(mode) },
                        modifier = Modifier.weight(1f),
                        label = { Text(text = stringResource(label), style = MaterialTheme.typography.labelLarge) },
                        leadingIcon = { Icon(imageVector = icon, contentDescription = null) },
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = stringResource(R.string.data), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)

            // Export format picker
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.exportFormat == ExportFormat.XLSX,
                    onClick = { onSetExportFormat(ExportFormat.XLSX) },
                    modifier = Modifier.weight(1f),
                    label = { Text(text = stringResource(R.string.format_xlsx)) },
                    leadingIcon = { Icon(imageVector = Icons.Outlined.FileDownload, contentDescription = null) },
                )
                FilterChip(
                    selected = state.exportFormat == ExportFormat.CSV,
                    onClick = { onSetExportFormat(ExportFormat.CSV) },
                    modifier = Modifier.weight(1f),
                    label = { Text(text = stringResource(R.string.format_csv)) },
                    leadingIcon = { Icon(imageVector = Icons.Outlined.FileDownload, contentDescription = null) },
                )
            }

            // Export all
            Button(
                onClick = onExportAll,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    if (state.isBusy) {
                        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.Outlined.Download, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.export_all), style = MaterialTheme.typography.labelLarge)
                }
            }

            // Import replace
            Button(
                onClick = onImportReplace,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.import_replace), style = MaterialTheme.typography.labelLarge)
                }
            }

            // Delete all
            Button(
                onClick = onDeleteAllRequested,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isBusy && state.employeeCount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.delete_all), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/**
 * Updates: the current check status, when the last check ran, a manual check button, the two
 * toggles, and (only when a newer release exists) a link to its release page.
 *
 * The status text and the release link both use start/end-agnostic layout, so the section mirrors
 * correctly under RTL.
 */
@Composable
private fun UpdatesSection(
    updateState: UpdateCheckResult,
    autoCheckEnabled: Boolean,
    includePrereleases: Boolean,
    lastCheckTime: Long?,
    onCheckForUpdates: () -> Unit,
    onSetAutoCheck: (Boolean) -> Unit,
    onSetIncludePrereleases: (Boolean) -> Unit,
    onViewRelease: (String) -> Unit,
) {
    val dateTimeFormatter = rememberDateTimeFormatter()
    val isChecking = updateState is UpdateCheckResult.Checking

    SectionCard(titleRes = R.string.settings_updates_title) {
        val statusText = when (updateState) {
            UpdateCheckResult.Idle -> stringResource(R.string.update_status_idle)
            UpdateCheckResult.Checking -> stringResource(R.string.update_status_checking)
            UpdateCheckResult.UpToDate -> stringResource(R.string.update_status_uptodate)
            is UpdateCheckResult.UpdateAvailable ->
                stringResource(R.string.update_status_available, updateState.info.tagName)
            is UpdateCheckResult.Error -> stringResource(R.string.update_status_error)
        }
        LabeledValue(label = stringResource(R.string.update_status_label), value = statusText)

        if (lastCheckTime != null) {
            val formatted = remember(lastCheckTime, dateTimeFormatter) {
                dateTimeFormatter.format(
                    Instant.ofEpochMilli(lastCheckTime).atZone(ZoneId.systemDefault()),
                )
            }
            LabeledValue(label = stringResource(R.string.update_last_check), value = formatted)
        }

        Button(
            onClick = onCheckForUpdates,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isChecking,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.update_check_now), style = MaterialTheme.typography.labelLarge)
            }
        }

        val available = updateState as? UpdateCheckResult.UpdateAvailable
        if (available != null) {
            TextButton(
                onClick = { onViewRelease(available.info.htmlUrl) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.update_view_release), style = MaterialTheme.typography.labelLarge)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.update_auto_check),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = autoCheckEnabled, onCheckedChange = onSetAutoCheck)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.update_include_prereleases),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = includePrereleases, onCheckedChange = onSetIncludePrereleases)
        }
    }
}

/** About: name, version, licenses link, backup note. */
@Composable
private fun AboutSection() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = stringResource(R.string.about), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = stringResource(R.string.version, com.gov.employeedb.BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.licenses), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.backup_disabled_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
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
    val triple = when (dialog) {
        is SettingsDialog.DeleteAll1 -> Triple(R.string.delete_all, R.string.delete_all_message, R.string.delete_all_confirm)
        is SettingsDialog.DeleteAll2 -> Triple(R.string.delete_all, R.string.delete_all_message, R.string.delete_all_confirm)
        is SettingsDialog.ImportReplace -> Triple(R.string.import_replace, R.string.import_replace_message, R.string.import_replace_confirm)
    }
    val (titleRes, messageRes, confirmText) = triple

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onConfirm() },
                modifier = Modifier.fillMaxWidth()
            ) { Text(text = stringResource(confirmText)) }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) { Text(text = stringResource(R.string.cancel)) }
        },
        title = { Text(text = stringResource(titleRes), style = MaterialTheme.typography.titleMedium) },
        text = { Text(text = stringResource(messageRes, employeeCount), style = MaterialTheme.typography.bodyMedium) }
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
            snackbarHostState = SnackbarHostState(),
            onSetLanguage = {},
            onSetThemeMode = {},
            onSetExportFormat = {},
            onExportAll = {},
            onImportReplace = {},
            onDeleteAllRequested = {},
            onDeleteAllConfirmed = {},
            onDialogDismissed = {},
            onCheckForUpdates = {},
            onSetAutoCheck = {},
            onSetIncludePrereleases = {},
            onViewRelease = {},
            onRetry = {},
        )
    }
}