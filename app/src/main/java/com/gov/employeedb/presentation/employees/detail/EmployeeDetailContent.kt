package com.gov.employeedb.presentation.employees.detail

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.gov.employeedb.R
import com.gov.employeedb.domain.model.Employee
import com.gov.employeedb.presentation.common.components.SectionCard
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants
import com.gov.employeedb.presentation.common.rememberDateFormatter
import com.gov.employeedb.presentation.employees.detail.components.EmployeeDetailHeader
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

/**
 * The read-only employee body (PLAN.md Phase-8 checklist): the [EmployeeDetailHeader] followed by
 * the Personal, Professional and Notes [SectionCard]s.
 *
 * A section is rendered only when it has something to say, so an employee with no address and no
 * phone gets a shorter Personal card instead of a column of placeholders. Every value is either the
 * stored text or derived through the domain model ([Employee.age], [Employee.yearsOfService]) and
 * formatted in the user's language - dates through [rememberDateFormatter], salary through the
 * locale's own number format - so nothing here hardcodes a pattern or a separator.
 */
@Composable
fun EmployeeDetailContent(
    employee: Employee,
    modifier: Modifier = Modifier,
    avatarModifier: Modifier = Modifier,
    nameModifier: Modifier = Modifier,
) {
    val formatter = rememberDateFormatter()
    val locale = LocalConfiguration.current.locales[0]
    val salaryFormat = remember(locale) { NumberFormat.getNumberInstance(locale) }

    val personal = listOfNotNull(
        employee.dateOfBirth?.let { R.string.date_of_birth to it.format(formatter) },
        employee.age?.let { R.string.age to it.toString() },
        employee.email?.let { R.string.email to it },
        employee.phone?.let { R.string.phone to it },
        employee.address?.let { R.string.address to it },
    )
    val professional = listOfNotNull(
        employee.hireDate?.let { R.string.hire_date to it.format(formatter) },
        employee.yearsOfService?.let { R.string.service_years to it.toString() },
        employee.department?.let { R.string.department to it },
        employee.position?.let { R.string.position to it },
        employee.grade?.let { R.string.grade to it },
        employee.salary?.let { R.string.salary to salaryFormat.format(it) },
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        EmployeeDetailHeader(
            fullName = employee.fullName,
            position = employee.position,
            avatarModifier = avatarModifier,
            nameModifier = nameModifier,
        )

        if (personal.isNotEmpty()) {
            SectionCard(titleRes = R.string.personal_info) {
                personal.forEach { (labelRes, value) -> DetailField(labelRes = labelRes, value = value) }
            }
        }

        if (professional.isNotEmpty()) {
            SectionCard(titleRes = R.string.professional_info) {
                professional.forEach { (labelRes, value) -> DetailField(labelRes = labelRes, value = value) }
            }
        }

        employee.notes?.let { notes ->
            SectionCard(titleRes = R.string.notes) {
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** One caption-over-value pair of a detail section: the label muted, the value the readable one. */
@Composable
private fun DetailField(@StringRes labelRes: Int, value: String) {
    Column {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** A fully populated row, so the preview shows every section at its longest. */
private val PreviewEmployee = Employee(
    id = 1L,
    fullName = "Amina Haddad",
    dateOfBirth = LocalDate.of(1985, 4, 12),
    hireDate = LocalDate.of(2016, 9, 1),
    department = "Finance",
    position = "Analyst",
    grade = "A2",
    salary = 45000.0,
    email = "amina.haddad@example.gov",
    phone = "+212600000000",
    address = "12 Rue des Orangers",
    notes = "Transferred from the regional office after the 2016 reorganisation.",
)

@Preview(name = "EmployeeDetailContent")
@Composable
private fun EmployeeDetailContentPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        EmployeeDetailContent(employee = PreviewEmployee)
    }
}

/** The sparse end of the range: only the name is known, so every optional section disappears. */
@Preview(name = "EmployeeDetailContent - name only")
@Composable
private fun EmployeeDetailContentSparsePreview(
    @PreviewParameter(PreviewVariants::class) variant: PreviewVariant,
) {
    AppPreview(variant) {
        EmployeeDetailContent(employee = Employee(id = 1L, fullName = "Amine Benali"))
    }
}
