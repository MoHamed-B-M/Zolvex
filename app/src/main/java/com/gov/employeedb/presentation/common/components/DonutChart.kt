package com.gov.employeedb.presentation.common.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.gov.employeedb.domain.model.AgeBucket
import com.gov.employeedb.domain.model.BucketCount
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants
import com.gov.employeedb.presentation.theme.AppMotion
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

/**
 * The colours the dashboard charts draw their slices and bars from, in the order they are assigned.
 *
 * A plain function rather than a composable, so a chart and the legend beside it can share one
 * palette without a second source of truth. The order is deliberately high-contrast first, so a
 * two-slice chart uses the colour scheme's `primary` and `tertiary` rather than two neighbours;
 * callers cycle it with `index % size`, so any number of slices gets a colour.
 */
internal fun chartPalette(scheme: ColorScheme): List<Color> = listOf(
    scheme.primary,
    scheme.tertiary,
    scheme.secondary,
    scheme.primaryContainer,
    scheme.tertiaryContainer,
    scheme.secondaryContainer,
)

/**
 * An animated donut with an optional total in the middle (PLAN.md Phase-9 checklist). Each slice
 * sweeps out from the top in turn, and a tap reports the slice it landed on through [onSliceClick].
 *
 * The whole drawing is wrapped in `LocalLayoutDirection provides Ltr`: a pie is not text, and letting
 * RTL flip the geometry would mirror the picture while the legend underneath stayed in reading order.
 *
 * Shown shares come from [BucketCount.share], which is `count / allRows`, so when some rows are
 * missing from the distribution (a null birth date, say) the ring honestly leaves a gap instead of
 * stretching the slices to fill it. The chart carries no labels of its own, which is why it has no
 * content description: the legend beside it is the accessible equivalent.
 *
 * `innerRadiusRatio = 0f` fills the ring into a pie, and [showTotal] = `false` then drops the centre
 * label: over a solid pie the total would sit on one slice's colour, where its contrast is whatever
 * that colour happens to be, and the legend underneath already carries every count.
 */
@Composable
fun DonutChart(
    buckets: List<BucketCount>,
    modifier: Modifier = Modifier,
    innerRadiusRatio: Float = 0.62f,
    showTotal: Boolean = true,
    onSliceClick: (BucketCount) -> Unit = {},
) {
    val palette = chartPalette(MaterialTheme.colorScheme)
    val currentOnSliceClick = rememberUpdatedState(onSliceClick)
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val slices = ArrayList<State<Float>>(buckets.size)
    buckets.forEachIndexed { index, bucket ->
        slices += animateFloatAsState(
            targetValue = if (LocalInspectionMode.current || entered) bucket.share else 0f,
            animationSpec = AppMotion.donutSweepSpec(index),
            label = "donutSlice$index",
        )
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier.pointerInput(buckets, innerRadiusRatio) {
                detectTapGestures { position ->
                    sliceAt(
                        position = position,
                        width = size.width.toFloat(),
                        height = size.height.toFloat(),
                        buckets = buckets,
                        innerRadiusRatio = innerRadiusRatio,
                    )?.let { currentOnSliceClick.value(it) }
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val outerRadius = size.minDimension / 2f
                val strokeWidth = outerRadius * (1f - innerRadiusRatio)
                val arcDiameter = 2f * (outerRadius - strokeWidth / 2f)
                val arcSize = Size(arcDiameter, arcDiameter)
                val topLeft = Offset(
                    x = (size.width - arcDiameter) / 2f,
                    y = (size.height - arcDiameter) / 2f,
                )
                var startAngle = START_ANGLE
                slices.forEachIndexed { index, sweep ->
                    val sweepAngle = sweep.value * FULL_CIRCLE
                    drawArc(
                        color = palette[index % palette.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth),
                    )
                    startAngle += sweepAngle
                }
            }
            if (showTotal) {
                Text(
                    text = buckets.sumOf { it.count }.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/**
 * The slice [position] falls inside, or `null` for the hole, the outside, or a gap left by shares
 * that do not add up to one. The angles mirror the drawing: measured clockwise from the top.
 */
private fun sliceAt(
    position: Offset,
    width: Float,
    height: Float,
    buckets: List<BucketCount>,
    innerRadiusRatio: Float,
): BucketCount? {
    if (buckets.isEmpty() || width <= 0f || height <= 0f) return null
    val outerRadius = min(width, height) / 2f
    val dx = position.x - width / 2f
    val dy = position.y - height / 2f
    val distance = hypot(dx, dy)
    if (distance < outerRadius * innerRadiusRatio || distance > outerRadius) return null
    var degrees = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() - START_ANGLE
    if (degrees < 0f) degrees += FULL_CIRCLE
    var cumulative = 0f
    for (bucket in buckets) {
        cumulative += bucket.share * FULL_CIRCLE
        if (degrees < cumulative) return bucket
    }
    return null
}

/** Where the first slice starts, in degrees: the top of the circle. */
private const val START_ANGLE = -90f

private const val FULL_CIRCLE = 360f

@Preview(name = "DonutChart")
@Composable
private fun DonutChartPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        DonutChart(
            buckets = previewBuckets,
            modifier = Modifier.size(200.dp),
        )
    }
}

private val previewBuckets: List<BucketCount> = listOf(
    BucketCount(AgeBucket.UNDER_25.name, 12, 0.12f),
    BucketCount(AgeBucket.B25_34.name, 34, 0.34f),
    BucketCount(AgeBucket.B35_44.name, 28, 0.28f),
    BucketCount(AgeBucket.B45_54.name, 18, 0.18f),
    BucketCount(AgeBucket.B55_PLUS.name, 8, 0.08f),
)
