package com.gov.employeedb.domain.usecase

import com.gov.employeedb.di.IoDispatcher
import com.gov.employeedb.domain.model.Employee
import com.gov.employeedb.domain.repository.EmployeeRepository
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Random
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Fills the database with a deterministic 10 000-row dataset (PLAN.md Phase-6 checklist), used by
 * the debug-only top-bar action so the list, search and paging can be exercised without a real
 * spreadsheet.
 *
 * The seed is fixed ([SEED]) and every value is derived from it, so two runs produce the same rows
 * in the same order, and the rows are written in [CHUNK]-sized transactional batches — the same
 * shape the Excel importer uses — to keep the insert flat on memory.
 */
class SeedDebugDataUseCase @Inject constructor(
    private val repository: EmployeeRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke() = withContext(dispatcher) {
        val random = Random(SEED)
        val batch = ArrayList<Employee>(CHUNK)
        repeat(COUNT) { index ->
            batch += employee(index, random)
            if (batch.size == CHUNK) {
                repository.insertImportBatch(batch)
                batch.clear()
            }
        }
        if (batch.isNotEmpty()) {
            repository.insertImportBatch(batch)
        }
    }

    private fun employee(index: Int, random: Random): Employee {
        val firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.size)]
        val lastName = LAST_NAMES[random.nextInt(LAST_NAMES.size)]
        val birth = randomDate(random, BIRTH_START, BIRTH_END)
        val earliestHire = birth.plusYears(MIN_HIRE_AGE)
        val hire = maxOf(randomDate(random, HIRE_START, HIRE_END), earliestHire).coerceAtMost(HIRE_END)
        val department = DEPARTMENTS[random.nextInt(DEPARTMENTS.size)]
        val position = POSITIONS[random.nextInt(POSITIONS.size)]
        return Employee(
            fullName = "$firstName $lastName",
            dateOfBirth = birth,
            hireDate = hire,
            department = department,
            position = position,
            grade = GRADES[random.nextInt(GRADES.size)],
            salary = SALARY_FLOOR + random.nextInt(SALARY_RANGE).toDouble(),
            email = "${firstName.lowercase()}.${lastName.lowercase()}$index@${EMAIL_DOMAIN}",
            phone = PHONE_PREFIX + (PHONE_FLOOR + random.nextInt(PHONE_RANGE)),
        )
    }

    private fun randomDate(random: Random, start: LocalDate, end: LocalDate): LocalDate =
        start.plusDays(random.nextLong(ChronoUnit.DAYS.between(start, end) + 1))

    private companion object {
        const val SEED = 42L
        const val COUNT = 10_000
        const val CHUNK = 500

        /** No employee is younger than [MIN_HIRE_AGE] at hire, as a real payroll would require. */
        const val MIN_HIRE_AGE = 18L

        const val SALARY_FLOOR = 20_000
        const val SALARY_RANGE = 60_001
        const val PHONE_PREFIX = "+213"
        const val PHONE_FLOOR = 100_000_000
        const val PHONE_RANGE = 900_000_000
        const val EMAIL_DOMAIN = "example.gov"

        val BIRTH_START: LocalDate = LocalDate.of(1958, 1, 1)
        val BIRTH_END: LocalDate = LocalDate.of(2003, 12, 31)
        val HIRE_START: LocalDate = LocalDate.of(1990, 1, 1)
        val HIRE_END: LocalDate = LocalDate.of(2024, 12, 31)

        val FIRST_NAMES = listOf(
            "Amine", "Sara", "Youssef", "Nadia", "Karim", "Leila", "Omar", "Yasmine",
            "Rachid", "Imane", "Sofiane", "Meriem", "Bilal", "Lina", "Adel", "Salma",
        )

        val LAST_NAMES = listOf(
            "Benali", "Bouzid", "Khelifi", "Mansouri", "Saidi", "Zerrouki", "Hamdi", "Cherif",
            "Bensalem", "Talbi", "Meziane", "Belkacem", "Haddad", "Boukhalfa", "Ait Ali", "Rahmani",
        )

        val DEPARTMENTS = listOf(
            "Finance", "Human Resources", "Information Technology", "Legal", "Operations",
            "Public Works", "Health", "Education", "Transport", "Agriculture",
        )

        val POSITIONS = listOf(
            "Analyst", "Officer", "Manager", "Technician", "Specialist", "Coordinator",
            "Director", "Assistant",
        )

        val GRADES = listOf("A", "B", "C", "D")
    }
}
