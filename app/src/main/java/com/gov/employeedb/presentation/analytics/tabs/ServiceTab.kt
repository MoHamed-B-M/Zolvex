package com.gov.employeedb.presentation.analytics.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WorkHistory
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.gov.employeedb.R
import com.gov.employeedb.domain.model.AnalyticsSnapshot
import com.gov.employeedb.domain.model.BucketCount
import com.gov.employeedb.domain.model.EmployeeSummary
import com.gov.employeedb.domain.model.ServiceBucket
import com.gov.employeedb.presentation.common.components.BarChartCard
import com.gov.employeedb.presentation.common.components.EmployeeCard
import com.gov.employeedb.presentation.common.components.StatCard
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants

/**
 * The Service tab (PLAN.md Phase-10 checklist): the years-of-service histogram as a Vico column
 * chart, the average and the median as statistic cards, and then the five longest-serving employees.
 *
 * The employee rows are the same [EmployeeCard] the dashboard and the list show, so tapping one
 * opens that employee's detail. [AnalyticsSnapshot.longestServing] is already the top five, ordered
 * by the engine, so this tab only has to draw it.
 */
@Composable
fun ServiceTab(
    snapshot: AnalyticsSnapshot,
    onEmployeeClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    icon = Icons.Outlined.WorkHistory,
                    labelRes = R.string.average_service,
                    value = snapshot.averageService,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    icon = Icons.Outlined.WorkOutline,
                    labelRes = R.string.median_service,
                    value = snapshot.medianService,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item(key = "distribution") {
            BarChartCard(buckets = snapshot.serviceBuckets, titleRes = R.string.analytics_service)
        }
        if (snapshot.longestServing.isNotEmpty()) {
            item(key = "longest_header") {
                Text(
                    text = stringResource(R.string.longest_serving),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            items(
                items = snapshot.longestServing,
                key = { employee -> employee.id },
            ) { employee ->
                EmployeeCard(employee = employee, onClick = { onEmployeeClick(employee.id) })
            }
        }
    }
}

@Preview(name = "ServiceTab")
@Composable
private fun ServiceTabPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        ServiceTab(snapshot = previewSnapshot, onEmployeeClick = {})
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
    serviceBuckets = listOf(
        BucketCount(ServiceBucket.LESS_5.name, 24, 0.20f),
        BucketCount(ServiceBucket.B5_10.name, 38, 0.32f),
        BucketCount(ServiceBucket.B10_15.name, 31, 0.26f),
        BucketCount(ServiceBucket.B15_20.name, 19, 0.16f),
        BucketCount(ServiceBucket.B20_PLUS.name, 8, 0.06f),
    ),
    departmentCounts = emptyList(),
    gradeCounts = emptyList(),
    longestServing = listOf(
        EmployeeSummary(
            id = 1L,
            fullName = "Amina Haddad",
            department = "Finance",
            position = "Head of accounting",
            age = 58,
            serviceYears = 31,
        ),
        EmployeeSummary(
            id = 2L,
            fullName = "Louis Bernard",
            department = "Health",
            position = "Chief physician",
            age = 61,
            serviceYears = 27,
        ),
        EmployeeSummary(
            id = 3L,
            fullName = "Nadia Cherif",
            department = "Education",
            position = "Inspector",
            age = 54,
            serviceYears = 22,
        ),
    ),
)
