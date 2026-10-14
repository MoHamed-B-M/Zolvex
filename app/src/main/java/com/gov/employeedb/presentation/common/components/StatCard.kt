package com.gov.employeedb.presentation.common.components

import androidx.annotation.StringRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.gov.employeedb.R
import com.gov.employeedb.core.NumberFormatters
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants

/**
 * One dashboard statistic (PLAN.md Phase-9 checklist): an icon, a label that wraps to two lines and
 * a large number that counts up to its value instead of appearing at once.
 *
 * There are two overloads because the statistics are of two kinds: whole counts (employees,
 * retirements) animate as an [Int], while the averages animate as a [Double] and are rendered with
 * one decimal place. A `null` average has nothing to count to, so the card shows a dash.
 *
 * The entry animation is deliberately inlined rather than extracted into a helper: `animateIntAsState`
 * starts at its target, so the value is forced to zero for the first frame and then released by a
 * state change in *this* scope. A helper returning the flag would recompose only itself, leaving the
 * number stuck at zero. Previews skip the entry so a reviewer sees the settled card.
 */
@Composable
fun StatCard(
    icon: ImageVector,
    @StringRes labelRes: Int,
    value: Int,
    modifier: Modifier = Modifier,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val animated by animateIntAsState(
        targetValue = if (LocalInspectionMode.current || entered) value else 0,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "statCountUp",
    )
    StatCardFrame(icon = icon, labelRes = labelRes, text = animated.toString(), modifier = modifier)
}

/** The decimal variant of [StatCard]; a `null` [value] renders as a dash rather than as `0.0`. */
@Composable
fun StatCard(
    icon: ImageVector,
    @StringRes labelRes: Int,
    value: Double?,
    modifier: Modifier = Modifier,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val animated by animateFloatAsState(
        targetValue = if (LocalInspectionMode.current || entered) (value ?: 0.0).toFloat() else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "statCountUpDecimal",
    )
    val text = if (value == null) NO_VALUE else NumberFormatters.oneDecimal(animated.toDouble())
    StatCardFrame(icon = icon, labelRes = labelRes, text = text, modifier = modifier)
}

@Composable
private fun StatCardFrame(
    icon: ImageVector,
    @StringRes labelRes: Int,
    text: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

/** The typographic stand-in for a statistic that could not be computed. */
private const val NO_VALUE = "\u2014"

@Preview(name = "StatCard - count")
@Composable
private fun StatCardCountPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        StatCard(
            icon = Icons.Outlined.People,
            labelRes = R.string.total_employees,
            value = 128,
        )
    }
}

@Preview(name = "StatCard - average")
@Composable
private fun StatCardAveragePreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        StatCard(
            icon = Icons.Outlined.CalendarMonth,
            labelRes = R.string.average_age,
            value = 34.7,
        )
    }
}

@Preview(name = "StatCard - no value")
@Composable
private fun StatCardNoValuePreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        StatCard(
            icon = Icons.Outlined.CalendarMonth,
            labelRes = R.string.average_age,
            value = null,
        )
    }
}
