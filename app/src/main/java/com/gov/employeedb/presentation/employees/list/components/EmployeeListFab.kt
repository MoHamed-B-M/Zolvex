package com.gov.employeedb.presentation.employees.list.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.gov.employeedb.R
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants

/**
 * The expanding "add" FAB (PLAN.md Phase-6 checklist): the main button flips to a close icon and
 * two extended actions scale out of its bottom-end corner (`TransformOrigin(1f, 1f)`), which is the
 * Material 3 expressive motion for a speed-dial.
 */
@Composable
fun EmployeeListFab(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAddEmployee: () -> Unit,
    onImportExcel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FabAction(
            visible = expanded,
            icon = Icons.Outlined.FileUpload,
            labelRes = R.string.import_excel,
            contentDescriptionRes = R.string.cd_import,
            onClick = {
                onExpandedChange(false)
                onImportExcel()
            },
        )
        FabAction(
            visible = expanded,
            icon = Icons.Outlined.PersonAdd,
            labelRes = R.string.add_employee,
            contentDescriptionRes = R.string.cd_add,
            onClick = {
                onExpandedChange(false)
                onAddEmployee()
            },
        )
        FloatingActionButton(onClick = { onExpandedChange(!expanded) }) {
            Icon(
                imageVector = if (expanded) Icons.Outlined.Close else Icons.Outlined.Add,
                contentDescription = stringResource(
                    if (expanded) R.string.close else R.string.cd_add,
                ),
            )
        }
    }
}

@Composable
private fun FabAction(
    visible: Boolean,
    icon: ImageVector,
    @StringRes labelRes: Int,
    @StringRes contentDescriptionRes: Int,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(transformOrigin = TransformOrigin(1f, 1f)) + fadeIn(),
        exit = scaleOut(transformOrigin = TransformOrigin(1f, 1f)) + fadeOut(),
    ) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(contentDescriptionRes),
                )
            },
            text = { Text(text = stringResource(labelRes)) },
        )
    }
}

@Preview(name = "EmployeeListFab")
@Composable
private fun EmployeeListFabPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        EmployeeListFab(
            expanded = true,
            onExpandedChange = {},
            onAddEmployee = {},
            onImportExcel = {},
        )
    }
}
