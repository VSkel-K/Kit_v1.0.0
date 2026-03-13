package com.dreiz.kit.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Extension function for inner shadow
fun Modifier.innerShadow(
    color: Color = Color.Black,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp,
    blurRadius: Dp = 0.dp,
    shapeRadius: Dp = 0.dp
) = this.drawBehind {
    drawIntoCanvas {
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = color.toArgb()
        frameworkPaint.setShadowLayer(
            blurRadius.toPx(),
            offsetX.toPx(),
            offsetY.toPx(),
            color.toArgb()
        )

        val rect = Rect(Offset.Zero, size)
        val path = Path().apply { addRoundRect(rect, shapeRadius.toPx(), shapeRadius.toPx()) }

        it.drawPath(path, paint)
    }
}

// Extension function for drop shadow (neumorphic style)
fun Modifier.dropShadow(
    color: Color = Color.Black,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp,
    blurRadius: Dp = 0.dp,
    shapeRadius: Dp = 0.dp
) = this.drawBehind {
    drawIntoCanvas {
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = Color.Transparent.toArgb() // Shadow only
        frameworkPaint.setShadowLayer(
            blurRadius.toPx(),
            offsetX.toPx(),
            offsetY.toPx(),
            color.toArgb()
        )

        val rect = Rect(Offset.Zero, size)
        val path = Path().apply { addRoundRect(rect, shapeRadius.toPx(), shapeRadius.toPx()) }

        it.drawPath(path, paint)
    }
}
