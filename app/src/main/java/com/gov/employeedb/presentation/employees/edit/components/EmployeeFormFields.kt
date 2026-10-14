package com.gov.employeedb.presentation.employees.edit.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.gov.employeedb.R
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants
import com.gov.employeedb.presentation.common.rememberDateFormatter
import java.time.LocalDate

/** Milliseconds in a day; the date picker works in UTC epoch millis, so the conversion is exact. */
private const val MILLIS_PER_DAY = 86_400_000L

/**
 * One text field of the employee form (PLAN.md Phase-8 checklist).
 *
 * [leftToRight] forces the field's own direction independently of the UI language: a phone number,
 * an e-mail address and a salary read left-to-right in every locale, so their text starts at the
 * left edge even inside an Arabic layout. The alignment travels inside `textStyle` because Material
 * 3's text fields no longer take a `textAlign` parameter.
 */
@Composable
fun EmployeeTextField(
    @StringRes labelRes: Int,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorRes: Int? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    minLines: Int = 1,
    leftToRight: Boolean = false,
) {
    val supportingText: (@Composable () -> Unit)? = if (errorRes != null) {
        { Text(text = stringResource(errorRes)) }
    } else {
        null
    }
    val field: @Composable () -> Unit = {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            label = { Text(text = stringResource(labelRes)) },
            isError = errorRes != null,
            supportingText = supportingText,
            textStyle = if (leftToRight) {
                LocalTextStyle.current.copy(textAlign = TextAlign.Start)
            } else {
                LocalTextStyle.current
            },
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            minLines = minLines,
        )
    }
    if (leftToRight) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { field() }
    } else {
        field()
    }
}

/**
 * One date field of the employee form (PLAN.md Phase-8 checklist): a read-only field whose calendar
 * button opens the Material 3 [DatePickerDialog].
 *
 * Typing is deliberately impossible - a half-typed date is not a state this form has to model - and
 * the button is described by the field's own label, which is exactly the choice it offers.
 */
@Composable
fun EmployeeDateField(
    @StringRes labelRes: Int,
    value: LocalDate?,
    onValueChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    errorRes: Int? = null,
) {
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    val formatter = rememberDateFormatter()
    val supportingText: (@Composable () -> Unit)? = if (errorRes != null) {
        { Text(text = stringResource(errorRes)) }
    } else {
        null
    }

    OutlinedTextField(
        value = value?.format(formatter).orEmpty(),
        onValueChange = {},
        modifier = modifier.fillMaxWidth(),
        readOnly = true,
        label = { Text(text = stringResource(labelRes)) },
        isError = errorRes != null,
        supportingText = supportingText,
        trailingIcon = {
            IconButton(onClick = { pickerOpen = true }) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = stringResource(labelRes),
                )
            }
        },
    )

    if (pickerOpen) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = value?.toEpochDay()?.let { it * MILLIS_PER_DAY },
        )
        DatePickerDialog(
            onDismissRequest = { pickerOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onValueChange(LocalDate.ofEpochDay(millis / MILLIS_PER_DAY))
                        }
                        pickerOpen = false
                    },
                ) {
                    Text(text = stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pickerOpen = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Preview(name = "EmployeeTextField - LTR field")
@Composable
private fun EmployeeTextFieldPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        EmployeeTextField(
            labelRes = R.string.phone,
            value = "+212600000000",
            onValueChange = {},
            modifier = Modifier.padding(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            leftToRight = true,
        )
    }
}

@Preview(name = "EmployeeTextField - error")
@Composable
private fun EmployeeTextFieldErrorPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        EmployeeTextField(
            labelRes = R.string.full_name,
            value = "",
            onValueChange = {},
            modifier = Modifier.padding(16.dp),
            errorRes = R.string.validation_name_required,
        )
    }
}

@Preview(name = "EmployeeDateField")
@Composable
private fun EmployeeDateFieldPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        EmployeeDateField(
            labelRes = R.string.date_of_birth,
            value = LocalDate.of(1985, 4, 12),
            onValueChange = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
