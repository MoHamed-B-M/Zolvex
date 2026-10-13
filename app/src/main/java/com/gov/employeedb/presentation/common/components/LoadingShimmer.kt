package com.gov.employeedb.presentation.common.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants

/**
 * Placeholder rows shown while the first list page loads (PLAN.md Phase-6 checklist). They pulse
 * their opacity rather than animating a moving gradient, so the effect costs one property animation
 * and no extra layers.
 */
@Composable
fun LoadingShimmer(modifier: Modifier = Modifier, rows: Int = 6) {
    val transition = rememberInfiniteTransition(label = "employeeShimmer")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "employeeShimmerAlpha",
    )
    val blockColor = MaterialTheme.colorScheme.surfaceVariant
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
                        .alpha(pulse)
                        .background(blockColor, CircleShape),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(14.dp)
                            .alpha(pulse)
                            .background(blockColor, RoundedCornerShape(7.dp)),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(12.dp)
                            .alpha(pulse)
                            .background(blockColor, RoundedCornerShape(6.dp)),
                    )
                }
            }
        }
    }
}

@Preview(name = "LoadingShimmer")
@Composable
private fun LoadingShimmerPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        LoadingShimmer()
    }
}
