package com.gov.employeedb.presentation.common.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.gov.employeedb.BuildConfig
import com.gov.employeedb.R
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants

/**
 * The debug-only "seed test data" top-bar action (PLAN.md Phase-6 checklist, offered from the
 * dashboard as well in Phase 9). Tapping it fills the database with the deterministic seed dataset;
 * while that runs the icon becomes a spinner and the button is disabled, so a second tap cannot
 * start a second seed.
 *
 * The whole action is behind [BuildConfig.DEBUG], so a release build compiles it away and cannot
 * reach the seed path.
 */
@Composable
fun SeedTestDataAction(
    isSeeding: Boolean,
    onSeed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (BuildConfig.DEBUG) {
        IconButton(onClick = onSeed, modifier = modifier, enabled = !isSeeding) {
            if (isSeeding) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Science,
                    contentDescription = stringResource(R.string.seed_test_data),
                )
            }
        }
    }
}

@Preview(name = "SeedTestDataAction")
@Composable
private fun SeedTestDataActionPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        SeedTestDataAction(isSeeding = false, onSeed = {})
    }
}

@Preview(name = "SeedTestDataAction - seeding")
@Composable
private fun SeedTestDataActionSeedingPreview(
    @PreviewParameter(PreviewVariants::class) variant: PreviewVariant,
) {
    AppPreview(variant) {
        SeedTestDataAction(isSeeding = true, onSeed = {})
    }
}
