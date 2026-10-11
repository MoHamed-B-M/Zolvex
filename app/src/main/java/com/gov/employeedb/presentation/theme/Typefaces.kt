package com.gov.employeedb.presentation.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.gov.employeedb.R

/**
 * Builds a [FontFamily] from a single variable font file.
 *
 * Every weight points at the same `wght` axis instance instead of at a separate static file,
 * which keeps the APK smaller than shipping four static weights per family. Compose applies the
 * variation settings through `Typeface.Builder#setFontVariationSettings` on API 26+, which is
 * exactly our `minSdk`.
 */
@OptIn(ExperimentalTextApi::class)
private fun variableFontFamily(resId: Int): FontFamily = FontFamily(
    Font(
        resId,
        FontWeight.Normal,
        FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        resId,
        FontWeight.Medium,
        FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        resId,
        FontWeight.SemiBold,
        FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        resId,
        FontWeight.Bold,
        FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

/** Inter — Latin script (English and French). OFL, see `assets/licenses/OFL_Inter.txt`. */
val InterFamily: FontFamily = variableFontFamily(R.font.inter_variable)

/** Cairo — Arabic script, with a Latin fallback. OFL, see `assets/licenses/OFL_Cairo.txt`. */
val CairoFamily: FontFamily = variableFontFamily(R.font.cairo_variable)
