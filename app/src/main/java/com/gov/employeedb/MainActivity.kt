package com.gov.employeedb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gov.employeedb.presentation.main.AppViewModel
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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (uiState.isLoaded) {
                        // The navigation shell replaces this content in Phase 3. Until then the
                        // themed shell renders the app name, which is enough to see the design
                        // system on a device.
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineLarge,
                        )
                    }
                }
            }
        }
    }
}
