package com.hirahira.snowing.scrollapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/** A long, ordinary list: the kind of screen people scroll while it snows. */
class ScrollActivity : ComponentActivity() {

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // Exposes the tag as a resource id, so UiAutomator in :benchmark can find the list.
                LazyColumn(Modifier.semantics { testTagsAsResourceId = true }.testTag(LIST_TAG)) {
                    items(ITEM_COUNT) { index ->
                        ListItem(
                            headlineContent = { Text("Item $index") },
                            supportingContent = { Text("Some supporting text for row $index") },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    private companion object {
        const val LIST_TAG = "list"
        const val ITEM_COUNT = 1_000
    }
}
