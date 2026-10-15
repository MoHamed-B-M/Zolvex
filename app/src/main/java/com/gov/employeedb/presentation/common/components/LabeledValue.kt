package com.gov.employeedb.presentation.common.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants

/**
 * A caption over a figure (PLAN.md Phase-7 checklist: the Result step's running totals). The label
 * is small and muted, the value large and prominent, so a column of these reads as a scoreboard.
 */
@Composable
fun LabeledValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(name = "LabeledValue")
@Composable
private fun LabeledValuePreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        LabeledValue(label = "Imported", value = "1,024")
    }
}

/**
 * The animated face of the same caption-over-figure: the figure counts up to [value] instead of
 * appearing at once, for totals that are the payoff of a long operation (PLAN.md §8 row 19).
 */
@Composable
fun LabeledValue(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        CountUpText(value = value, style = MaterialTheme.typography.headlineSmall)
    }
}

@Preview(name = "LabeledValue - animated")
@Composable
private fun LabeledValueAnimatedPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        LabeledValue(label = "Imported", value = 1024)
    }
}
