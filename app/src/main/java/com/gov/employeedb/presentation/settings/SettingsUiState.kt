package com.gov.employeedb.presentation.settings

import androidx.compose.runtime.Immutable
import com.gov.employeedb.core.AppLanguage
import com.gov.employeedb.core.AppThemeMode
import com.gov.employeedb.data.excel.ExcelMime

/**
 * Everything the settings screen renders from (PLAN.md Phase-11 checklist).
 *
 * [language] and [themeMode] come from [AppViewModel] and are applied instantly via
 * [LocaleManager] and [MaterialTheme]. [employeeCount] and [isBusy] come from the data layer
 * (export/import use cases). [dialog] controls the two-step confirm flows; it is set by the
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
    val exportFormat: ExcelMime.ExportFormat = ExcelMime.ExportFormat.XLSX,
    val dialog: SettingsDialog? = null,
    val error: Boolean = false,
) {

    /** True when there is a dialog to show. */
    val hasDialog: Boolean = dialog != null
}

/** The two-step confirm dialogs the settings screen can show. */
sealed interface SettingsDialog {
    /** "Import (replaces all)" – first tap opens this, second tap confirms. */
    data class ImportReplace(val employeeCount: Int) : SettingsDialog

    /** "Delete all" – first tap opens this, second tap opens the message dialog. */
    data class DeleteAll1 : SettingsDialog

    /** "Delete all" – second tap shows the final warning, third tap confirms. */
    data class DeleteAll2 : SettingsDialog
}

/** Everything the settings screen can ask for. */
sealed interface SettingsEvent {
    /** User chose a language; persists and applies instantly. */
    data class SetLanguage(val language: AppLanguage) : SettingsEvent

    /** User chose a theme mode; persists and applies instantly. */
    data class SetThemeMode(val themeMode: AppThemeMode) : SettingsEvent

    /** User chose an export format; persists for the next export. */
    data class SetExportFormat(val format: ExcelMime.ExportFormat) : SettingsEvent

    /** User tapped "Export all employees"; writes the chosen format to SAF. */
    data class ExportAll(val uri: android.net.Uri, val format: ExcelMime.ExportFormat) : SettingsEvent

    /** User tapped "Import (replaces all)"; SAF picker opens the file. */
    data class ImportReplace(val uri: android.net.Uri) : SettingsEvent

    /** First confirmation of "Delete all". */
    data object DeleteAllRequested : SettingsEvent

    /** Second confirmation of "Delete all" (after the message dialog). */
    data object DeleteAllConfirmed : SettingsEvent

    /** User dismissed a dialog without confirming. */
    data object DialogDismissed : SettingsEvent

    /** The error state's Retry. */
    data object Retry : SettingsEvent
}

/** One-shot things the settings screen has to say. */
sealed interface SettingsEffect {
    /** The workbook/report was written; the screen confirms it with a snackbar. */
    data object Exported : SettingsEffect

    /** The write failed; the screen reports it with a snackbar. */
    data object ExportFailed : SettingsEffect

    /** The import started; the screen navigates to the import wizard. */
    data object ImportStarted : SettingsEffect

    /** The import failed (read error, etc.). */
    data object ImportFailed : SettingsEffect

    /** All data was deleted; the screen resets the employee count. */
    data object DataDeleted : SettingsEffect
}