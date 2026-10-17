package com.hirahira.snowing.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.hirahira.snowing.ui.theme.SnowingTheme
import kotlin.math.roundToInt

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SnowingTheme.sizes.touchTarget),
        enabled = enabled,
        shape = SnowingTheme.shapes.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = SnowingTheme.colors.ctaFill,
            contentColor = SnowingTheme.colors.ctaDot,
        ),
    ) {
        Text(text = text, style = SnowingTheme.typography.rowTitle)
    }
}

/** A named slider over a continuous range, with its value shown as [valueText]. */
@Composable
fun SettingSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueText: String? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SnowingTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ControlLabel(label, Modifier.weight(1f))
            if (valueText != null) ControlValue(valueText)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            onValueChangeFinished = onValueChangeFinished,
        )
    }
}

/**
 * A named slider over whole numbers, e.g. a level 1-10. Short ranges show a mark per step;
 * long ones (degrees) slide freely and round.
 */
@Composable
fun StepSlider(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    valueText: String = value.toString(),
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val count = range.count()
    SettingSlider(
        label = label,
        value = value.toFloat(),
        onValueChange = { onValueChange(it.roundToInt()) },
        modifier = modifier,
        valueText = valueText,
        valueRange = range.first.toFloat()..range.last.toFloat(),
        steps = if (count <= MAX_MARKED_STEPS) count - 2 else 0,
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
    )
}

private const val MAX_MARKED_STEPS = 12

@Composable
fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SnowingTheme.sizes.touchTarget)
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch),
        horizontalArrangement = Arrangement.spacedBy(SnowingTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlLabel(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}
