package com.gov.employeedb.data.remote

import com.gov.employeedb.BuildConfig
import com.gov.employeedb.data.preferences.PreferencesRepository
import com.gov.employeedb.domain.model.UpdateCheckResult
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The checker's contract (updateservice.md Section 4): the ranking rules the release feed is judged
 * by, the 24-hour cooldown that keeps a relaunch off the network, and the promise that a bad
 * response comes back as [UpdateCheckResult.Error] rather than a crash.
 *
 * Robolectric supplies the real `org.json` parser, which matters here: the plain-JVM stub of an
 * Android class would swallow a malformed body and this test would never see the exception the
 * checker is supposed to catch. The HTTP path is driven by an interceptor that answers with a
 * canned body instead of a mock server, so no dependency beyond the ones the plan allows is needed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UpdateCheckerTest {

    /** Every URL the checker actually asked for, so a test can prove that no call was made. */
    private val requests = mutableListOf<String>()

    private var responseCode: Int = 200
    private var responseBody: String = "{}"

    private val client = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.SECONDS)
        .addInterceptor(Interceptor { chain ->
            val request = chain.request()
            requests += request.url.toString()
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(responseCode)
                .message(if (responseCode == 200) "OK" else "Error")
                .body(responseBody.toResponseBody(JSON_MEDIA_TYPE))
                .build()
        })
        .build()

    private val preferences = mockk<PreferencesRepository>(relaxed = true)

    private fun stubPreferences(lastCheck: Long? = null, prereleases: Boolean = false) {
        every { preferences.lastUpdateCheckTime } returns flowOf(lastCheck)
        every { preferences.includePrereleases } returns flowOf(prereleases)
    }

    private fun newChecker(): UpdateChecker = UpdateChecker(
        okHttpClient = client,
        preferencesRepository = preferences,
        dispatcher = Dispatchers.Unconfined,
    )

    private fun release(tagName: String, prerelease: Boolean = false): String =
        """{"tag_name":"$tagName","name":"$tagName","body":"notes","html_url":"https://example.com/$tagName","prerelease":$prerelease}"""

    private fun prereleaseFeed(tagName: String): String =
        """[{"tag_name":"$tagName","name":"$tagName","body":"notes","html_url":"https://example.com/$tagName","prerelease":true}]"""

    @Test
    fun `the version comparator ranks the forms the release feed uses`() {
        val checker = newChecker()

        assertThat(checker.compareVersions("1.1.0", "1.0.0")).isGreaterThan(0)
        assertThat(checker.compareVersions("1.0.0", "1.1.0")).isLessThan(0)
        assertThat(checker.compareVersions("1.0.0", "1.0.0")).isEqualTo(0)
        assertThat(checker.compareVersions("1.0.0", "1.0.0-rc1")).isGreaterThan(0)
        assertThat(checker.compareVersions("1.0.0-rc1", "1.0.0")).isLessThan(0)
        assertThat(checker.compareVersions("2.0.0", "1.9.9")).isGreaterThan(0)
        assertThat(checker.compareVersions("1.0", "1.0.0")).isEqualTo(0)
        assertThat(checker.compareVersions("1.0.0.1", "1.0.0")).isGreaterThan(0)
        assertThat(checker.compareVersions("v1.0.0", "1.0.0")).isEqualTo(0)
    }

    @Test
    fun `a check inside the cooldown never reaches the network`() = runTest {
        stubPreferences(lastCheck = System.currentTimeMillis())
        val checker = newChecker()

        val result = checker.checkForUpdate(force = false)

        assertThat(result).isEqualTo(UpdateCheckResult.UpToDate)
        assertThat(requests).isEmpty()
        coVerify(exactly = 0) { preferences.setLastUpdateCheckTime(any()) }
    }

    @Test
    fun `a forced check ignores the cooldown and records when it ran`() = runTest {
        stubPreferences(lastCheck = System.currentTimeMillis())
        responseBody = release(newerTag())
        val checker = newChecker()

        val result = checker.checkForUpdate(force = true)

        assertThat(result).isInstanceOf(UpdateCheckResult.UpdateAvailable::class.java)
        assertThat(requests).hasSize(1)
        coVerify(exactly = 1) { preferences.setLastUpdateCheckTime(any()) }
    }

    @Test
    fun `a malformed body comes back as an error instead of a crash`() = runTest {
        stubPreferences(lastCheck = null)
        responseBody = "not json"
        val checker = newChecker()

        val result = checker.checkForUpdate(force = false)

        assertThat(result).isInstanceOf(UpdateCheckResult.Error::class.java)
        assertThat((result as UpdateCheckResult.Error).message).isNotEmpty()
    }

    @Test
    fun `a failed status comes back as an error`() = runTest {
        stubPreferences(lastCheck = null)
        responseCode = 503
        responseBody = "service unavailable"
        val checker = newChecker()

        val result = checker.checkForUpdate(force = false)

        assertThat(result).isInstanceOf(UpdateCheckResult.Error::class.java)
        assertThat(requests).hasSize(1)
    }

    @Test
    fun `the stable feed is used when pre-releases are excluded`() = runTest {
        stubPreferences(lastCheck = null, prereleases = false)
        responseBody = release(BuildConfig.VERSION_NAME)
        val checker = newChecker()

        val result = checker.checkForUpdate(force = false)

        assertThat(result).isEqualTo(UpdateCheckResult.UpToDate)
        assertThat(requests.single()).endsWith("/releases/latest")
    }

    @Test
    fun `a newer pre-release is offered when pre-releases are included`() = runTest {
        stubPreferences(lastCheck = null, prereleases = true)
        responseBody = prereleaseFeed("${newerTag()}-rc1")
        val checker = newChecker()

        val result = checker.checkForUpdate(force = false)

        assertThat(result).isInstanceOf(UpdateCheckResult.UpdateAvailable::class.java)
        assertThat(requests.single()).endsWith("/releases?per_page=1")
    }

    /**
     * A tag strictly above whatever version the app is currently built as, so the "an update
     * exists" cases keep meaning that when the version is bumped.
     */
    private fun newerTag(): String = "v${BuildConfig.VERSION_NAME}.999"

    private companion object {
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
