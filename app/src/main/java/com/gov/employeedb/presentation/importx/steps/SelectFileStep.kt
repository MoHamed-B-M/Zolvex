package com.gov.employeedb.presentation.importx.steps

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gov.employeedb.R
import com.gov.employeedb.core.AppError
import com.gov.employeedb.data.excel.ExcelMime
import com.gov.employeedb.presentation.importx.messageRes

/**
 * Step 1 of the wizard (PLAN.md Phase-7 checklist): a dashed drop-card that opens the SAF picker
 * restricted to spreadsheets, the chosen file's name, and the `.xlsx`-only hint.
 *
 * The card is the only interactive element before a file exists; a failed preview turns into an
 * inline message with the same picker one tap away, so a bad pick never traps the user.
 */
@Composable
fun SelectFileStep(
    fileName: String?,
    isReading: Boolean,
    error: AppError?,
    onFileSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) onFileSelected(uri)
    }
    val pick = { launcher.launch(ExcelMime.OPEN_TYPES) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        UploadCard(onClick = pick)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.select_excel_file),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.unsupported_file_type),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (fileName != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        if (isReading) {
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator()
        }
        if (error != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = pick) {
                Text(text = stringResource(R.string.retry))
            }
        }
    }
}

/** The dashed-border drop card; the dashes are drawn behind the content so the icon stays sharp. */
@Composable
private fun UploadCard(onClick: () -> Unit) {
    val outline = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onClick)
            .drawBehind {
                drawRoundRect(
                    color = outline,
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = CornerRadius(CORNER_RADIUS.toPx()),
                    style = Stroke(
                        width = STROKE_WIDTH.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(DASH.toPx(), GAP.toPx()),
                        ),
                    ),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.UploadFile,
                contentDescription = stringResource(R.string.cd_import),
                modifier = Modifier.size(56.dp),
                tint = outline,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.select_file),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private val CORNER_RADIUS = 24.dp
private val STROKE_WIDTH = 2.dp
private val DASH = 24.dp
private val GAP = 16.dp
