package com.dreiz.kit.ui.theme.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dreiz.kit.ui.theme.ClayHighlightLight
import com.dreiz.kit.ui.theme.ClayInnerShadowDark
import com.dreiz.kit.ui.theme.ClayShadowDark
import com.dreiz.kit.ui.theme.innerShadow
import com.dreiz.kit.ui.theme.dropShadow

@Composable
fun ClayContainer(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    cornerRadius: Dp = 24.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "clay_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .dropShadow(
                shapeRadius = cornerRadius,
                color = ClayHighlightLight,
                offsetX = (-6).dp,
                offsetY = (-6).dp,
                blurRadius = 12.dp
            )
            .dropShadow(
                shapeRadius = cornerRadius,
                color = ClayShadowDark,
                offsetX = 6.dp,
                offsetY = 6.dp,
                blurRadius = 12.dp
            )
            .innerShadow(
                shapeRadius = cornerRadius,
                color = ClayInnerShadowDark,
                offsetX = 4.dp,
                offsetY = 4.dp,
                blurRadius = 20.dp
            )
            .then(onClick?.let { 
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = it
                )
            } ?: Modifier),
        content = content
    )
}
