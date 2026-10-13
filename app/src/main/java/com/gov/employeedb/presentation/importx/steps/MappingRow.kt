package com.gov.employeedb.presentation.importx.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.gov.employeedb.R
import com.gov.employeedb.domain.model.EmployeeField
import com.gov.employeedb.presentation.common.labelRes
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants

/**
 * One header→field row of the mapping step (PLAN.md Phase-7 checklist): the raw header and its
 * first sample value on the left, the field dropdown on the right.
 *
 * A row the detector filled in is tinted, so the user only has to look at the untinted ones; a row
 * that collides with another is tinted with the error colour and flagged through [isError].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MappingRow(
    header: String,
    sample: String?,
    selected: EmployeeField?,
    autoDetected: Boolean,
    onFieldChange: (EmployeeField?) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    val tint = when {
        isError -> MaterialTheme.colorScheme.errorContainer
        autoDetected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(tint)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = header,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!sample.isNullOrBlank()) {
                Text(
                    text = sample,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.width(160.dp),
        ) {
            OutlinedTextField(
                value = selected?.let { stringResource(it.labelRes()) }
                    ?: stringResource(R.string.ignore),
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                isError = isError,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.ignore)) },
                    onClick = {
                        expanded = false
                        onFieldChange(null)
                    },
                )
                EmployeeField.entries.forEach { field ->
                    DropdownMenuItem(
                        text = { Text(text = stringResource(field.labelRes())) },
                        onClick = {
                            expanded = false
                            onFieldChange(field)
                        },
                    )
                }
            }
        }
    }
}

@Preview(name = "MappingRow")
@Composable
private fun MappingRowPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        MappingRow(
            header = "Birth date",
            sample = "1990-04-02",
            selected = EmployeeField.DATE_OF_BIRTH,
            autoDetected = true,
            onFieldChange = {},
        )
    }
}
