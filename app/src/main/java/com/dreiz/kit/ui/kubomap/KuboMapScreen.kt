package com.dreiz.kit.ui.kubomap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.dreiz.kit.domain.kubomap.KuboDef
import com.dreiz.kit.domain.kubomap.KuboState
import com.dreiz.kit.ui.theme.AzulCobalto
import com.dreiz.kit.ui.theme.FondoOscuroClay
import com.dreiz.kit.ui.theme.GrisArcilla
import com.dreiz.kit.ui.theme.Hueso
import com.dreiz.kit.ui.theme.Mandarina
import com.dreiz.kit.ui.theme.components.ClayContainer
import kotlin.math.roundToInt

// ── TOKENS DE TEMA ────────────────────────────────────────────
// Usando la paleta de colores Claymorphic definida en Theme.kt
private val LineaMalla    = Color(0xFF1A2035) // Mantener para la malla, ajustar si es necesario
private val AcentoCyan    = Color(0xFF06B6D4) // Mantener por ahora, ajustar si es necesario
private val PurpleAccent  = Color(0xFF7C3AED) // Mantener por ahora, ajustar si es necesario

// ── PANTALLA PRINCIPAL ─────────────────────────────────────────

@Composable
fun KuboMapScreen(viewModel: KuboMapViewModel) {
    val state by viewModel.mapState.collectAsState()
    val density = LocalDensity.current
    var gridSize by remember { mutableStateOf(Size.Zero) }
    val kuboSizeDp = 72.dp
    val kuboSizePx = with(density) { kuboSizeDp.toPx() }

    val kuboSeleccionado = state.kubos.find { it.state == KuboState.ACTIVE }

    Row(modifier = Modifier.fillMaxSize().background(FondoOscuroClay)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .onGloballyPositioned { coords ->
                    gridSize = Size(coords.size.width.toFloat(), coords.size.height.toFloat())
                }
        ) {
            if (gridSize != Size.Zero) {
                val cellWidth  = gridSize.width  / 8f
                val cellHeight = gridSize.height / 4f

                CapaMalla(
                    cellWidth  = cellWidth,
                    cellHeight = cellHeight,
                    modifier   = Modifier.fillMaxSize()
                )

                CapaConexiones(
                    state.connections.mapNotNull { conn ->
                        val src = state.kubos.find { it.kuboId == conn.sourceKubo }
                        val tgt = state.kubos.find { it.kuboId == conn.targetKubo }
                        if (src != null && tgt != null) Triple(src, tgt, conn.type) else null
                    },
                    kuboSizePx = kuboSizePx,
                    modifier   = Modifier.fillMaxSize()
                )

                state.kubos.forEach { kubo ->
                    KuboInteractivo(
                        kubo       = kubo,
                        kuboSizeDp = kuboSizeDp,
                        kuboSizePx = kuboSizePx,
                        figureId   = state.slots.find { it.slotId == kubo.dominantSlot }?.figureId ?: "circle",
                        cellWidth  = cellWidth,
                        cellHeight = cellHeight,
                        onMove     = { dx, dy -> viewModel.moverKubo(kubo.kuboId, kubo.position.x + dx, kubo.position.y + dy) },
                        onDrop     = { viewModel.aplicarSoftSnap(kubo.kuboId, kubo.position.x, kubo.position.y, cellWidth, cellHeight, kuboSizePx) },
                        onTap      = { viewModel.seleccionarKubo(kubo.kuboId) }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = kuboSeleccionado != null,
            enter   = slideInHorizontally { it } + fadeIn(),
            exit    = slideOutHorizontally { it } + fadeOut()
        ) {
            kuboSeleccionado?.let { kubo ->
                PanelDetalleKubo(
                    kubo    = kubo,
                    onClose = { viewModel.deseleccionarTodos() }
                )
            }
        }
    }
}

@Composable
private fun CapaMalla(cellWidth: Float, cellHeight: Float, modifier: Modifier) {
    Canvas(modifier = modifier) {
        for (r in 0 until 4) {
            for (c in 0 until 8) {
                val x = c * cellWidth
                val y = r * cellHeight
                drawRect(
                    color    = LineaMalla,
                    topLeft  = Offset(x, y),
                    size     = Size(cellWidth, cellHeight),
                    style    = Stroke(1f)
                )
                val cx = x + cellWidth  / 2f
                val cy = y + cellHeight / 2f
                val arm = 10f
                val sw  = 1.5f
                drawLine(LineaMalla, Offset(cx - arm, cy), Offset(cx + arm, cy), sw)
                drawLine(LineaMalla, Offset(cx, cy - arm), Offset(cx, cy + arm), sw)
            }
        }
    }
}

@Composable
private fun CapaConexiones(
    conexiones: List<Triple<KuboDef, KuboDef, String>>,
    kuboSizePx: Float,
    modifier: Modifier
) {
    Canvas(modifier = modifier) {
        conexiones.forEach { (src, tgt, tipo) ->
            val startX = src.position.x + kuboSizePx / 2f
            val startY = src.position.y + kuboSizePx / 2f
            val endX   = tgt.position.x + kuboSizePx / 2f
            val endY   = tgt.position.y + kuboSizePx / 2f
            val midX   = (startX + endX) / 2f
            val path = Path().apply {
                moveTo(startX, startY)
                cubicTo(midX, startY, midX, endY, endX, endY)
            }
            val lineColor = when (tipo) {
                "dependency" -> AzulCobalto.copy(alpha = 0.6f)
                "security"   -> Mandarina.copy(alpha = 0.6f)
                else         -> AcentoCyan.copy(alpha = 0.5f)
            }
            drawPath(path, lineColor, style = Stroke(width = 3f))
            drawCircle(lineColor, 5f, Offset(startX, startY))
            drawCircle(lineColor, 7f, Offset(endX, endY))
            drawCircle(FondoOscuroClay, 3f, Offset(endX, endY))
        }
    }
}

@Composable
private fun KuboInteractivo(
    kubo:       KuboDef,
    kuboSizeDp: Dp,
    kuboSizePx: Float,
    figureId:   String,
    cellWidth:  Float,
    cellHeight: Float,
    onMove:     (Float, Float) -> Unit,
    onDrop:     () -> Unit,
    onTap:      () -> Unit,
) {
    val isDragging = kubo.state == KuboState.SELECTED
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "kubo_scale"
    )
    val kuboColor = when (kubo.type) {
        "security" -> Mandarina
        "utility"  -> GrisArcilla
        "users"    -> AcentoCyan
        "database" -> PurpleAccent
        else       -> AzulCobalto
    }
    val isActive = kubo.state == KuboState.ACTIVE
    val isError  = kubo.state == KuboState.ERROR

    ClayContainer(
        modifier = Modifier
            .offset { IntOffset(kubo.position.x.roundToInt(), kubo.position.y.roundToInt()) }
            .size(kuboSizeDp)
            .scale(scale),
        backgroundColor = if (isActive) kuboColor.copy(alpha = 0.25f) else FondoOscuroClay,
        cornerRadius = 16.dp,
        onClick = onTap
    ) {
        Box(modifier = Modifier.size(kuboSizeDp * 0.55f)) {
            RenderKuboFigure(figureId = figureId, color = Hueso)
        }
        if (isError) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Mandarina) // Usar Mandarina para errores
            )
        }
    }
}

@Composable
private fun PanelDetalleKubo(kubo: KuboDef, onClose: () -> Unit) {
    val kuboColor = when (kubo.type) {
        "security" -> Mandarina
        "utility"  -> GrisArcilla
        "users"    -> AcentoCyan
        "database" -> PurpleAccent
        else       -> AzulCobalto
    }

    Column(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(FondoOscuroClay)
            .border(
                width = 1.dp,
                color = LineaMalla, // Mantener borde sutil
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text       = kubo.kuboId.uppercase(),
                color      = Hueso,
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 11.sp,
                letterSpacing = 2.sp
            )
            ClayContainer(
                onClick = onClose,
                backgroundColor = GrisArcilla,
                cornerRadius = 12.dp,
                modifier = Modifier.size(24.dp)
            ) {
                Text("×", color = Hueso, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(kuboColor))
            Text(kubo.type.uppercase(), color = kuboColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
        KuboDetailRow("SLOT",    kubo.dominantSlot)
        KuboDetailRow("ESTADO",  kubo.state.name)
        KuboDetailRow("POS X",   "%.0f".format(kubo.position.x))
        KuboDetailRow("POS Y",   "%.0f".format(kubo.position.y))
        Spacer(Modifier.weight(1f))
        Text(text = "Kit · Kubo Map 1.0", color = LineaMalla, fontSize = 9.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun KuboDetailRow(label: String, value: String) {
    ClayContainer(
        backgroundColor = GrisArcilla.copy(alpha = 0.5f), // Fondo más suave para la fila
        cornerRadius = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Hueso.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text(value, color = Hueso, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}
