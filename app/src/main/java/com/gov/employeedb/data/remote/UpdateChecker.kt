package com.gov.employeedb.data.remote

import com.gov.employeedb.BuildConfig
import com.gov.employeedb.data.preferences.PreferencesRepository
import com.gov.employeedb.di.IoDispatcher
import com.gov.employeedb.domain.model.UpdateCheckResult
import com.gov.employeedb.domain.model.UpdateInfo
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

private const val GITHUB_API_ACCEPT = "application/vnd.github+json"
private const val GITHUB_API_VERSION = "2022-11-28"
private const val TWENTY_FOUR_HOURS_MS = 24L * 60L * 60L * 1000L

// TODO: replace with actual repository details when publishing a release
private const val GITHUB_OWNER = "MoHamed-B-M"
private const val GITHUB_REPO = "Zolvex"

/**
 * Checks GitHub Releases for a newer version of the app.
 */
@Singleton
class UpdateChecker @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val preferencesRepository: PreferencesRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    suspend fun checkForUpdate(force: Boolean): UpdateCheckResult = withContext(dispatcher) {
        try {
            if (!force) {
                val lastCheck = preferencesRepository.lastUpdateCheckTime.first()
                if (lastCheck != null && (System.currentTimeMillis() - lastCheck) < TWENTY_FOUR_HOURS_MS) {
                    return@withContext UpdateCheckResult.UpToDate
                }
            }

            val includePrereleases = preferencesRepository.includePrereleases.first()
            val remote = if (includePrereleases) fetchLatestReleaseOrPrerelease() else fetchLatestRelease()

            preferencesRepository.setLastUpdateCheckTime(System.currentTimeMillis())

            val localVersion = BuildConfig.VERSION_NAME.trim()
            val isNewer = isNewerVersion(remote.tagName, localVersion)
            val info = remote.copy(isNewerThanLocal = isNewer)

            if (isNewer) {
                UpdateCheckResult.UpdateAvailable(info)
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (e: IOException) {
            UpdateCheckResult.Error("Network error while checking for updates", e)
        } catch (e: JSONException) {
            UpdateCheckResult.Error("Failed to parse update response", e)
        } catch (e: Exception) {
            UpdateCheckResult.Error("Unexpected error while checking for updates", e)
        }
    }

    private suspend fun fetchLatestRelease(): UpdateInfo = withContext(dispatcher) {
        val url = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
        val request = Request.Builder()
            .url(url)
            .header("Accept", GITHUB_API_ACCEPT)
            .header("X-GitHub-Api-Version", GITHUB_API_VERSION)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code ${response.code}")
            val body = response.body?.string() ?: throw IOException("Empty response")
            parseRelease(JSONObject(body))
        }
    }

    private suspend fun fetchLatestReleaseOrPrerelease(): UpdateInfo = withContext(dispatcher) {
        val url = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases?per_page=1"
        val request = Request.Builder()
            .url(url)
            .header("Accept", GITHUB_API_ACCEPT)
            .header("X-GitHub-Api-Version", GITHUB_API_VERSION)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code ${response.code}")
            val body = response.body?.string() ?: throw IOException("Empty response")
            val array = JSONArray(body)
            if (array.length() == 0) throw IOException("No releases found")
            parseRelease(array.getJSONObject(0))
        }
    }

    private fun parseRelease(json: JSONObject): UpdateInfo {
        val tagName = json.optString("tag_name", "")
        val name = json.optString("name", tagName)
        val body = json.optString("body", "")
        val htmlUrl = json.optString("html_url", "")
        val publishedAt = json.optString("published_at", null)
        val isPrerelease = json.optBoolean("prerelease", false)
        val assets = json.optJSONArray("assets")
        var downloadUrl: String? = null
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val assetName = asset.optString("name", "")
                if (assetName.endsWith(".apk", ignoreCase = true)) {
                    downloadUrl = asset.optString("browser_download_url", null)
                    break
                }
            }
        }
        return UpdateInfo(
            tagName = tagName,
            name = name,
            body = body,
            htmlUrl = htmlUrl,
            downloadUrl = downloadUrl,
            publishedAt = publishedAt,
            isPrerelease = isPrerelease,
            isNewerThanLocal = false,
        )
    }

    /**
     * Very small semver-like comparison. Handles 1.2.3, 1.2.3.4, 1.2, optional 'v' prefix.
     * Stable release is considered greater than a pre-release of the same numeric version.
     */
    internal fun isNewerVersion(remote: String, local: String): Boolean {
        if (remote.isBlank() || local.isBlank()) return false
        val r = normalizeVersion(remote)
        val l = normalizeVersion(local)
        val rParts = parseParts(r.first)
        val lParts = parseParts(l.first)
        val max = maxOf(rParts.size, lParts.size)
        for (i in 0 until max) {
            val rv = rParts.getOrNull(i) ?: 0
            val lv = lParts.getOrNull(i) ?: 0
            if (rv != lv) return rv > lv
        }
        // Same numeric version; treat stable > prerelease
        val rIsPre = r.second
        val lIsPre = l.second
        if (rIsPre && !lIsPre) return false
        if (!rIsPre && lIsPre) return true
        return false
    }

    private fun normalizeVersion(v: String): Pair<String, Boolean> {
        var s = v.trim()
        if (s.startsWith("v", ignoreCase = true)) s = s.substring(1)
        val isPre = s.contains("-") || s.contains("rc", ignoreCase = true) || s.contains("beta", ignoreCase = true) || s.contains("alpha", ignoreCase = true)
        val core = s.substringBefore('-').substringBefore('+')
        return core to isPre
    }

    private fun parseParts(core: String): List<Int> {
        if (core.isEmpty()) return emptyList()
        return core.split('.').mapNotNull { it.takeWhile(Char::isDigit).toIntOrNull() }
    }
}
