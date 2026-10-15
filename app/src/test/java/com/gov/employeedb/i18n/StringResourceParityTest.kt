package com.gov.employeedb.i18n

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Fails the build whenever the three locale files drift apart.
 *
 * The resource XML is read straight off disk with JDK APIs instead of through a `Resources`-backed
 * test, because the point of the check is the *files*: a key that exists in English but not in
 * Arabic is invisible to the resource system until an Arabic user opens that exact screen. This
 * test turns that into a build failure.
 *
 * It deliberately does not go through aapt2, so a value must stay valid XML here too.
 */
class StringResourceParityTest {

    private val english: Map<String, String> = readStrings(resourceFile("values"))
    private val french: Map<String, String> = readStrings(resourceFile("values-b+fr"))
    private val arabic: Map<String, String> = readStrings(resourceFile("values-b+ar"))

    @Test
    fun everyLocaleDeclaresExactlyTheSameKeys() {
        val englishKeys = english.keys.sorted()
        assertEquals("French and English have drifted apart", englishKeys, french.keys.sorted())
        assertEquals("Arabic and English have drifted apart", englishKeys, arabic.keys.sorted())
    }

    @Test
    fun defaultLocaleDeclaresTheAppName() {
        assertEquals("Zolvex", english["app_name"])
    }

    @Test
    fun noValueIsBlank() {
        for ((locale, strings) in allLocales()) {
            for ((key, value) in strings) {
                assertFalse("[$locale] `$key` is blank", value.isBlank())
            }
        }
    }

    @Test
    fun noValueIsJustTheKeyName() {
        for ((locale, strings) in allLocales()) {
            for ((key, value) in strings) {
                if (key in KEYS_WHOSE_VALUE_MAY_EQUAL_THE_KEY) continue
                assertNotEquals(
                    "[$locale] `$key` was shipped as its own key name instead of a translation",
                    key,
                    value,
                )
            }
        }
    }

    /**
     * A placeholder missing from one translation is a `IllegalFormatException` at runtime in that
     * language only, which is exactly the class of bug a parity test should own.
     */
    @Test
    fun placeholdersMatchAcrossLocales() {
        for (key in english.keys) {
            val expected = placeholders(english.getValue(key))
            assertEquals(
                "[fr] `$key` placeholders differ from English",
                expected,
                placeholders(french.getValue(key)),
            )
            assertEquals(
                "[ar] `$key` placeholders differ from English",
                expected,
                placeholders(arabic.getValue(key)),
            )
        }
    }

    @Test
    fun everyLocaleCoversThePlannedKeyCount() {
        for ((locale, strings) in allLocales()) {
            assertTrue(
                "[$locale] declares only ${strings.size} strings, expected at least $EXPECTED_KEY_COUNT",
                strings.size >= EXPECTED_KEY_COUNT,
            )
        }
    }

    private fun allLocales(): List<Pair<String, Map<String, String>>> = listOf(
        "en" to english,
        "fr" to french,
        "ar" to arabic,
    )

    private fun placeholders(value: String): List<String> =
        PLACEHOLDER.findAll(value).map { it.value }.toList()

    private fun readStrings(file: Path): Map<String, String> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isExpandEntityReferences = false
            // There is no doctype in our resources; refusing one removes the XXE surface entirely.
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        val elements = factory.newDocumentBuilder().parse(file.toFile()).getElementsByTagName("string")
        val strings = LinkedHashMap<String, String>(elements.length)
        for (index in 0 until elements.length) {
            val element = elements.item(index) as Element
            strings[element.getAttribute("name")] = element.textContent
        }
        return strings
    }

    private fun resourceFile(localeDirectory: String): Path {
        val file = resRoot().resolve(localeDirectory).resolve("strings.xml")
        assertTrue("Missing resource file: $file", Files.isRegularFile(file))
        return file
    }

    /**
     * Unit tests run with the working directory set to the Gradle project directory, which is the
     * module directory for AGP tests — but `user.dir` is not contractually the module, so walk up
     * until a `res` directory is found rather than assuming a fixed relative path.
     */
    private fun resRoot(): Path {
        var directory: Path? = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
        while (directory != null) {
            for (candidate in listOf(
                directory.resolve("src/main/res"),
                directory.resolve("app/src/main/res"),
            )) {
                if (Files.isDirectory(candidate)) return candidate
            }
            directory = directory.parent
        }
        throw AssertionError("No src/main/res found above ${System.getProperty("user.dir")}")
    }

    private companion object {
        /**
         * How many keys every locale must declare, as a running total raised whenever a feature adds
         * strings. It started from PLAN.md section 11.3's flat key list and the update service adds
         * its four notification and twelve settings strings on top. Keep it equal to the real
         * per-locale count, so a locale that lags behind fails here rather than rendering a raw key.
         */
        const val EXPECTED_KEY_COUNT = 193

        /**
         * `years` is the English unit label — "12 years" — so its value is legitimately spelled
         * exactly like its key. Every other key must not be.
         */
        val KEYS_WHOSE_VALUE_MAY_EQUAL_THE_KEY = setOf("years")

        /** Matches `%s`, `%d`, `%1$s`, `%2$d` — the positional and non-positional forms. */
        val PLACEHOLDER = Regex("%(\\d+\\\$)?[sd]")
    }
}
