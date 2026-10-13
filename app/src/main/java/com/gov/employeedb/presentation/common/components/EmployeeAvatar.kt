package com.gov.employeedb.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gov.employeedb.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants
import kotlin.math.abs

/** An eight-entry tonal palette; the name picks an index so a person keeps the same colour. */
private val AvatarPalette = listOf(
    Color(0xFF00696B),
    Color(0xFF3F6653),
    Color(0xFF00658C),
    Color(0xFF6750A4),
    Color(0xFF7D5260),
    Color(0xFF8B5000),
    Color(0xFF386A20),
    Color(0xFF984061),
)

/**
 * An initials avatar (PLAN.md Phase-6 checklist): the first letters of up to two name words on a
 * background colour derived deterministically from the name, so no image loading is needed.
 */
@Composable
fun EmployeeAvatar(
    fullName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val color = AvatarPalette[abs(fullName.hashCode()) % AvatarPalette.size]
    val description = stringResource(R.string.cd_avatar)
    Box(
        modifier = modifier
            .size(size)
            .background(color, CircleShape)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsOf(fullName),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** The first letter of the first two words, uppercased; empty for a blank name. */
internal fun initialsOf(fullName: String): String =
    fullName.trim()
        .split(WHITESPACE)
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString(separator = "") { it.first().uppercaseChar().toString() }

private val WHITESPACE = Regex("\\s+")

@Preview(name = "EmployeeAvatar")
@Composable
private fun EmployeeAvatarPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        EmployeeAvatar(fullName = "Amina Haddad")
    }
}
