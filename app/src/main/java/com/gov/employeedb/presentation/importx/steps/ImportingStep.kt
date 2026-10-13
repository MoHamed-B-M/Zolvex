package com.gov.employeedb.presentation.importx.steps

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.gov.employeedb.R
import com.gov.employeedb.presentation.common.components.CountUpText
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants
import com.gov.employeedb.presentation.theme.AppMotion
import kotlin.math.roundToInt

/**
 * The progress face of the preview page (PLAN.md Phase-7 checklist): a ring that eases toward the
 * processed fraction, a count-up percentage, the live row counter, and Cancel.
 *
 * The ring follows [fraction] through a spring rather than snapping, so the streamed progress reads
 * as motion instead of as a number ticking in a static bar.
 */
@Composable
fun ImportingStep(
    fraction: Float,
    processed: Int,
    total: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(
        targetValue = fraction,
        animationSpec = AppMotion.spatial(),
        label = "importProgress",
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { animated },
                modifier = Modifier.size(PROGRESS_SIZE),
                strokeWidth = PROGRESS_STROKE,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CountUpText(
                value = (animated * 100f).roundToInt(),
                style = MaterialTheme.typography.displaySmall,
            )
            Text(text = PERCENT, style = MaterialTheme.typography.displaySmall)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.import_progress),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$processed$COUNT_SEPARATOR$total",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(onClick = onCancel) {
            Text(text = stringResource(R.string.cancel_import))
        }
    }
}

private const val PERCENT = "%"
private const val COUNT_SEPARATOR = " / "
private val PROGRESS_SIZE = 160.dp
private val PROGRESS_STROKE = 12.dp

@Preview(name = "ImportingStep")
@Composable
private fun ImportingStepPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        ImportingStep(fraction = 0.42f, processed = 54, total = 128, onCancel = {})
    }
}
