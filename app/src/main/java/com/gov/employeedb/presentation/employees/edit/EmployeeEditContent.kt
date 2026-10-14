package com.gov.employeedb.presentation.employees.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.gov.employeedb.R
import com.gov.employeedb.domain.model.EmployeeField
import com.gov.employeedb.presentation.common.components.SectionCard
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants
import com.gov.employeedb.presentation.employees.edit.components.EmployeeDateField
import com.gov.employeedb.presentation.employees.edit.components.EmployeeTextField
import java.time.LocalDate

/**
 * The employee form itself (PLAN.md Phase-8 checklist): three [SectionCard]s holding the eleven
 * editable columns, each with its own keyboard.
 *
 * Stateless by design - the screen owns the ViewModel, this owns the layout - so every field, error
 * and locale can be previewed without a database. Phone, e-mail and salary are passed
 * `leftToRight = true`, so their digits and addresses never mirror under an RTL language.
 */
@Composable
fun EmployeeEditContent(
    state: EmployeeEditUiState,
    onEvent: (EmployeeEditEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionCard(titleRes = R.string.personal_info) {
            EmployeeTextField(
                labelRes = R.string.full_name,
                value = state.form.fullName,
                onValueChange = { onEvent(EmployeeEditEvent.FullNameChanged(it)) },
                errorRes = state.errorFor(EmployeeField.FULL_NAME)?.messageRes,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )
            EmployeeDateField(
                labelRes = R.string.date_of_birth,
                value = state.form.dateOfBirth,
                onValueChange = { onEvent(EmployeeEditEvent.DateOfBirthChanged(it)) },
                errorRes = state.errorFor(EmployeeField.DATE_OF_BIRTH)?.messageRes,
            )
            EmployeeTextField(
                labelRes = R.string.email,
                value = state.form.email,
                onValueChange = { onEvent(EmployeeEditEvent.EmailChanged(it)) },
                errorRes = state.errorFor(EmployeeField.EMAIL)?.messageRes,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                leftToRight = true,
            )
            EmployeeTextField(
                labelRes = R.string.phone,
                value = state.form.phone,
                onValueChange = { onEvent(EmployeeEditEvent.PhoneChanged(it)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                ),
                leftToRight = true,
            )
            EmployeeTextField(
                labelRes = R.string.address,
                value = state.form.address,
                onValueChange = { onEvent(EmployeeEditEvent.AddressChanged(it)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
        }

        SectionCard(titleRes = R.string.professional_info) {
            EmployeeDateField(
                labelRes = R.string.hire_date,
                value = state.form.hireDate,
                onValueChange = { onEvent(EmployeeEditEvent.HireDateChanged(it)) },
                errorRes = state.errorFor(EmployeeField.HIRE_DATE)?.messageRes,
            )
            EmployeeTextField(
                labelRes = R.string.department,
                value = state.form.department,
                onValueChange = { onEvent(EmployeeEditEvent.DepartmentChanged(it)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )
            EmployeeTextField(
                labelRes = R.string.position,
                value = state.form.position,
                onValueChange = { onEvent(EmployeeEditEvent.PositionChanged(it)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )
            EmployeeTextField(
                labelRes = R.string.grade,
                value = state.form.grade,
                onValueChange = { onEvent(EmployeeEditEvent.GradeChanged(it)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next,
                ),
            )
            EmployeeTextField(
                labelRes = R.string.salary,
                value = state.form.salary,
                onValueChange = { onEvent(EmployeeEditEvent.SalaryChanged(it)) },
                errorRes = state.errorFor(EmployeeField.SALARY)?.messageRes,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
                leftToRight = true,
            )
        }

        SectionCard(titleRes = R.string.additional_info) {
            EmployeeTextField(
                labelRes = R.string.notes,
                value = state.form.notes,
                onValueChange = { onEvent(EmployeeEditEvent.NotesChanged(it)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default,
                ),
                singleLine = false,
                minLines = 3,
            )
        }
    }
}

@Preview(name = "EmployeeEditContent - filled")
@Composable
private fun EmployeeEditContentPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        EmployeeEditContent(
            state = EmployeeEditUiState(
                form = EmployeeForm(
                    fullName = "Amina Haddad",
                    dateOfBirth = LocalDate.of(1985, 4, 12),
                    hireDate = LocalDate.of(2016, 9, 1),
                    department = "Finance",
                    position = "Analyst",
                    grade = "A2",
                    salary = "45000",
                    email = "amina.haddad@example.gov",
                    phone = "+212600000000",
                    address = "12 Rue des Orangers",
                    notes = "Transferred from the regional office.",
                ),
                isEditing = true,
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "EmployeeEditContent - errors revealed")
@Composable
private fun EmployeeEditContentErrorsPreview(
    @PreviewParameter(PreviewVariants::class) variant: PreviewVariant,
) {
    AppPreview(variant) {
        EmployeeEditContent(
            state = EmployeeEditStateFixtures.withRevealedErrors(),
            onEvent = {},
        )
    }
}

/** The state the error preview renders, kept out of the preview body to keep it readable. */
private object EmployeeEditStateFixtures {

    fun withRevealedErrors(): EmployeeEditUiState {
        val form = EmployeeForm(
            fullName = "",
            email = "not-an-address",
            salary = "45000",
        )
        return EmployeeEditUiState(
            form = form,
            errors = EmployeeValidator.validate(form, LocalDate.of(2026, 10, 12)),
            showErrors = true,
        )
    }
}
