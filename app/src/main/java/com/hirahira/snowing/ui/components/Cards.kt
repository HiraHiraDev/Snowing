package com.hirahira.snowing.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hirahira.snowing.ui.theme.SnowingTheme

@Composable
fun SnowingCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SnowingTheme.shapes.card,
        color = SnowingTheme.colors.surface,
        contentColor = SnowingTheme.colors.content,
    ) {
        Column(
            modifier = Modifier.padding(SnowingTheme.spacing.m),
            verticalArrangement = Arrangement.spacedBy(SnowingTheme.spacing.s),
            content = content,
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, style = SnowingTheme.typography.title)
}

/** A card that asks the user to do something, with one action. */
@Composable
fun NoticeCard(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SnowingCard(modifier) {
        Text(text = title, style = SnowingTheme.typography.title, color = SnowingTheme.colors.attention)
        Text(text = body, style = SnowingTheme.typography.body, color = SnowingTheme.colors.contentMuted)
        PrimaryButton(text = actionLabel, onClick = onAction)
    }
}
