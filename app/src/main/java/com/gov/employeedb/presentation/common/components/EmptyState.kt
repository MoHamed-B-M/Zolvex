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
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
 * The centred "there is nothing here yet" block: a large muted icon, a short headline and an
 * optional line of detail.
 *
 * It expects to be given a full-size modifier (the caller decides the available space) — the
 * column centres itself inside whatever it is handed. The icon is decorative: the headline already
 * names the state, so a screen reader would only repeat itself.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    @StringRes headlineRes: Int,
    modifier: Modifier = Modifier,
    @StringRes supportingRes: Int? = null,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(headlineRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (supportingRes != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(supportingRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(name = "EmptyState — headline only")
@Composable
private fun EmptyStatePreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        EmptyState(
            icon = Icons.Outlined.People,
            headlineRes = R.string.empty_state_employees,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(name = "EmptyState — with supporting text")
@Composable
private fun EmptyStateSupportingPreview(
    @PreviewParameter(PreviewVariants::class) variant: PreviewVariant,
) {
    AppPreview(variant) {
        EmptyState(
            icon = Icons.Outlined.People,
            headlineRes = R.string.empty_state_analytics,
            modifier = Modifier.fillMaxSize(),
            supportingRes = R.string.backup_disabled_note,
        )
    }
}
