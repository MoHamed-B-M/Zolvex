package com.gov.employeedb.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.TypeConverters
import androidx.room.Update
import com.gov.employeedb.data.local.Converters
import com.gov.employeedb.data.local.entity.EmployeeEntity
import com.gov.employeedb.data.local.projection.EmployeeListItemEntity
import com.gov.employeedb.data.local.projection.EmployeeStatRow
import kotlinx.coroutines.flow.Flow

/** Full read/write surface over `employees` (PLAN.md §5.6). */
@Dao
@TypeConverters(Converters::class)
interface EmployeeDao {

    /**
     * One windowed page of the filtered, sorted list. Search covers name, position and department;
     * filters are department equality plus epochDay ranges; the ORDER BY is a single indexed query.
     */
    @Query(
        """
        SELECT id, fullName, department, position, birth_date AS dateOfBirth, hire_date AS hireDate
        FROM employees
        WHERE (:q = '' OR fullName LIKE '%' || :q || '%'
                       OR position LIKE '%' || :q || '%'
                       OR department LIKE '%' || :q || '%')
          AND (:department = '' OR department = :department)
          AND (:birthFrom IS NULL OR birth_date >= :birthFrom)
          AND (:birthTo   IS NULL OR birth_date <  :birthTo)
          AND (:hireFrom  IS NULL OR hire_date  >= :hireFrom)
          AND (:hireTo    IS NULL OR hire_date  <  :hireTo)
        ORDER BY
          CASE WHEN :sortKey = 'NAME'       AND :ascending THEN fullName    END ASC,
          CASE WHEN :sortKey = 'NAME'       AND NOT :ascending THEN fullName END DESC,
          CASE WHEN :sortKey = 'AGE'        AND :ascending THEN birth_date  END DESC,
          CASE WHEN :sortKey = 'AGE'        AND NOT :ascending THEN birth_date END ASC,
          CASE WHEN :sortKey = 'HIRE_DATE'  AND :ascending THEN hire_date   END ASC,
          CASE WHEN :sortKey = 'HIRE_DATE'  AND NOT :ascending THEN hire_date END DESC,
          CASE WHEN :sortKey = 'DEPARTMENT' AND :ascending THEN department  END ASC,
          CASE WHEN :sortKey = 'DEPARTMENT' AND NOT :ascending THEN department END DESC,
          id ASC
        LIMIT :limit OFFSET :offset
        """,
    )
    fun observePage(
        q: String,
        department: String,
        birthFrom: Long?,
        birthTo: Long?,
        hireFrom: Long?,
        hireTo: Long?,
        sortKey: String,
        ascending: Boolean,
        limit: Int,
        offset: Int,
    ): Flow<List<EmployeeListItemEntity>>

    @Query("SELECT COUNT(*) FROM employees")
    fun observeCount(): Flow<Int>

    /** Import dedupe: one indexed lookup on the normalized name + birth epochDay (null-safe). */
    @Query("SELECT COUNT(*) FROM employees WHERE fullNameKey = :key AND birth_date IS :epochDay")
    suspend fun countMatching(key: String, epochDay: Long?): Int

    @Query(
        """
        SELECT id, fullName, position, birth_date AS birthDate, hire_date AS hireDate,
               department, grade
        FROM employees
        """,
    )
    fun observeStatRows(): Flow<List<EmployeeStatRow>>

    @Query(
        """
        SELECT id, fullName, department, position, birth_date AS dateOfBirth, hire_date AS hireDate
        FROM employees
        ORDER BY createdAt DESC
        LIMIT :limit
        """,
    )
    fun observeRecent(limit: Int): Flow<List<EmployeeListItemEntity>>

    @Query("SELECT * FROM employees WHERE id = :id")
    fun observeById(id: Long): Flow<EmployeeEntity?>

    @Query("SELECT * FROM employees WHERE id = :id")
    suspend fun getById(id: Long): EmployeeEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: EmployeeEntity): Long

    @Insert
    suspend fun insertAll(entities: List<EmployeeEntity>): List<Long>

    @Update
    suspend fun update(entity: EmployeeEntity)

    @Delete
    suspend fun delete(entity: EmployeeEntity)

    @Query("DELETE FROM employees WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM employees")
    suspend fun deleteAll()
}