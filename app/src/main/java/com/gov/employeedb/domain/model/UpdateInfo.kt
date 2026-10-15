package com.gov.employeedb.domain.model

/**
 * Release information retrieved from GitHub Releases API.
 *
 * Only basic metadata needed to decide if an update is available and to
 * surface a notification with a link to the release page.
 */
data class UpdateInfo(
    val tagName: String,
    val name: String,
    val body: String,
    val htmlUrl: String,
    val downloadUrl: String? = null,
    val publishedAt: String? = null,
    val isPrerelease: Boolean = false,
    val isNewerThanLocal: Boolean = false,
)
