package com.gov.employeedb.core

/** Which light/dark colour scheme the app renders in; [SYSTEM] follows the device setting. */
enum class AppThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
    ;

    companion object {
        /** Used when the stored value is missing or unrecognised. */
        val DEFAULT: AppThemeMode = SYSTEM

        /**
         * Resolves a persisted value. `null` or an unknown name falls back to [DEFAULT] so a
         * downgrade (or a renamed constant) can never leave the app without a theme.
         */
        fun fromName(name: String?): AppThemeMode =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
