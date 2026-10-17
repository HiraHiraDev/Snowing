package com.hirahira.snowing.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.launch

/**
 * How everything tappable answers a touch (design 6a): no ripple, the pressed thing dims
 * softly and comes back. [SnowingTheme] provides it in place of Material's ripple, so every
 * clickable and selectable gets it without asking.
 */
internal object PressFade : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = PressFadeNode(interactionSource)

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = System.identityHashCode(this)
}

private class PressFadeNode(private val interactionSource: InteractionSource) : Modifier.Node(), DrawModifierNode {
    private val alpha = Animatable(1f)
    private val layerPaint = Paint()

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                val (target, millis) = when (interaction) {
                    is PressInteraction.Press -> PRESSED_ALPHA to PRESS_MILLIS
                    is PressInteraction.Release, is PressInteraction.Cancel -> 1f to RELEASE_MILLIS
                    else -> return@collect
                }
                launch { alpha.animateTo(target, tween(millis)) { invalidateDraw() } }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val value = alpha.value
        if (value >= 1f) {
            drawContent()
            return
        }
        layerPaint.alpha = value
        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(0f, 0f, size.width, size.height), layerPaint)
            drawContent()
            canvas.restore()
        }
    }
}

private const val PRESSED_ALPHA = 0.55f
private const val PRESS_MILLIS = 90
private const val RELEASE_MILLIS = 220
