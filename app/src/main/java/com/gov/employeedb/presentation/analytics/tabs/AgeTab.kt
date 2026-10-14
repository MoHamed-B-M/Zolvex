package com.gov.employeedb.presentation.analytics.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.gov.employeedb.R
import com.gov.employeedb.domain.model.AgeBucket
import com.gov.employeedb.domain.model.AnalyticsSnapshot
import com.gov.employeedb.domain.model.BucketCount
import com.gov.employeedb.presentation.common.components.BucketLegend
import com.gov.employeedb.presentation.common.components.DonutChart
import com.gov.employeedb.presentation.common.components.SectionCard
import com.gov.employeedb.presentation.common.components.StatCard
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants

/**
 * The Age tab (PLAN.md Phase-10 checklist): the age distribution as an animated donut, its legend,
 * the average and the median as statistic cards, and the two ends of the range as chips.
 *
 * The chips are shown only when there is a range to name: [AnalyticsSnapshot.youngestAge] and
 * [AnalyticsSnapshot.oldestAge] are null exactly when the average and the median are, which is when
 * no row has a birth date at all.
 */
@Composable
fun AgeTab(snapshot: AnalyticsSnapshot, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "statistics") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StatCard(
                    icon = Icons.Outlined.CalendarMonth,
                    labelRes = R.string.average_age,
                    value = snapshot.averageAge,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    icon = Icons.Outlined.Timeline,
                    labelRes = R.string.median_age,
                    value = snapshot.medianAge,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        val youngest = snapshot.youngestAge
        val oldest = snapshot.oldestAge
        if (youngest != null && oldest != null) {
            item(key = "range") { AgeRangeChips(youngest = youngest, oldest = oldest) }
        }
        item(key = "distribution") {
            SectionCard(titleRes = R.string.analytics_age) {
                DonutChart(
                    buckets = snapshot.ageBuckets,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(DONUT_SIZE),
                )
                BucketLegend(buckets = snapshot.ageBuckets)
            }
        }
    }
}

/** The youngest and oldest ages, as the same non-interactive chips an employee card shows. */
@Composable
private fun AgeRangeChips(youngest: Int, oldest: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AssistChip(
            onClick = {},
            label = { Text(text = "${stringResource(R.string.youngest)} $youngest") },
        )
        AssistChip(
            onClick = {},
            label = { Text(text = "${stringResource(R.string.oldest)} $oldest") },
        )
    }
}

private val DONUT_SIZE = 200.dp

@Preview(name = "AgeTab")
@Composable
private fun AgeTabPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        AgeTab(snapshot = previewSnapshot)
    }
}

private val previewSnapshot = AnalyticsSnapshot(
    total = 120,
    averageAge = 41.2,
    medianAge = 40.0,
    youngestAge = 22,
    oldestAge = 63,
    averageService = 9.4,
    medianService = 8.0,
    retirementSoon = 7,
    ageBuckets = listOf(
        BucketCount(AgeBucket.UNDER_25.name, 6, 0.05f),
        BucketCount(AgeBucket.B25_34.name, 34, 0.28f),
        BucketCount(AgeBucket.B35_44.name, 42, 0.35f),
        BucketCount(AgeBucket.B45_54.name, 28, 0.23f),
        BucketCount(AgeBucket.B55_PLUS.name, 10, 0.09f),
    ),
    serviceBuckets = emptyList(),
    departmentCounts = emptyList(),
    gradeCounts = emptyList(),
    longestServing = emptyList(),
)
