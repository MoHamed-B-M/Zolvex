package com.gov.employeedb.presentation.common.components

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.gov.employeedb.R
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants

/**
 * The two-button confirmation shown before anything irreversible (PLAN.md Phase-7 checklist, first
 * used by the Phase-8 detail screen's delete).
 *
 * Both labels are resources, and the confirm label carries the error colour when [destructive], so
 * "Delete" is never the visually inviting choice next to "Cancel".
 */
@Composable
fun ConfirmDialog(
    @StringRes titleRes: Int,
    @StringRes messageRes: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    @StringRes confirmRes: Int = R.string.delete,
    @StringRes dismissRes: Int = R.string.cancel,
    destructive: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(titleRes)) },
        text = { Text(text = stringResource(messageRes)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(confirmRes),
                    color = if (destructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        LocalContentColor.current
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(dismissRes))
            }
        },
    )
}

@Preview(name = "ConfirmDialog")
@Composable
private fun ConfirmDialogPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        ConfirmDialog(
            titleRes = R.string.delete_confirm_title,
            messageRes = R.string.delete_confirm_message,
            onConfirm = {},
            onDismiss = {},
        )
    }
}
