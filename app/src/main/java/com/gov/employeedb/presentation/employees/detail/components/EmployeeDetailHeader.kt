package com.gov.employeedb.presentation.employees.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.gov.employeedb.presentation.common.components.EmployeeAvatar
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants

/** Bigger than the list avatar, so the shared-element transition has a size change to animate. */
private val DETAIL_AVATAR_SIZE = 96.dp

/**
 * The detail screen's header (PLAN.md Phase-8 checklist): the large initials avatar, the name and
 * the position.
 *
 * [avatarModifier] and [nameModifier] arrive from the navigation graph carrying this destination's
 * shared-element modifiers, so the header is the far end of the transition that starts on the list
 * card. Both default to `Modifier`, which is what lets the header still render on its own here.
 *
 * The header stacks its content instead of using a row, so it reads the same way in a narrow
 * portrait window and in a wide landscape one, and the name is centred under the avatar.
 */
@Composable
fun EmployeeDetailHeader(
    fullName: String,
    position: String?,
    modifier: Modifier = Modifier,
    avatarModifier: Modifier = Modifier,
    nameModifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EmployeeAvatar(
            fullName = fullName,
            modifier = avatarModifier,
            size = DETAIL_AVATAR_SIZE,
        )
        Text(
            text = fullName,
            modifier = nameModifier,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (position != null) {
            Text(
                text = position,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(name = "EmployeeDetailHeader")
@Composable
private fun EmployeeDetailHeaderPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        EmployeeDetailHeader(fullName = "Amina Haddad", position = "Analyst")
    }
}

@Preview(name = "EmployeeDetailHeader - no position")
@Composable
private fun EmployeeDetailHeaderWithoutPositionPreview(
    @PreviewParameter(PreviewVariants::class) variant: PreviewVariant,
) {
    AppPreview(variant) {
        EmployeeDetailHeader(fullName = "Amine Benali", position = null)
    }
}
