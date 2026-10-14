package com.gov.employeedb.presentation.employees.list.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.gov.employeedb.R
import com.gov.employeedb.presentation.common.components.SeedTestDataAction
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants
import com.gov.employeedb.presentation.employees.list.EmployeeSort

/**
 * The list's top bar (PLAN.md Phase-6 checklist): the screen title, a sort action that anchors
 * [SortMenu], and [SeedTestDataAction], which is itself a no-op in release builds.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeListTopBar(
    isSeeding: Boolean,
    sort: EmployeeSort,
    sortMenuOpen: Boolean,
    onSortMenuOpenChange: (Boolean) -> Unit,
    onSortSelected: (EmployeeSort) -> Unit,
    onSeed: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.employees),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        actions = {
            Box {
                IconButton(onClick = { onSortMenuOpenChange(true) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = stringResource(R.string.cd_sort),
                    )
                }
                SortMenu(
                    expanded = sortMenuOpen,
                    sort = sort,
                    onDismiss = { onSortMenuOpenChange(false) },
                    onSelected = onSortSelected,
                )
            }
            SeedTestDataAction(isSeeding = isSeeding, onSeed = onSeed)
        },
    )
}

@Preview(name = "EmployeeListTopBar")
@Composable
private fun EmployeeListTopBarPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        EmployeeListTopBar(
            isSeeding = false,
            sort = EmployeeSort(),
            sortMenuOpen = false,
            onSortMenuOpenChange = {},
            onSortSelected = {},
            onSeed = {},
        )
    }
}
