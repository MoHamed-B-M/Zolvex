package com.gov.employeedb.presentation.employees.edit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gov.employeedb.R
import com.gov.employeedb.presentation.common.components.AnimatedCheckmark
import com.gov.employeedb.presentation.common.components.AppTopBar
import com.gov.employeedb.presentation.common.components.EmptyState
import com.gov.employeedb.presentation.common.components.ErrorState
import com.gov.employeedb.presentation.common.components.LoadingShimmer
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The form's shake: ±8 dp, three swings, 300 ms (PLAN.md Phase-8 checklist). */
private const val SHAKE_AMPLITUDE_DP = 8f
private const val SHAKE_DURATION_MS = 300

/** How long the success mark stays on screen before the form pops itself. */
private const val SAVED_ANIMATION_MS = 900L

/**
 * The add/edit form (PLAN.md Phase-8 checklist).
 *
 * The screen owns only view concerns - the Snackbar host, the success overlay and the shake
 * animation - while the form, its errors and the save itself live in [EmployeeEditViewModel]. The
 * route decides create vs edit, so this screen never has to know about the sentinel id.
 */
@Composable
fun EmployeeEditScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmployeeEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = stringResource(R.string.error_generic)
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                EmployeeEditEffect.Saved -> {
                    // Hold long enough for the check to finish drawing, then leave the form.
                    delay(SAVED_ANIMATION_MS)
                    onNavigateUp()
                }

                EmployeeEditEffect.SaveFailed -> snackbarHostState.showSnackbar(errorMessage)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                titleRes = if (state.isEditing) R.string.edit_employee else R.string.add_employee,
                onNavigateUp = onNavigateUp,
            )
        },
        bottomBar = {
            SaveBar(
                canSave = state.canSave,
                isSaving = state.isSaving,
                shakeOffset = shake.value,
                onSave = {
                    if (!state.isValid) {
                        scope.launch { shake.shake() }
                    }
                    viewModel.onEvent(EmployeeEditEvent.Save)
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.systemBars.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.error -> ErrorState(
                    onRetry = { viewModel.onEvent(EmployeeEditEvent.Retry) },
                    modifier = Modifier.fillMaxSize(),
                )

                state.notFound -> EmptyState(
                    icon = Icons.Outlined.People,
                    headlineRes = R.string.no_data,
                    modifier = Modifier.fillMaxSize(),
                )

                state.isLoading -> LoadingShimmer(modifier = Modifier.fillMaxWidth())

                else -> EmployeeEditContent(
                    state = state,
                    onEvent = viewModel::onEvent,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (state.isSaved) {
                SaveOverlay(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

/** The bottom bar: one full-width Save button, shaken in place when a save is rejected. */
@Composable
private fun SaveBar(
    canSave: Boolean,
    isSaving: Boolean,
    shakeOffset: Float,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, tonalElevation = 3.dp) {
        Button(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .offset { IntOffset(shakeOffset.roundToInt(), 0) },
        ) {
            Text(text = stringResource(if (isSaving) R.string.saving else R.string.save))
        }
    }
}

/** Dims the form and draws the success mark while the screen is on its way back. */
@Composable
private fun SaveOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            MaterialTheme.colorScheme.scrim.copy(alpha = OVERLAY_SCRIM_ALPHA),
        ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedCheckmark()
    }
}

/** A single swing of the shake curve, oscillating around zero. */
private suspend fun Animatable<Float, AnimationVector1D>.shake() {
    snapTo(0f)
    animateTo(
        targetValue = 0f,
        animationSpec = keyframes {
            durationMillis = SHAKE_DURATION_MS
            -SHAKE_AMPLITUDE_DP at 0
            SHAKE_AMPLITUDE_DP at 50
            -SHAKE_AMPLITUDE_DP at 100
            SHAKE_AMPLITUDE_DP at 150
            -SHAKE_AMPLITUDE_DP at 200
            -SHAKE_AMPLITUDE_DP / 2f at 250
        },
    )
}

private const val OVERLAY_SCRIM_ALPHA = 0.32f
