package com.gov.employeedb.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gov.employeedb.domain.model.EmployeeField
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** The process-wide mapping-profile DataStore (a distinct file from `zolvex_settings`). */
private val Context.mappingProfileDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "zolvex_mapping_profiles",
)

/**
 * Persists the user-confirmed header→field mapping so the next file with the same layout skips the
 * manual Step-2 mapping (PLAN.md §6.2 rule 6). The key is `sha256(headers.joinToString("|"))`, so an
 * identical header string produces an identical profile regardless of locale. Bound to the app
 * context at the delegate level, so no extra Hilt module is required.
 */
@Singleton
class MappingProfileStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val appContext: Context = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }

    /** The stored mapping for [headers], or `null` when no profile exists yet. */
    fun load(headers: List<String>): Flow<Map<Int, EmployeeField>?> =
        appContext.mappingProfileDataStore.data
            .catch { cause -> if (cause is IOException) emit(emptyPreferences()) else throw cause }
            .map { prefs -> prefs[profileKey(headers)]?.let { decode(it) } }

    /** Stores the confirmed mapping; the next identical layout re-applies it silently. */
    suspend fun save(headers: List<String>, mapping: Map<Int, EmployeeField>) {
        appContext.mappingProfileDataStore.edit { prefs ->
            prefs[profileKey(headers)] = encode(mapping)
        }
    }

    private fun profileKey(headers: List<String>): Preferences.Key<String> =
        stringPreferencesKey("profile_" + sha256(headers.joinToString("|")))

    private fun encode(mapping: Map<Int, EmployeeField>): String {
        val size = (mapping.keys.maxOrNull() ?: -1) + 1
        val list = MutableList(size) { "" }
        mapping.forEach { (column, field) -> list[column] = field.name }
        return json.encodeToString(list)
    }

    private fun decode(encoded: String): Map<Int, EmployeeField>? {
        val list = try {
            json.decodeFromString<List<String>>(encoded)
        } catch (_: RuntimeException) {
            return null
        }
        return list.mapIndexedNotNull { index, name ->
            val field = EmployeeField.entries.firstOrNull { it.name == name } ?: return@mapIndexedNotNull null
            index to field
        }.toMap()
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}