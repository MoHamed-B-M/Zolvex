package com.gov.employeedb.presentation.main

import androidx.compose.runtime.Immutable
import com.gov.employeedb.core.AppLanguage
import com.gov.employeedb.core.AppThemeMode

/**
 * The state the app shell renders from: the two persisted settings, plus the flag that holds the
 * splash screen in place until they have been read.
 *
 * [isLoaded] exists to make a cold start flash-free. DataStore is asynchronous, so the first
 * composition happens before the stored language and theme are known; the splash screen covers
 * exactly that window instead of letting the UI paint one frame in the wrong language or theme.
 */
@Immutable
data class AppUiState(
    val language: AppLanguage,
    val themeMode: AppThemeMode,
    val isLoaded: Boolean,
)
