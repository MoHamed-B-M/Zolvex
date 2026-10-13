package com.gov.employeedb.presentation.importx.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.gov.employeedb.R
import com.gov.employeedb.data.excel.ExcelPreview
import com.gov.employeedb.domain.model.EmployeeField
import com.gov.employeedb.presentation.common.components.LabeledValue
import com.gov.employeedb.presentation.common.labelRes
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants
import com.gov.employeedb.presentation.importx.ImportStep
import com.gov.employeedb.presentation.importx.ImportUiState

/**
 * Step 3 of the wizard (PLAN.md Phase-7 checklist): a read-only table of the mapped columns against
 * the first sample rows, the sheet's row estimate, and the action that commits the run.
 *
 * Only mapped columns are shown, so the table is exactly what the pipeline will read; the header
 * band and the data band use different tonal containers, which keeps a wide, ragged table legible
 * without drawing any grid lines. Both scroll axes are shared by every cell, so the table never
 * tears apart.
 */
@Composable
fun PreviewStep(
    state: ImportUiState,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preview = state.preview ?: return
    val columns = remember(state.mapping) {
        state.mapping.entries.sortedBy { it.key }.toList()
    }
    val rows = remember(preview) { preview.sampleRows.take(PREVIEW_ROWS) }
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            LabeledValue(
                label = stringResource(R.string.total_rows),
                value = preview.totalRowEstimate.toString(),
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Column {
                    Row {
                        columns.forEach { (_, field) ->
                            TableCell(
                                text = stringResource(field.labelRes()),
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                    rows.forEach { row ->
                        Row {
                            columns.forEach { (index, _) ->
                                TableCell(
                                    text = row.getOrNull(index).orEmpty(),
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
        Button(
            onClick = onImport,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(text = stringResource(R.string.import_excel))
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    containerColor: Color,
    contentColor: Color,
    style: TextStyle,
) {
    Text(
        text = text,
        modifier = Modifier
            .width(CELL_WIDTH)
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        style = style,
        color = contentColor,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

private const val PREVIEW_ROWS = 10
private val CELL_WIDTH = 140.dp

private val SAMPLE_MAPPING: Map<Int, EmployeeField> = mapOf(
    0 to EmployeeField.FULL_NAME,
    1 to EmployeeField.DATE_OF_BIRTH,
    2 to EmployeeField.HIRE_DATE,
)

private val SAMPLE_PREVIEW = ExcelPreview(
    sheetName = "Employees",
    headerRowIndex = 0,
    headers = listOf("Name", "Birth date", "Hire date"),
    detectedMapping = SAMPLE_MAPPING,
    sampleRows = listOf(listOf("Amina Haddad", "1990-04-02", "2016-09-01")),
    totalRowEstimate = 128,
)

@Preview(name = "PreviewStep")
@Composable
private fun PreviewStepPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        PreviewStep(
            state = ImportUiState(
                step = ImportStep.PREVIEW,
                preview = SAMPLE_PREVIEW,
                mapping = SAMPLE_MAPPING,
            ),
            onImport = {},
        )
    }
}
