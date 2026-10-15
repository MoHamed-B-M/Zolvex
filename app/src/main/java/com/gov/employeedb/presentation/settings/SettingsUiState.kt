package com.gov.employeedb.presentation.settings

import androidx.compose.runtime.Immutable
import com.gov.employeedb.core.AppLanguage
import com.gov.employeedb.core.AppThemeMode
import com.gov.employeedb.data.excel.ExportFormat
import com.gov.employeedb.domain.model.UpdateCheckResult

/**
 * Everything the settings screen renders from (PLAN.md Phase-11 checklist).
 *
 * [language] and [themeMode] come from [AppViewModel] and are applied instantly via
 * [LocaleManager] and [MaterialTheme]. [employeeCount] and [isBusy] come from the data layer
 * (export/import use cases). [dialog] controls the confirm flows; it is set by the
 * ViewModel and cleared by the screen on dismiss.
 *
 * [exportFormat] persists the last-chosen format so the export button remembers it.
 */
@Immutable
data class SettingsUiState(
    val language: AppLanguage = AppLanguage.DEFAULT,
    val themeMode: AppThemeMode = AppThemeMode.DEFAULT,
    val employeeCount: Int = 0,
    val isBusy: Boolean = false,
    val exportFormat: ExportFormat = ExportFormat.XLSX,
    val dialog: SettingsDialog? = null,
    val error: Boolean = false,
    val updateState: UpdateCheckResult = UpdateCheckResult.Idle,
    val autoCheckEnabled: Boolean = true,
    val includePrereleases: Boolean = false,
    val lastCheckTime: Long? = null,
) {

    /** True when there is a dialog to show. */
    val hasDialog: Boolean = dialog != null
}

/** The confirm dialogs the settings screen can show. */
sealed interface SettingsDialog {
    /** "Delete all" – first tap opens this, second tap opens the message dialog. */
    object DeleteAll1 : SettingsDialog

    /** "Delete all" – second tap shows the final warning, third tap confirms. */
    object DeleteAll2 : SettingsDialog

    /** "Import (replaces all)" – first tap opens this, second tap confirms. */
    data class ImportReplace(val employeeCount: Int) : SettingsDialog
}

/** Everything the settings screen can ask for. */
sealed interface SettingsEvent {
    /** User chose a language; persists and applies instantly. */
    data class SetLanguage(val language: AppLanguage) : SettingsEvent

    /** User chose a theme mode; persists and applies instantly. */
    data class SetThemeMode(val themeMode: AppThemeMode) : SettingsEvent

    /** User chose an export format; persists for the next export. */
    data class SetExportFormat(val format: ExportFormat) : SettingsEvent

    /** User tapped "Export all employees"; writes the chosen format to SAF. */
    data class ExportAll(val uri: android.net.Uri, val format: ExportFormat) : SettingsEvent

    /** User confirmed "Import (replaces all)" - ViewModel deletes all, screen opens picker. */
    data object ImportReplaceConfirmed : SettingsEvent

    /** First confirmation of "Delete all". */
    data object DeleteAllRequested : SettingsEvent

    /** Second confirmation of "Delete all" (after the message dialog). */
    data object DeleteAllConfirmed : SettingsEvent

    /** User dismissed a dialog without confirming. */
    data object DialogDismissed : SettingsEvent

    /** User tapped "Check for updates"; forces a check that bypasses the cooldown. */
    data object CheckForUpdates : SettingsEvent

    /** User toggled "Auto-check for updates"; persists and schedules or cancels the worker. */
    data class SetAutoCheck(val enabled: Boolean) : SettingsEvent

    /** User toggled "Include pre-releases"; persists and re-runs a forced check. */
    data class SetIncludePrereleases(val include: Boolean) : SettingsEvent

    /** The error state's Retry. */
    data object Retry : SettingsEvent
}

/** One-shot things the settings screen has to say. */
sealed interface SettingsEffect {
    /** The workbook/report was written; the screen confirms it with a snackbar. */
    data object Exported : SettingsEffect

    /** The write failed; the screen reports it with a snackbar. */
    data object ExportFailed : SettingsEffect

    /** All data was deleted; the screen resets the employee count. */
    data object DataDeleted : SettingsEffect
}