package com.gov.employeedb.presentation.theme

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect

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

    /** The delay applied to section `index` when the dashboard animates its sections in. */
    private const val SECTION_STEP_MS = 60L

    /** Upper bound on the dashboard section stagger, so the last section starts promptly. */
    private const val MAX_SECTION_STEPS = 8

    /** How long one dashboard section takes to fade and slide in. */
    private const val SECTION_DURATION_MS = 400

    /** How long the donut's sweep takes, per slice. */
    private const val DONUT_SWEEP_MS = 700

    /** The extra delay each successive donut slice starts with. */
    private const val DONUT_SLICE_STEP_MS = 40L

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
     * The shortest effects spec in the motion scheme, for a transition that swaps whole screens
     * rather than parts of one: the analytics tabs slide and cross-fade with it, and a shorter curve
     * keeps a deliberate tab tap from feeling laggy on top of the navigation transition.
     *
     * Generic in the animated value so one spec drives both the horizontal offset (`IntOffset`) and
     * the two opacities (`Float`) of a single [androidx.compose.animation.ContentTransform].
     */
    @Composable
    @ReadOnlyComposable
    fun <T> fastEffects(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.fastEffectsSpec()

    /**
     * The [BoundsTransform] a shared-element transition animates its bounds with: the slow spatial
     * spring, so a surface that travels between two screens grows and settles on the curve a large
     * Material 3 surface uses, instead of the faster default.
     *
     * Takes no animation factory from the caller on purpose - a shared-element transition is one
     * movement, so both ends read the same spec here.
     */
    @Composable
    fun boundsTransform(): BoundsTransform {
        val scheme = MaterialTheme.motionScheme
        return remember(scheme) {
            object : BoundsTransform {
                override fun createAnimationSpec(
                    initialBounds: Rect,
                    targetBounds: Rect,
                ): FiniteAnimationSpec<Rect> = scheme.slowSpatialSpec()
            }
        }
    }

    /**
     * Stagger offset for the item at [index] in a list entrance animation, in milliseconds.
     *
     * Clamped so the last item of a long list starts at most [MAX_STAGGER_STEPS] steps in.
     */
    fun staggeredDelay(index: Int): Long =
        STAGGER_STEP_MS * index.coerceIn(0, MAX_STAGGER_STEPS)

    /**
     * The spec a dashboard section enters with: a [SECTION_DURATION_MS] tween delayed by one
     * [SECTION_STEP_MS] step per [index], clamped to [MAX_SECTION_STEPS] steps.
     *
     * A tween rather than a spring because a spring has no start delay, and the stagger is the whole
     * point of the effect. Generic in the animated value so the same spec drives both the opacity
     * (`Float`) and the vertical offset.
     */
    fun <T> sectionEntrySpec(index: Int): FiniteAnimationSpec<T> = tween(
        durationMillis = SECTION_DURATION_MS,
        delayMillis = (SECTION_STEP_MS * index.coerceIn(0, MAX_SECTION_STEPS)).toInt(),
    )

    /**
     * The spec one donut slice sweeps out with: a [DONUT_SWEEP_MS] tween delayed by one
     * [DONUT_SLICE_STEP_MS] step per [index], so the ring fills clockwise instead of all at once.
     */
    fun donutSweepSpec(index: Int): FiniteAnimationSpec<Float> = tween(
        durationMillis = DONUT_SWEEP_MS,
        delayMillis = (DONUT_SLICE_STEP_MS * index).toInt(),
    )
}
