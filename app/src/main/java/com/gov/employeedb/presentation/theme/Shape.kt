package com.gov.employeedb.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The Material 3 shape scale.
 *
 * All eight roles are passed explicitly (rather than relying on the five-parameter convenience
 * constructor) so a future Material 3 release adding a ninth role fails loudly at compile time
 * instead of silently inheriting a default.
 *
 * [extraLarge] is 28dp — the Material 3 specification value for large FABs, extended FABs and
 * dialogs, and the expressive-corner treatment this app uses for its cards and sheets.
 */
val AppShapes: Shapes = Shapes(
    extraSmall = ShapeDefaults.ExtraSmall,
    small = ShapeDefaults.Small,
    medium = ShapeDefaults.Medium,
    large = ShapeDefaults.Large,
    extraLarge = RoundedCornerShape(28.dp),
    largeIncreased = ShapeDefaults.LargeIncreased,
    extraLargeIncreased = ShapeDefaults.ExtraLargeIncreased,
    extraExtraLarge = ShapeDefaults.ExtraExtraLarge,
)
