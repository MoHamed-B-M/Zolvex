package com.gov.employeedb.presentation.analytics.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.gov.employeedb.R
import com.gov.employeedb.domain.model.AnalyticsSnapshot
import com.gov.employeedb.domain.model.BucketCount
import com.gov.employeedb.presentation.common.components.BucketLegend
import com.gov.employeedb.presentation.common.components.DonutChart
import com.gov.employeedb.presentation.common.components.SectionCard
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants

/**
 * The Grade tab (PLAN.md Phase-10 checklist): the same animated chart as the dashboard's age donut,
 * filled in as a pie (`innerRadiusRatio = 0f`) because a grade distribution reads as parts of one
 * whole rather than as a ring, with the legend beneath carrying every count.
 *
 * `showTotal = false` drops the centre label the donut shows: on a solid pie the total would sit on
 * one grade's colour, where its contrast is whatever that colour happens to be.
 */
@Composable
fun GradeTab(snapshot: AnalyticsSnapshot, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "distribution") {
            SectionCard(titleRes = R.string.analytics_grade) {
                DonutChart(
                    buckets = snapshot.gradeCounts,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(PIE_SIZE),
                    innerRadiusRatio = 0f,
                    showTotal = false,
                )
                BucketLegend(buckets = snapshot.gradeCounts)
            }
        }
    }
}

private val PIE_SIZE = 220.dp

@Preview(name = "GradeTab")
@Composable
private fun GradeTabPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        GradeTab(snapshot = previewSnapshot)
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
    ageBuckets = emptyList(),
    serviceBuckets = emptyList(),
    departmentCounts = emptyList(),
    gradeCounts = listOf(
        BucketCount("A", 42, 0.35f),
        BucketCount("B", 51, 0.43f),
        BucketCount("C", 21, 0.17f),
        BucketCount("", 6, 0.05f),
    ),
    longestServing = emptyList(),
)
