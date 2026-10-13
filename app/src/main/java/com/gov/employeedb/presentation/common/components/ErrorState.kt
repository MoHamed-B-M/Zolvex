package com.gov.employeedb.presentation.common.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.gov.employeedb.R
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants

/**
 * The centred "the last read failed" block with a retry affordance. [messageRes] is a resource, not
 * a string, so the same component can carry any of the app's failure lines without a hardcoded
 * literal leaking in.
 */
@Composable
fun ErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes messageRes: Int = R.string.error_generic,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.retry))
        }
    }
}

@Preview(name = "ErrorState")
@Composable
private fun ErrorStatePreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        ErrorState(onRetry = {}, modifier = Modifier.fillMaxSize())
    }
}
