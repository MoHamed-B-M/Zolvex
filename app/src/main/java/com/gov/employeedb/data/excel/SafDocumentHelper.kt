package com.gov.employeedb.data.excel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * SAF plumbing for the import/export flows (PLAN.md Phase-5 checklist): opening a picked document,
 * creating an export file without a picker, resolving a display name, and writing to a chooser Uri.
 *
 * The picker-based paths (Phase 7/10/11) hand out a `CreateDocument` Uri from
 * `ActivityResultContracts.CreateDocument`; the use cases pass it straight to [openOutput], so the
 * permission model stays "no storage permission, ever".
 */
object SafDocumentHelper {

    const val EXPORT_DIR = "exports"

    fun openInput(context: Context, uri: Uri): InputStream? = context.contentResolver.openInputStream(uri)

    fun openOutput(context: Context, uri: Uri): OutputStream? = context.contentResolver.openOutputStream(uri)

    /** The user-visible file name via [OpenableColumns.DISPLAY_NAME], falling back to the path. */
    fun displayName(context: Context, uri: Uri): String {
        var name: String? = null
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) name = cursor.getString(0)
        }
        return name ?: uri.lastPathSegment ?: DEFAULT_NAME
    }

    /**
     * Creates a `.xlsx` under the app's private `files/exports` directory and returns its Uri — a
     * permission-free sink for exports that do not need a picker. The file is app-private, so it is
     * cleaned on uninstall and never shows up in other apps' pickers.
     */
    fun createDocument(context: Context, name: String): Uri? {
        val dir = File(context.filesDir, EXPORT_DIR).apply { mkdirs() }
        val target = File(dir, sanitizeFileName(name))
        val parent = DocumentFile.fromFile(dir) ?: return null
        val created = parent.createFile(ExcelMime.XLSX_MIME, target.name) ?: return null
        return created.uri
    }

    private fun sanitizeFileName(name: String): String {
        val cleaned = name.replace(INVALID_FILENAME_CHARS, "-").trim('-')
        return if (cleaned.isBlank()) DEFAULT_NAME else cleaned
    }

    private const val DEFAULT_NAME = "export.xlsx"

    private val INVALID_FILENAME_CHARS = Regex("[^A-Za-z0-9._\\-\\u0600-\\u06FF ]")
}