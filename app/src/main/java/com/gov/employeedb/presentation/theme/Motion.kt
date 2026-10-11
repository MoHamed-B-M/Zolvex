package com.gov.employeedb.presentation.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * The single access point for animation timing.
 *
 * Every duration, easing curve and spring in the app is read from here, so no screen hardcodes
 * a `tween(...)` or a spring: the values come from the active Material 3 motion scheme
 * (`MotionScheme.expressive()`, installed by [AppTheme]) and can be themed in one place.
 */
object AppMotion {
    /** The delay applied to item `index` when a list animates in as a staggered cascade. */
    private const val STAGGER_STEP_MS = 40L

    /** Upper bound on the stagger multiplier, so a 1000-row list does not crawl in. */
    private const val MAX_STAGGER_STEPS = 12

    /**
     * Position, size and rotation changes: a spring that overshoots slightly and settles, which
     * reads as "physical" and matches the Material 3 expressive spatial spec.
     */
    @Composable
    @ReadOnlyComposable
    fun <T> spatial(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.defaultSpatialSpec()

    /**
     * Colour, alpha and elevation changes: a fast, curve-based spec with no overshoot, so
     * cross-fades never look bouncy.
     */
    @Composable
    @ReadOnlyComposable
    fun <T> effects(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.defaultEffectsSpec()

    /**
     * Stagger offset for the item at [index] in a list entrance animation, in milliseconds.
     *
     * Clamped so the last item of a long list starts at most [MAX_STAGGER_STEPS] steps in.
     */
    fun staggeredDelay(index: Int): Long =
        STAGGER_STEP_MS * index.coerceIn(0, MAX_STAGGER_STEPS)
}
