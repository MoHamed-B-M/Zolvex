package com.gov.employeedb.presentation.common.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.gov.employeedb.R
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants

/**
 * The app's top bar: a title, and a back arrow whenever the destination is not a top-level tab.
 *
 * The arrow is the auto-mirrored one, so it points the other way in Arabic without any extra code.
 * The title is passed as a string resource rather than a string, so a screen cannot ship an
 * untranslated title by accident.
 */
@Composable
fun AppTopBar(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
    onNavigateUp: (() -> Unit)? = null,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(titleRes),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
        navigationIcon = {
            if (onNavigateUp != null) {
                IconButton(onClick = onNavigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                    )
                }
            }
        },
    )
}

@Preview(name = "AppTopBar — title only")
@Composable
private fun AppTopBarPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        AppTopBar(titleRes = R.string.dashboard)
    }
}

@Preview(name = "AppTopBar — with back arrow")
@Composable
private fun AppTopBarBackPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        AppTopBar(titleRes = R.string.employee_detail, onNavigateUp = {})
    }
}
