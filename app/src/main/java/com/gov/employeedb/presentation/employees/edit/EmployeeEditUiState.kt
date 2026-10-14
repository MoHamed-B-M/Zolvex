package com.gov.employeedb.presentation.employees.edit

import androidx.compose.runtime.Immutable
import com.gov.employeedb.domain.model.EmployeeField

/**
 * Everything the employee form renders from (PLAN.md Phase-8 checklist).
 *
 * [errors] is recomputed on every change, so [isValid] always describes the form as it is *now*,
 * while [showErrors] keeps the messages out of the way until the first Save attempt. That is why
 * [canSave] stays live until then: an invalid first Save has to be clickable for the shake to be
 * able to fire, and from the second attempt on the button is disabled until the form is valid.
 */
@Immutable
data class EmployeeEditUiState(
    val form: EmployeeForm = EmployeeForm(),
    val errors: Map<EmployeeField, EmployeeValidationError> = emptyMap(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isEditing: Boolean = false,
    val showErrors: Boolean = false,
    val notFound: Boolean = false,
    val error: Boolean = false,
) {

    /** True while every field the validator looks at is acceptable. */
    val isValid: Boolean get() = errors.isEmpty()

    /** The Save button follows the form once the first attempt has revealed the errors. */
    val canSave: Boolean get() = !isSaving && (!showErrors || isValid)

    /** The message to show beneath [field], or `null` while the errors are still hidden. */
    fun errorFor(field: EmployeeField): EmployeeValidationError? =
        errors[field].takeIf { showErrors }
}
