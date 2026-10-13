package com.gov.employeedb.presentation.common.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants

/**
 * A number that animates to its new value instead of jumping (PLAN.md Phase-6 checklist). Used for
 * the list total and the import percentage, where the change is the feedback.
 */
@Composable
fun CountUpText(
    value: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
) {
    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "countUp",
    )
    Text(text = animated.toString(), modifier = modifier, style = style)
}

@Preview(name = "CountUpText")
@Composable
private fun CountUpTextPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        CountUpText(value = 128)
    }
}
