package com.gov.employeedb.presentation.importx.steps

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gov.employeedb.R
import com.gov.employeedb.data.excel.ExcelPreview
import com.gov.employeedb.domain.model.EmployeeField
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants
import com.gov.employeedb.presentation.importx.ImportStep
import com.gov.employeedb.presentation.importx.ImportUiState
import kotlin.math.roundToInt

/**
 * Step 2 of the wizard (PLAN.md Phase-7 checklist): a [MappingRow] per header, a banner that states
 * which requirement is still unmet, the duplicate switch, and a Continue button.
 *
 * Continue is enabled only when every core field is mapped and no field is claimed twice, and the
 * banner shakes the moment a collision appears — the user sees *which* invariant they broke at the
 * moment they break it, not only when they try to leave.
 */
@Composable
fun MappingStep(
    state: ImportUiState,
    onFieldChange: (Int, EmployeeField?) -> Unit,
    onSkipDuplicatesChange: (Boolean) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preview = state.preview ?: return
    val sample = preview.sampleRows.firstOrNull()
    val duplicate = state.duplicateField
    val shake = remember { Animatable(0f) }
    LaunchedEffect(duplicate) {
        if (duplicate != null) {
            shake.snapTo(0f)
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = SHAKE_DURATION_MS
                    0f at 0
                    -1f at 50
                    1f at 100
                    -1f at 150
                    1f at 200
                    0f at 250
                },
            )
        }
    }
    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "banner") {
                MappingBanner(
                    state = state,
                    modifier = Modifier.offset {
                        IntOffset((shake.value * SHAKE_DP.dp.toPx()).roundToInt(), 0)
                    },
                )
            }
            itemsIndexed(
                items = preview.headers,
                key = { index, _ -> index },
            ) { index, header ->
                MappingRow(
                    header = header,
                    sample = sample?.getOrNull(index),
                    selected = state.mapping[index],
                    autoDetected = index in state.autoDetectedColumns,
                    isError = duplicate != null && state.mapping[index] == duplicate,
                    onFieldChange = { field -> onFieldChange(index, field) },
                )
            }
            item(key = "skip") {
                SkipDuplicatesRow(
                    checked = state.skipDuplicates,
                    onCheckedChange = onSkipDuplicatesChange,
                )
            }
            item(key = "next") {
                Button(
                    onClick = onNext,
                    enabled = state.mappingValid,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.next))
                }
            }
        }
    }
}

/** The one unmet requirement, or nothing when the mapping is ready. */
@Composable
private fun MappingBanner(state: ImportUiState, modifier: Modifier = Modifier) {
    val duplicate = state.duplicateField
    val messageRes = when {
        duplicate != null -> R.string.mapping_duplicate_field
        !state.coreFieldsMapped -> R.string.mapping_required_fields_hint
        else -> return
    }
    val isError = duplicate != null
    Text(
        text = stringResource(messageRes),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isError) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
            )
            .padding(12.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
    )
}

@Composable
private fun SkipDuplicatesRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.skip_duplicates),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private const val SHAKE_DURATION_MS = 250
private const val SHAKE_DP = 8f

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

@Preview(name = "MappingStep")
@Composable
private fun MappingStepPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        MappingStep(
            state = ImportUiState(
                step = ImportStep.MAPPING,
                preview = SAMPLE_PREVIEW,
                mapping = SAMPLE_MAPPING,
            ),
            onFieldChange = { _, _ -> },
            onSkipDuplicatesChange = {},
            onNext = {},
        )
    }
}
