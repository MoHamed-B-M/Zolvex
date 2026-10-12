package com.gov.employeedb.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gov.employeedb.data.local.dao.EmployeeDao
import com.gov.employeedb.testutil.MainDispatcherRule
import com.gov.employeedb.testutil.TestData
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * DAO-level contract of PLAN.md §5.6, run against an in-memory Room database on the JVM
 * (Robolectric). Every assertion is the SQL itself: paging, ORDER BY CASE sorting, the range
 * filters, the null-safe dedupe lookup and the column aliases of the projections.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EmployeeDaoTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var database: AppDatabase
    private lateinit var dao: EmployeeDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = database.employeeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insert_update_delete_roundTrips() = runTest {
        val id = dao.insert(TestData.entity(fullName = "Amine"))

        assertThat(id).isGreaterThan(0L)
        assertThat(dao.observeCount().first()).isEqualTo(1)
        assertThat(dao.getById(id)?.fullName).isEqualTo("Amine")
        // The mapper computes the dedupe key; the DAO persists and repeats it verbatim.
        assertThat(dao.getById(id)?.fullNameKey).isEqualTo("amine")

        dao.update(TestData.entity(id = id, fullName = "Amine B."))
        assertThat(dao.getById(id)?.fullName).isEqualTo("Amine B.")
        assertThat(dao.observeCount().first()).isEqualTo(1)

        dao.deleteById(id)
        assertThat(dao.getById(id)).isNull()
        assertThat(dao.observeCount().first()).isEqualTo(0)
    }

    @Test
    fun deleteEntity_removesOnlyThatRow() = runTest {
        val kept = dao.insert(TestData.entity(fullName = "Amine"))
        val removed = dao.insert(TestData.entity(fullName = "Bilal"))

        dao.delete(TestData.entity(id = removed, fullName = "Bilal"))

        assertThat(dao.getById(removed)).isNull()
        assertThat(dao.getById(kept)?.fullName).isEqualTo("Amine")
        assertThat(dao.observeCount().first()).isEqualTo(1)
    }

    @Test
    fun insertAll_returnsOneGeneratedIdPerRow() = runTest {
        val ids = dao.insertAll(
            listOf("Amine", "Salma", "Zineb").map { TestData.entity(fullName = it) },
        )

        assertThat(ids).hasSize(3)
        assertThat(ids[0]).isGreaterThan(0L)
        assertThat(ids[1]).isGreaterThan(ids[0])
        assertThat(ids[2]).isGreaterThan(ids[1])
        assertThat(dao.observeCount().first()).isEqualTo(3)
    }

    @Test
    fun observePage_paginatesWithLimitAndOffset() = runTest {
        val names = listOf("Amine", "Bilal", "Chaima", "Dalia", "Elena")
        dao.insertAll(names.map { TestData.entity(fullName = it) })

        val firstPage = dao.observePage("", "", null, null, null, null, "NAME", true, 2, 0).first()
        assertThat(firstPage.map { it.fullName }).containsExactly("Amine", "Bilal").inOrder()

        val secondPage = dao.observePage("", "", null, null, null, null, "NAME", true, 2, 2).first()
        assertThat(secondPage.map { it.fullName }).containsExactly("Chaima", "Dalia").inOrder()

        val tailPage = dao.observePage("", "", null, null, null, null, "NAME", true, 2, 4).first()
        assertThat(tailPage.map { it.fullName }).containsExactly("Elena").inOrder()

        assertThat(dao.observeCount().first()).isEqualTo(5)
    }

    @Test
    fun observePage_sortsByNameAscendingAndDescending() = runTest {
        dao.insertAll(listOf("Bilal", "Amine", "Chaima").map { TestData.entity(fullName = it) })

        val ascending = dao.observePage("", "", null, null, null, null, "NAME", true, 10, 0).first()
        assertThat(ascending.map { it.fullName }).containsExactly("Amine", "Bilal", "Chaima").inOrder()

        val descending = dao.observePage("", "", null, null, null, null, "NAME", false, 10, 0).first()
        assertThat(descending.map { it.fullName }).containsExactly("Chaima", "Bilal", "Amine").inOrder()
    }

    @Test
    fun observePage_sortByAge_youngestFirstWhenAscending() = runTest {
        dao.insertAll(
            listOf(
                TestData.entity(fullName = "Older", birthDate = LocalDate.of(1960, 6, 1)),
                TestData.entity(fullName = "Younger", birthDate = LocalDate.of(2000, 3, 1)),
            ),
        )

        val ascending = dao.observePage("", "", null, null, null, null, "AGE", true, 10, 0).first()
        assertThat(ascending.map { it.fullName }).containsExactly("Younger", "Older").inOrder()

        val descending = dao.observePage("", "", null, null, null, null, "AGE", false, 10, 0).first()
        assertThat(descending.map { it.fullName }).containsExactly("Older", "Younger").inOrder()
    }

    @Test
    fun observePage_filtersByDepartment() = runTest {
        dao.insertAll(
            listOf(
                TestData.entity(fullName = "Amine", department = "Interior"),
                TestData.entity(fullName = "Bilal", department = "Interior"),
                TestData.entity(fullName = "Chaima", department = "Finance"),
            ),
        )

        val interior = dao.observePage("", "Interior", null, null, null, null, "NAME", true, 10, 0).first()
        assertThat(interior.map { it.fullName }).containsExactly("Amine", "Bilal").inOrder()

        val all = dao.observePage("", "", null, null, null, null, "NAME", true, 10, 0).first()
        assertThat(all).hasSize(3)
    }

    @Test
    fun observePage_filtersByBirthDateRange() = runTest {
        dao.insertAll(
            listOf(
                TestData.entity(fullName = "Before", birthDate = LocalDate.of(1985, 6, 1)),
                TestData.entity(fullName = "Inside", birthDate = LocalDate.of(1995, 6, 1)),
                TestData.entity(fullName = "After", birthDate = LocalDate.of(2005, 6, 1)),
            ),
        )
        val from = LocalDate.of(1990, 1, 1).toEpochDay()
        val to = LocalDate.of(2000, 1, 1).toEpochDay()

        val rows = dao.observePage("", "", from, to, null, null, "NAME", true, 10, 0).first()
        assertThat(rows.map { it.fullName }).containsExactly("Inside").inOrder()
    }

    @Test
    fun observePage_searchesNamePositionAndDepartment() = runTest {
        dao.insert(
            TestData.entity(fullName = "Ahmed", position = "Medecin", department = "Sante"),
        )

        assertThat(dao.observePage("Ahmed", "", null, null, null, null, "NAME", true, 10, 0).first()).hasSize(1)
        assertThat(dao.observePage("medecin", "", null, null, null, null, "NAME", true, 10, 0).first()).hasSize(1)
        assertThat(dao.observePage("SANTE", "", null, null, null, null, "NAME", true, 10, 0).first()).hasSize(1)
        assertThat(dao.observePage("zzz", "", null, null, null, null, "NAME", true, 10, 0).first()).isEmpty()
    }

    @Test
    fun observeRecent_ordersByCreatedAtDescendingAndLimits() = runTest {
        dao.insertAll(
            listOf(
                TestData.entity(fullName = "Old", createdAt = 100L),
                TestData.entity(fullName = "Middle", createdAt = 200L),
                TestData.entity(fullName = "New", createdAt = 300L),
            ),
        )

        val all = dao.observeRecent(5).first()
        assertThat(all.map { it.fullName }).containsExactly("New", "Middle", "Old").inOrder()

        val two = dao.observeRecent(2).first()
        assertThat(two.map { it.fullName }).containsExactly("New", "Middle").inOrder()
    }

    @Test
    fun observeStatRows_returnsTheStatProjection() = runTest {
        dao.insert(
            TestData.entity(
                fullName = "Amine",
                birthDate = LocalDate.of(1970, 5, 5),
                hireDate = LocalDate.of(2000, 3, 1),
                department = "Interior",
                position = "Chef de service",
                grade = "A2",
            ),
        )

        val rows = dao.observeStatRows().first()
        assertThat(rows).hasSize(1)
        val row = rows.single()
        assertThat(row.id).isGreaterThan(0L)
        assertThat(row.fullName).isEqualTo("Amine")
        assertThat(row.position).isEqualTo("Chef de service")
        assertThat(row.department).isEqualTo("Interior")
        assertThat(row.grade).isEqualTo("A2")
        assertThat(row.birthDate).isEqualTo(LocalDate.of(1970, 5, 5))
        assertThat(row.hireDate).isEqualTo(LocalDate.of(2000, 3, 1))
    }

    @Test
    fun observeById_emitsTheRowOrNull() = runTest {
        assertThat(dao.observeById(99L).first()).isNull()

        val id = dao.insert(TestData.entity(fullName = "Amine"))
        assertThat(dao.observeById(id).first()?.fullName).isEqualTo("Amine")
    }

    @Test
    fun countMatching_deduplicatesOnNormalizedNameAndBirthDate() = runTest {
        dao.insert(TestData.entity(fullName = "  Amine  ", birthDate = LocalDate.of(1990, 5, 5)))
        val birthDay = LocalDate.of(1990, 5, 5).toEpochDay()

        assertThat(dao.countMatching("amine", birthDay)).isEqualTo(1)
        assertThat(dao.countMatching("amine", LocalDate.of(1990, 5, 6).toEpochDay())).isEqualTo(0)
        assertThat(dao.countMatching("salma", birthDay)).isEqualTo(0)

        // The `IS :epochDay` comparison is null-safe: no birth date matches the null bound.
        dao.insert(TestData.entity(fullName = "NoBirthday"))
        assertThat(dao.countMatching("nobirthday", null)).isEqualTo(1)
    }

    @Test
    fun deleteAll_clearsEveryRow() = runTest {
        dao.insertAll((1..4).map { TestData.entity(fullName = "Row$it") })

        dao.deleteAll()

        assertThat(dao.observeCount().first()).isEqualTo(0)
        assertThat(dao.observeRecent(10).first()).isEmpty()
    }
}