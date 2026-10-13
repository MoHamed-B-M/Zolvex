package com.gov.employeedb.presentation.employees.list.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.gov.employeedb.R
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants

/**
 * The pinned search field (PLAN.md Phase-6 checklist): a leading search icon, a trailing clear
 * button while there is text, and the platform "search" IME action. Text alignment is forced to
 * `Start` so the caret always sits on the leading edge, in both text directions.
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(text = stringResource(R.string.search_hint)) },
        leadingIcon = {
            Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.cd_clear),
                    )
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Start),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

@Preview(name = "SearchBar")
@Composable
private fun SearchBarPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        SearchBar(query = "ada", onQueryChange = {})
    }
}
