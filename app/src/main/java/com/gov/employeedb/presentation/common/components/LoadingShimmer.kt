package com.gov.employeedb.presentation.common.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants
import com.gov.employeedb.presentation.theme.AppMotion

/**
 * Placeholder rows shown while the first list page loads (PLAN.md Phase-6 checklist): a linear
 * gradient whose highlight band sweeps across the blocks (Phase-12 §8 row 25), so a slow first load
 * reads as work in progress rather than as an empty screen.
 */
@Composable
fun LoadingShimmer(modifier: Modifier = Modifier, rows: Int = 6) {
    val transition = rememberInfiniteTransition(label = "loadingShimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = AppMotion.sweep(),
        label = "loadingShimmerSweep",
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val span = with(LocalDensity.current) { SHIMMER_SPAN.toPx() }
    // One brush for every block, so the highlight bands line up across a row. The band starts a
    // full span before the row and travels twice that distance, which reads as a left-to-right
    // sweep in LTR and is mirrored for free by the RTL layout.
    val brush = Brush.linearGradient(
        colors = listOf(base.copy(alpha = 0.4f), base, base.copy(alpha = 0.4f)),
        start = Offset(x = progress * 2f * span - span, y = 0f),
        end = Offset(x = progress * 2f * span + span, y = 0f),
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(20.dp),
    ) {
        repeat(rows) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(
                    modifier = Modifier
                        .size(40.dp)
                        .background(brush, CircleShape),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(14.dp)
                            .background(brush, RoundedCornerShape(7.dp)),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(12.dp)
                            .background(brush, RoundedCornerShape(6.dp)),
                    )
                }
            }
        }
    }
}

private val SHIMMER_SPAN = 320.dp

@Preview(name = "LoadingShimmer")
@Composable
private fun LoadingShimmerPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        LoadingShimmer()
    }
}
