package com.gov.employeedb.presentation.employees.list.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.gov.employeedb.R
import com.gov.employeedb.domain.model.AgeBucket
import com.gov.employeedb.domain.model.ServiceBucket
import com.gov.employeedb.presentation.common.labelRes
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants
import com.gov.employeedb.presentation.employees.list.EmployeeFilters

/**
 * The full filter sheet (PLAN.md Phase-6 checklist): one single-select chip group per dimension
 * (age band, service band, department). Each group maps to a single DAO parameter, so selecting a
 * chip replaces the group's previous value rather than accumulating — tapping the active chip
 * clears it. Edits are held in a local draft until Apply, so Reset is a one-line `copy`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    departments: List<String>,
    filters: EmployeeFilters,
    onApply: (EmployeeFilters) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember(filters) { mutableStateOf(filters) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.filter),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.age),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SingleSelectChips(
                options = AgeBucket.entries,
                selected = draft.ageBucket,
                onSelect = { bucket ->
                    draft = draft.copy(ageBuckets = bucket?.let { setOf(it) } ?: emptySet())
                },
                label = { bucket -> Text(text = stringResource(bucket.labelRes())) },
            )
            Text(
                text = stringResource(R.string.service_years),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SingleSelectChips(
                options = ServiceBucket.entries,
                selected = draft.serviceBucket,
                onSelect = { bucket ->
                    draft = draft.copy(serviceBuckets = bucket?.let { setOf(it) } ?: emptySet())
                },
                label = { bucket -> Text(text = stringResource(bucket.labelRes())) },
            )
            Text(
                text = stringResource(R.string.department),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SingleSelectChips(
                options = departments,
                selected = draft.department.ifEmpty { null },
                onSelect = { department ->
                    draft = draft.copy(
                        departments = department?.let { setOf(it) } ?: emptySet(),
                    )
                },
                label = { department -> Text(text = department) },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { draft = EmployeeFilters(query = draft.query) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(R.string.reset))
                }
                Button(
                    onClick = { onApply(draft) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(R.string.apply))
                }
            }
        }
    }
}

@Composable
private fun <T> SingleSelectChips(
    options: List<T>,
    selected: T?,
    onSelect: (T?) -> Unit,
    label: @Composable (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(if (isSelected) null else option) },
                label = { label(option) },
            )
        }
    }
}

@Preview(name = "FilterSheet")
@Composable
private fun FilterSheetPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        FilterSheet(
            departments = listOf("Finance", "Health"),
            filters = EmployeeFilters(),
            onApply = {},
            onDismiss = {},
        )
    }
}
