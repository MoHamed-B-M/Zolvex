package com.gov.employeedb.presentation.common.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants

/**
 * The success mark on the Result step (PLAN.md Phase-7 checklist): a ring sweeps first, then the
 * tick draws itself in, both from one 0→1 animation.
 *
 * The tick is not a dashed stroke: it is a [PathMeasure] segment whose end is the animated fraction
 * of the path's length, so the stroke tip stays perfectly round and the motion reads as handwriting
 * rather than as a wipe.
 */
@Composable
fun AnimatedCheckmark(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    strokeWidth: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = DURATION_MS, easing = LinearEasing),
        )
    }
    Canvas(modifier = modifier.size(size)) {
        val stroke = Stroke(
            width = strokeWidth.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val fraction = progress.value
        val ring = (fraction / RING_PORTION).coerceIn(0f, 1f)
        val tick = ((fraction - RING_PORTION) / (1f - RING_PORTION)).coerceIn(0f, 1f)
        val box = this.size
        val inset = strokeWidth.toPx() / 2f
        val diameter = box.minDimension - strokeWidth.toPx()

        drawArc(
            color = color,
            startAngle = START_ANGLE,
            sweepAngle = SWEEP * ring,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(diameter, diameter),
            style = stroke,
        )

        val tickPath = Path().apply {
            moveTo(box.width * TICK_START_X, box.height * TICK_START_Y)
            lineTo(box.width * TICK_MID_X, box.height * TICK_MID_Y)
            lineTo(box.width * TICK_END_X, box.height * TICK_END_Y)
        }
        val measure = PathMeasure().apply { setPath(tickPath, false) }
        val segment = Path()
        measure.getSegment(0f, measure.length * tick, segment, true)
        drawPath(path = segment, color = color, style = stroke)
    }
}

private const val DURATION_MS = 600
private const val RING_PORTION = 0.6f
private const val START_ANGLE = -90f
private const val SWEEP = 360f
private const val TICK_START_X = 0.28f
private const val TICK_START_Y = 0.52f
private const val TICK_MID_X = 0.45f
private const val TICK_MID_Y = 0.68f
private const val TICK_END_X = 0.74f
private const val TICK_END_Y = 0.36f

@Preview(name = "AnimatedCheckmark")
@Composable
private fun AnimatedCheckmarkPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        AnimatedCheckmark()
    }
}
