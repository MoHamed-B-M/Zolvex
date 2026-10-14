package com.gov.employeedb.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.gov.employeedb.core.NumberFormatters
import com.gov.employeedb.domain.model.AgeBucket
import com.gov.employeedb.domain.model.BucketCount
import com.gov.employeedb.presentation.common.bucketLabel
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants

/**
 * The legend under a dashboard chart (PLAN.md Phase-9 checklist): one row per bucket, with a colour
 * dot, the localized label and the count plus share.
 *
 * Because the charts draw no labels, this is their accessible equivalent: every slice's meaning is
 * spelled out, and nothing is conveyed by colour alone. [colors] is cycled with `index % size`, so a
 * one-colour legend (the bar chart) works as well as the donut's six, and it defaults to the donut's
 * palette so the common case stays in step with the chart above it.
 */
@Composable
fun BucketLegend(
    buckets: List<BucketCount>,
    modifier: Modifier = Modifier,
    colors: List<Color> = chartPalette(MaterialTheme.colorScheme),
) {
    val context = LocalContext.current
    val labels = remember(buckets, context) { buckets.map { bucketLabel(context, it.label) } }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        buckets.forEachIndexed { index, bucket ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (colors.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(DOT_SIZE)
                            .background(color = colors[index % colors.size], shape = CircleShape),
                    )
                }
                Text(
                    text = labels[index],
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${bucket.count}$SEPARATOR${NumberFormatters.percent(bucket.share)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

private val DOT_SIZE = 10.dp

/** The same separator the employee cards join their subtitle with, so the app stays visually one. */
private const val SEPARATOR = " \u00B7 "

@Preview(name = "BucketLegend")
@Composable
private fun BucketLegendPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        BucketLegend(buckets = previewBuckets)
    }
}

private val previewBuckets: List<BucketCount> = listOf(
    BucketCount(AgeBucket.UNDER_25.name, 12, 0.12f),
    BucketCount(AgeBucket.B25_34.name, 34, 0.34f),
    BucketCount(AgeBucket.B35_44.name, 28, 0.28f),
    BucketCount(AgeBucket.B45_54.name, 18, 0.18f),
    BucketCount(AgeBucket.B55_PLUS.name, 8, 0.08f),
)
