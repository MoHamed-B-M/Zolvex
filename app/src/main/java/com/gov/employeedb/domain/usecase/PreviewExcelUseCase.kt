package com.gov.employeedb.domain.usecase

import android.content.Context
import android.net.Uri
import com.gov.employeedb.core.AppError
import com.gov.employeedb.core.AppResult
import com.gov.employeedb.data.excel.ExcelMime
import com.gov.employeedb.data.excel.ExcelPreview
import com.gov.employeedb.data.excel.ExcelReader
import com.gov.employeedb.data.excel.SafDocumentHelper
import com.gov.employeedb.data.preferences.MappingProfileStore
import com.gov.employeedb.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Step 1→3 of the Excel wizard (PLAN.md §6.1): opens the picked SAF document, previews the first
 * sheet, detects the mapping, and silently re-applies a stored mapping profile for an identical
 * header layout (PLAN.md §6.2 rule 6). Non-`.xlsx` names fail fast with [AppError.UnsupportedFile].
 */
class PreviewExcelUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reader: ExcelReader,
    private val profileStore: MappingProfileStore,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(uri: Uri): AppResult<ExcelPreview> = withContext(dispatcher) {
        val name = SafDocumentHelper.displayName(context, uri)
        if (!ExcelMime.isXlsxName(name)) {
            return@withContext AppResult.Failure(AppError.UnsupportedFile(name))
        }
        val input = SafDocumentHelper.openInput(context, uri)
        if (input == null) {
            return@withContext AppResult.Failure(AppError.FileRead(name, "cannot open input"))
        }
        return@withContext input.use { stream ->
            val previewResult = reader.preview(stream)
            val preview = when (previewResult) {
                is AppResult.Success -> previewResult.data
                is AppResult.Failure -> return@withContext previewResult
            }
            val stored = profileStore.load(preview.headers).first()
            if (stored != null && stored.isNotEmpty()) {
                AppResult.Success(preview.copy(detectedMapping = stored))
            } else {
                AppResult.Success(preview)
            }
        }
    }
}