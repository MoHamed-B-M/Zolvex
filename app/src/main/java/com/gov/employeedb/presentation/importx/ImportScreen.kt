package com.gov.employeedb.presentation.importx

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.gov.employeedb.R
import com.gov.employeedb.presentation.common.components.EmptyState
import com.gov.employeedb.presentation.common.components.ScreenScaffold
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants

/**
 * The Excel import wizard: pick a file, map the columns, preview, commit.
 *
 * Phase 3 routes the destination so the graph is complete; the file picker, mapping UI and chunked
 * commit arrive in Phase 7 on top of the Phase 5 Excel engine.
 */
@Composable
fun ImportScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        titleRes = R.string.import_excel,
        onNavigateUp = onNavigateUp,
        modifier = modifier,
    ) { padding ->
        EmptyState(
            icon = Icons.Outlined.FileUpload,
            headlineRes = R.string.no_data,
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}

@Preview(name = "Import")
@Composable
private fun ImportScreenPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        ImportScreen(onNavigateUp = {})
    }
}