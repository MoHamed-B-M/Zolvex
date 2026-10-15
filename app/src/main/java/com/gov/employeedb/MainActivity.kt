package com.gov.employeedb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gov.employeedb.presentation.main.AppViewModel
import com.gov.employeedb.presentation.navigation.AppNavigation
import com.gov.employeedb.presentation.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate so the splash view is installed into the window.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep the splash screen up until the persisted language and theme have been read, so no
        // frame is ever painted in the wrong language or in the wrong light/dark mode.
        splashScreen.setKeepOnScreenCondition { !appViewModel.uiState.value.isLoaded }
        enableEdgeToEdge()

        setContent {
            val uiState by appViewModel.uiState.collectAsStateWithLifecycle()
            AppTheme(uiState = uiState) {
                if (uiState.isLoaded) {
                    // Runs once, on the first loaded frame. Everything it starts is asynchronous, so
                    // the first frame is painted before any DataStore read or network call happens.
                    LaunchedEffect(Unit) {
                        appViewModel.onBoot()
                    }
                    AppNavigation()
                }
            }
        }
    }
}
