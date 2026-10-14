package com.gov.employeedb.presentation.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.gov.employeedb.R
import com.gov.employeedb.domain.model.EmployeeSummary
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants

/**
 * One employee row (PLAN.md Phase-6 checklist): the initials avatar, the name, the position and
 * department, and a pair of assist chips carrying the derived age and years of service. A missing
 * derived value simply drops its chip rather than showing a placeholder.
 *
 * [avatarModifier] and [titleModifier] exist for the Phase-8 shared-element transition: the list
 * passes the modifiers that mark this row's avatar and name as one end of the animation, and the
 * defaults keep every other caller (and the preview) unchanged.
 */
@Composable
fun EmployeeCard(
    employee: EmployeeSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatarModifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EmployeeAvatar(fullName = employee.fullName, modifier = avatarModifier)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = employee.fullName,
                    modifier = titleModifier,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = listOfNotNull(employee.position, employee.department)
                    .joinToString(SEPARATOR)
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (employee.age != null || employee.serviceYears != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        employee.age?.let { age ->
                            AssistChip(
                                onClick = {},
                                label = { Text("${stringResource(R.string.age)} $age") },
                            )
                        }
                        employee.serviceYears?.let { years ->
                            AssistChip(
                                onClick = {},
                                label = { Text("${stringResource(R.string.service_years)} $years") },
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val SEPARATOR = " · "

@Preview(name = "EmployeeCard")
@Composable
private fun EmployeeCardPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        EmployeeCard(
            employee = EmployeeSummary(
                id = 1L,
                fullName = "Amine Benali",
                department = "Finance",
                position = "Analyst",
                age = 34,
                serviceYears = 8,
            ),
            onClick = {},
        )
    }
}
