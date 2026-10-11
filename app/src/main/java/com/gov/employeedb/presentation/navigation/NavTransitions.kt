package com.gov.employeedb.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import com.gov.employeedb.presentation.theme.AppMotion

/**
 * One move of a navigation transition: the incoming screen fades in from a slightly smaller size,
 * the outgoing one simply fades away. Going back plays it the other way round, so the screen being
 * left shrinks away as the previous one returns.
 *
 * The four lambdas are held as values rather than as a composable function because `NavHost`
 * takes non-composable transition callbacks: the specs are read once, in composition, and captured
 * here (see [rememberNavTransitions]).
 */
@Immutable
class NavTransitions internal constructor(
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition,
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition,
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition,
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition,
)

/** How far the incoming screen has to grow (or the outgoing one has to shrink) to reach its size. */
private const val SCALE_FROM = 0.92f

/**
 * Builds the app's navigation transitions from the active Material 3 motion scheme.
 *
 * `MaterialTheme.motionScheme` is read here, in composition, because the four transitions `NavHost`
 * wants are plain (non-composable) lambdas. No duration is written down: the spatial spec is a
 * spring and the effects spec is a short curve, both supplied by the expressive motion scheme that
 * [com.gov.employeedb.presentation.theme.AppTheme] installs, so the system animation scale and any
 * future change of motion scheme apply to navigation too (PLAN.md §8).
 */
@Composable
fun rememberNavTransitions(): NavTransitions {
    val spatial = AppMotion.spatial<Float>()
    val effects = AppMotion.effects<Float>()
    return remember(spatial, effects) {
        NavTransitions(
            enter = {
                fadeIn(animationSpec = effects) +
                    scaleIn(initialScale = SCALE_FROM, animationSpec = spatial)
            },
            exit = { fadeOut(animationSpec = effects) },
            popEnter = { fadeIn(animationSpec = effects) },
            popExit = {
                fadeOut(animationSpec = effects) +
                    scaleOut(targetScale = SCALE_FROM, animationSpec = spatial)
            },
        )
    }
}
