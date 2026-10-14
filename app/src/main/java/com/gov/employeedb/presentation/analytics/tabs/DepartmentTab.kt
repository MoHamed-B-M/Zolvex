package com.gov.employeedb.presentation.analytics.tabs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.gov.employeedb.R
import com.gov.employeedb.core.NumberFormatters
import com.gov.employeedb.domain.analytics.WorkforceAnalytics
import com.gov.employeedb.domain.model.AnalyticsSnapshot
import com.gov.employeedb.domain.model.BucketCount
import com.gov.employeedb.presentation.common.bucketLabel
import com.gov.employeedb.presentation.common.components.SectionCard
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants
import com.gov.employeedb.presentation.theme.AppMotion

/**
 * The Department tab (PLAN.md Phase-10 checklist): the ten largest departments and the rollup, one
 * horizontal bar per row, with the bar itself as the filter control.
 *
 * The bars are drawn with Compose primitives rather than Vico. Vico 2.0.0-alpha.2 draws a column
 * chart's columns along one axis only, and its `rememberCartesianChart` slots are typed by
 * `AxisPosition` (`Vertical.Start` / `Horizontal.Bottom`), so the axis swap a horizontal column chart
 * would need cannot be expressed at all; the deviation is recorded in PLAN.md Appendix A. A `Canvas`
 * or Vico layer would also have to reproduce the labels, the counts and the tap targets by hand,
 * which is exactly what a row of composables already gives us.
 *
 * Only the **bar geometry** is pinned to [LayoutDirection.Ltr], so the bars grow left to right even
 * in Arabic while the labels around them still follow the reading direction. [WorkforceAnalytics.OTHER_LABEL]
 * and the empty label are aggregates, not departments, so their rows are not selectable: a filter for
 * either of them would match no row.
 */
@Composable
fun DepartmentTab(
    snapshot: AnalyticsSnapshot,
    selectedDepartment: String?,
    onDepartmentSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "distribution") {
            SectionCard(titleRes = R.string.analytics_department) {
                snapshot.departmentCounts.forEach { bucket ->
                    val selectable = bucket.label.isSelectableDepartment()
                    DepartmentBar(
                        bucket = bucket,
                        selected = bucket.label == selectedDepartment,
                        selectable = selectable,
                        onClick = {
                            onDepartmentSelected(if (bucket.label == selectedDepartment) null else bucket.label)
                        },
                    )
                }
            }
        }
    }
}

/** One department: its label and count above a bar whose length is its share of the workforce. */
@Composable
private fun DepartmentBar(
    bucket: BucketCount,
    selected: Boolean,
    selectable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val share by animateFloatAsState(
        targetValue = bucket.share.coerceIn(0f, 1f),
        animationSpec = AppMotion.effects(),
        label = "departmentBarShare",
    )
    val shape = MaterialTheme.shapes.extraSmall
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(enabled = selectable, onClick = onClick)
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = bucketLabel(context, bucket.label),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${bucket.count} (${NumberFormatters.percent(bucket.share)})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BAR_HEIGHT)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                if (share > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(share)
                            .fillMaxHeight()
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
    }
}

/** True when [this] names a real department, rather than an aggregate or a missing value. */
private fun String.isSelectableDepartment(): Boolean =
    isNotEmpty() && this != WorkforceAnalytics.OTHER_LABEL

private val BAR_HEIGHT = 10.dp

@Preview(name = "DepartmentTab")
@Composable
private fun DepartmentTabPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        DepartmentTab(
            snapshot = previewSnapshot,
            selectedDepartment = "Finance",
            onDepartmentSelected = {},
        )
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
    departmentCounts = listOf(
        BucketCount("Finance", 32, 0.27f),
        BucketCount("Health", 27, 0.23f),
        BucketCount("Education", 21, 0.18f),
        BucketCount("Transport", 14, 0.12f),
        BucketCount(WorkforceAnalytics.OTHER_LABEL, 26, 0.22f),
    ),
    gradeCounts = emptyList(),
    longestServing = emptyList(),
)
