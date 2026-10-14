package com.gov.employeedb.presentation.common.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.gov.employeedb.R
import com.gov.employeedb.domain.model.BucketCount
import com.gov.employeedb.domain.model.ServiceBucket
import com.gov.employeedb.presentation.common.bucketLabel
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.CartesianChartHost
import com.patrykandpatrick.vico.compose.chart.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.chart.rememberCartesianChart
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.model.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.model.columnSeries
import kotlinx.coroutines.delay

/**
 * A titled card holding a Vico column chart and its legend (PLAN.md Phase-9 checklist).
 *
 * The card is the service-years section, so the bars use a single colour: Vico indexes columns by
 * series, not by entry, so giving each bar its own colour would spread one bar per series across the
 * chart instead of drawing five bars side by side. The legend beneath tells the buckets apart.
 */
@Composable
fun BarChartCard(
    buckets: List<BucketCount>,
    modifier: Modifier = Modifier,
) {
    SectionCard(titleRes = R.string.analytics_service, modifier = modifier) {
        BarChart(buckets = buckets)
        BucketLegend(
            buckets = buckets,
            colors = listOf(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun BarChart(buckets: List<BucketCount>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val labels = remember(buckets, context) { buckets.map { bucketLabel(context, it.label) } }
    val producer = remember { CartesianChartModelProducer.build() }
    LaunchedEffect(buckets) {
        if (buckets.isEmpty()) return@LaunchedEffect
        delay(BAR_ENTRY_DELAY_MS)
        producer.runTransaction {
            columnSeries { series(y = buckets.map { it.count }) }
        }
    }
    // The layers and axes take their colours from `currentChartStyle` when they are built, so the
    // chart is created inside the provider: outside it they would fall back to Vico's own palette and
    // the bars would not match the legend dot.
    ProvideChartStyle(chartStyle = m3ChartStyle()) {
        val chart = rememberCartesianChart(
            rememberColumnCartesianLayer(),
            startAxis = rememberStartAxis(),
            bottomAxis = rememberBottomAxis(
                valueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _, _ ->
                    labels.getOrElse(value.toInt()) { "" }
                },
            ),
        )
        // Bars are read left to right even in Arabic; the localised labels travel with them.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            CartesianChartHost(
                chart = chart,
                modelProducer = producer,
                modifier = modifier
                    .fillMaxWidth()
                    .height(CHART_HEIGHT),
            )
        }
    }
}

private val CHART_HEIGHT = 200.dp

/** One frame of breathing room, so the chart is measured before its first transaction lands. */
private const val BAR_ENTRY_DELAY_MS = 60L

@Preview(name = "BarChartCard")
@Composable
private fun BarChartCardPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        BarChartCard(buckets = previewBuckets)
    }
}

private val previewBuckets: List<BucketCount> = listOf(
    BucketCount(ServiceBucket.LESS_5.name, 22, 0.22f),
    BucketCount(ServiceBucket.B5_10.name, 31, 0.31f),
    BucketCount(ServiceBucket.B10_15.name, 26, 0.26f),
    BucketCount(ServiceBucket.B15_20.name, 15, 0.15f),
    BucketCount(ServiceBucket.B20_PLUS.name, 6, 0.06f),
)
