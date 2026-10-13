package com.gov.employeedb.presentation.employees.list.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.gov.employeedb.R
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants
import com.gov.employeedb.presentation.employees.list.EmployeeFilters

/**
 * The always-visible filter row (PLAN.md Phase-6 checklist): a leading chip that opens the full
 * filter sheet (with an active count) followed by the department quick-chips. Tapping the selected
 * department again clears it, so there is always a one-tap way back to "all".
 */
@Composable
fun FilterBar(
    departments: List<String>,
    filters: EmployeeFilters,
    onDepartmentSelected: (String?) -> Unit,
    onOpenSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            val count = filters.activeCount
            val label = if (count > 0) {
                "${stringResource(R.string.filter)} ($count)"
            } else {
                stringResource(R.string.filter)
            }
            FilterChip(
                selected = filters.isActive,
                onClick = onOpenSheet,
                label = { Text(text = label) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = stringResource(R.string.cd_filter),
                    )
                },
            )
        }
        items(departments) { department ->
            val selected = department in filters.departments
            FilterChip(
                selected = selected,
                onClick = { onDepartmentSelected(if (selected) null else department) },
                label = { Text(text = department) },
            )
        }
    }
}

@Preview(name = "FilterBar")
@Composable
private fun FilterBarPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        FilterBar(
            departments = listOf("Finance", "Health"),
            filters = EmployeeFilters(departments = setOf("Finance")),
            onDepartmentSelected = {},
            onOpenSheet = {},
        )
    }
}
