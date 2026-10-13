package com.gov.employeedb.data.excel

import com.gov.employeedb.core.AppError
import com.gov.employeedb.core.AppResult
import java.io.InputStream
import java.util.concurrent.LinkedBlockingQueue
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.ss.usermodel.Comment
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable
import org.apache.poi.xssf.eventusermodel.XSSFReader
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler
import org.apache.poi.xssf.model.StylesTable
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler

/** One spreadsheet row as it streams out of the SAX reader: 0-based [cells] plus its 1-based row. */
data class StreamedRow(
    val cells: List<String?>,
    val sourceRow: Int,
)

/**
 * Memory-safe `.xlsx` reading (PLAN.md §6.3): `OPCPackage` + [XSSFReader] +
 * [ReadOnlySharedStringsTable] + [XSSFSheetXMLHandler] over SAX. `XSSFWorkbook`/`WorkbookFactory`/
 * HSSF are never used — they build the full sheet DOM and OOM at 100k rows. One row is materialized
 * at a time, the package is closed in `finally` on every path, and cancellations stay prompt.
 */
class ExcelReader(
    private val dispatcher: CoroutineDispatcher,
) {

    /**
     * Scans the first sheet: locates the header row, samples the first data rows, counts the sheet
     * and runs [ColumnDetector]. Any open/parse failure yields [AppError.FileRead] so the wizard can
     * return to Step 1 with a retry (PLAN.md §6.4).
     */
    fun preview(input: InputStream): AppResult<ExcelPreview> = try {
        val pkg = OPCPackage.open(input)
        try {
            val reader = XSSFReader(pkg)
            val sheetsIt = reader.sheetsData
            if (!sheetsIt.hasNext()) {
                return AppResult.Failure(AppError.FileRead(null, "empty workbook"))
            }
            val sheetName = try {
                val m = sheetsIt.javaClass.getMethod("getSheetName")
                m.invoke(sheetsIt) as? String ?: ""
            } catch (_: Exception) { "" }
            val sheetPart = try {
                val m = sheetsIt.javaClass.getMethod("getSheetPart")
                m.invoke(sheetsIt) as? org.apache.poi.openxml4j.opc.PackagePart
            } catch (_: Exception) { null }
            val scan = scanSheet(
                source = (sheetPart ?: error("no sheet")).inputStream,
                shared = ReadOnlySharedStringsTable(pkg),
                styles = reader.stylesTable,
                collectRows = true,
            )
            val rows = scan.rows
            if (rows.isEmpty()) {
                return AppResult.Failure(AppError.FileRead(sheetName, "no rows found"))
            }
            val header = ColumnDetector.findHeaderRow(rows)
            val headers = rows[header.index].map { it.orEmpty() }
            val sampleRows = rows.drop(header.index + 1)
            val detection = ColumnDetector.detect(headers, sampleRows)
            AppResult.Success(
                ExcelPreview(
                    sheetName = sheetName,
                    headerRowIndex = header.index,
                    headers = headers,
                    detectedMapping = detection.mapping,
                    sampleRows = sampleRows,
                    totalRowEstimate = (scan.totalRows - (header.index + 1)).coerceAtLeast(0),
                ),
            )
        } finally {
            pkg.close()
        }
    } catch (t: Throwable) {
        if (t is CancellationException) throw t
        AppResult.Failure(AppError.FileRead(null, t.message ?: t.javaClass.simpleName))
    }

    /**
     * Streams every non-empty row of the first sheet, back-pressured through a bounded queue so the
     * SAX pass never outruns the collector. `delay(1)` every 100 rows keeps the UI thread's events
     * flowing and makes cancellation prompt (PLAN.md §6.3).
     */
    fun streamRows(input: InputStream): Flow<StreamedRow> = flow {
        val queue = LinkedBlockingQueue<StreamedRow?>(STREAM_BUFFER)
        coroutineScope {
            launch {
                var failure: Throwable? = null
                try {
                    val pkg = OPCPackage.open(input)
                    try {
                        val reader = XSSFReader(pkg)
                        val sheetsIt = reader.sheetsData
                        if (sheetsIt.hasNext()) {
                            val handler = SheetHandler(
                                styles = reader.stylesTable,
                                shared = ReadOnlySharedStringsTable(pkg),
                                collectRows = false,
                            ) { rowNumber, cells -> queue.put(StreamedRow(cells, rowNumber)) }
                            val sheetPart = try {
                                sheetsIt.javaClass.getMethod("getSheetPart").invoke(sheetsIt) as? org.apache.poi.openxml4j.opc.PackagePart
                            } catch (_: Exception) { null }
                            parseSheet((sheetPart ?: error("no sheet")).inputStream, handler)
                        }
                    } finally {
                        pkg.close()
                    }
                } catch (t: Throwable) {
                    failure = t
                } finally {
                    // Unblock the collector before rethrowing so the flow fails cleanly.
                    queue.put(null)
                }
                failure?.let { throw it }
            }
            var processed = 0
            while (true) {
                val row = queue.take() ?: break
                emit(row)
                processed++
                if (processed % PROGRESS_YIELD_EVERY == 0) delay(1)
            }
        }
    }.flowOn(dispatcher)

    /** One SAX pass over the sheet: counts `<row>`s and, when asked, captures the first rows. */
    private fun scanSheet(
        source: InputStream,
        shared: ReadOnlySharedStringsTable,
        styles: StylesTable,
        collectRows: Boolean,
    ): SheetScan {
        val handler = SheetHandler(styles, shared, collectRows) { _, _ -> /* stream-only callback */ }
        parseSheet(source, handler)
        return SheetScan(totalRows = handler.totalRows, rows = handler.capturedRows.toList())
    }

    private fun parseSheet(source: InputStream, handler: DefaultHandler) {
        val spf = SAXParserFactory.newInstance()
        val parser: SAXParser = spf.newSAXParser()
        parser.parse(InputSource(source), handler)
    }

    /** Immutable result of a preview scan over the first sheet. */
    private class SheetScan(
        val totalRows: Int,
        val rows: List<List<String?>>,
    )

    /**
     * Forwards every SAX event to [XSSFSheetXMLHandler] (which owns the shared-string and style
     * resolution) while counting `<row>` elements and, when [collectRows], capturing the first rows
     * for the preview. Only rows with at least one non-blank cell are surfaced.
     */
    private class SheetHandler(
        styles: StylesTable,
        shared: ReadOnlySharedStringsTable,
        private val collectRows: Boolean,
        private val onRow: (Int, List<String?>) -> Unit,
    ) : DefaultHandler() {

        val totalRows: Int get() = total

        val capturedRows: MutableList<List<String?>> = mutableListOf()

        private var total = 0
        private var currentRow: MutableList<String?>? = null

        private val contentsHandler = object : XSSFSheetXMLHandler.SheetContentsHandler {
            override fun startRow(rowNum: Int) {
                currentRow = ArrayList()
            }

            override fun cell(cellReference: String, formattedValue: String, comment: Comment?) {
                val row = currentRow ?: return
                val column = cellReference.columnIndexFromReference()
                while (row.size <= column) row.add(null)
                row[column] = formattedValue
            }

            override fun headerFooter(text: String, isHeader: Boolean, tagName: String) {
                // no-op
            }

            override fun endRow(rowNum: Int) {
                val row = currentRow ?: return
                currentRow = null
                if (collectRows && capturedRows.size < PREVIEW_ROW_LIMIT) capturedRows += row.toList()
                if (row.any { it != null }) onRow(rowNum, row.toList())
            }
        }

        private val delegate = XSSFSheetXMLHandler(
            styles,
            shared,
            contentsHandler,
            DataFormatter(),
            /* formulasNotResults = */ false,
        )

        override fun startDocument() = delegate.startDocument()

        override fun endDocument() = delegate.endDocument()

        override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes) {
            if (elementName(localName, qName) == "row") total++
            delegate.startElement(uri ?: "", localName ?: "", qName ?: "", attributes)
        }

        override fun endElement(uri: String?, localName: String?, qName: String?) {
            delegate.endElement(uri ?: "", localName ?: "", qName ?: "")
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            delegate.characters(ch, start, length)
        }

        private fun elementName(localName: String?, qName: String?): String =
            if (!localName.isNullOrEmpty()) localName else qName ?: ""
    }

    companion object {
        const val PREVIEW_ROW_LIMIT = 10
        const val STREAM_BUFFER = 256
        const val PROGRESS_YIELD_EVERY = 100
    }
}

private fun String.columnIndexFromReference(): Int {
    var index = 0
    for (ch in this) {
        if (ch !in 'A'..'Z' && ch !in 'a'..'z') break
        index = index * 26 + (ch.uppercaseChar() - 'A' + 1)
    }
    return if (index == 0) 0 else index - 1
}