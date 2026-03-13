// ============================================================
// KuboMapScreen.kt
// Descripción: Pantalla del tablero Kubo Map.
//   · Malla 4×8 dibujada con Canvas (líneas + símbolo genérico +)
//   · Conexiones como curvas Bézier cúbicas entre Kubos
//   · Cada Kubo es un Box arrastrable con neumorfismo (RubberKubo)
//   · Soft Snap al soltar vía KuboMapViewModel.aplicarSoftSnap()
//   · Figura geométrica determinística renderizada dentro del Kubo
//   · Panel lateral de detalle al seleccionar un Kubo
// Dónde va: app/src/main/java/com/dreiz/kit/ui/kubomap/KuboMapScreen.kt
//
// Colores de tema importados de KitTheme.kt (ajusta según tus tokens):
//   FondoOscuro, LineaMalla, AzulCobalto, Mandarina, GrisArcilla, BlancoHueso
// ============================================================

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
import kotlin.math.roundToInt

// ── TOKENS DE TEMA ────────────────────────────────────────────
// Reemplaza estas constantes por tus Design Tokens reales
// cuando los tengas integrados en MaterialTheme.colorScheme
private val FondoOscuro   = Color(0xFF070A0F)
private val LineaMalla    = Color(0xFF1A2035)
private val AzulCobalto   = Color(0xFF0047AB)
private val Mandarina     = Color(0xFFFF8C00)
private val GrisArcilla   = Color(0xFF4A4A48)
private val BlancoHueso   = Color(0xFFF9F6EE)
private val AcentoCyan    = Color(0xFF06B6D4)
private val PurpleAccent  = Color(0xFF7C3AED)

// ── PANTALLA PRINCIPAL ─────────────────────────────────────────

@Composable
fun KuboMapScreen(viewModel: KuboMapViewModel) {
    val state       by viewModel.mapState.collectAsState()
    val density      = LocalDensity.current
    var gridSize     by remember { mutableStateOf(Size.Zero) }
    val kuboSizeDp   = 72.dp
    val kuboSizePx   = with(density) { kuboSizeDp.toPx() }

    // Kubo actualmente seleccionado para el panel de detalle
    val kuboSeleccionado = state.kubos.find { it.state == KuboState.ACTIVE }

    Row(modifier = Modifier.fillMaxSize().background(FondoOscuro)) {

        // ── TABLERO ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .onGloballyPositioned { coords ->
                    gridSize = coords.size.toSize()
                }
        ) {
            if (gridSize != Size.Zero) {
                val cellWidth  = gridSize.width  / 8f
                val cellHeight = gridSize.height / 4f

                // ── CAPA 1: Malla fija ─────────────────────────
                CapaMalla(
                    cellWidth  = cellWidth,
                    cellHeight = cellHeight,
                    modifier   = Modifier.fillMaxSize()
                )

                // ── CAPA 2: Conexiones Bézier ──────────────────
                CapaConexiones(
                    state.connections.mapNotNull { conn ->
                        val src = state.kubos.find { it.kuboId == conn.sourceKubo }
                        val tgt = state.kubos.find { it.kuboId == conn.targetKubo }
                        if (src != null && tgt != null) Triple(src, tgt, conn.type) else null
                    },
                    kuboSizePx = kuboSizePx,
                    modifier   = Modifier.fillMaxSize()
                )

                // ── CAPA 3: Kubos arrastrables ─────────────────
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

        // ── PANEL DETALLE (slide desde la derecha) ─────────────
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

// ── CAPA 1: MALLA ─────────────────────────────────────────────

@Composable
private fun CapaMalla(cellWidth: Float, cellHeight: Float, modifier: Modifier) {
    Canvas(modifier = modifier) {
        for (r in 0 until 4) {
            for (c in 0 until 8) {
                val x = c * cellWidth
                val y = r * cellHeight

                // Borde de celda
                drawRect(
                    color    = LineaMalla,
                    topLeft  = Offset(x, y),
                    size     = Size(cellWidth, cellHeight),
                    style    = Stroke(1f)
                )

                // Símbolo genérico "+" en el centro de cada celda vacía
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

// ── CAPA 2: CONEXIONES ────────────────────────────────────────

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

            // Control points para curva cúbica suave
            val midX   = (startX + endX) / 2f

            val path = Path().apply {
                moveTo(startX, startY)
                cubicTo(
                    midX, startY,   // control 1
                    midX, endY,     // control 2
                    endX, endY
                )
            }

            val lineColor = when (tipo) {
                "dependency" -> AzulCobalto.copy(alpha = 0.6f)
                "security"   -> Mandarina.copy(alpha = 0.6f)
                else         -> AcentoCyan.copy(alpha = 0.5f)
            }

            drawPath(path, lineColor, style = Stroke(width = 3f))

            // Punto de origen
            drawCircle(lineColor, 5f, Offset(startX, startY))
            // Punta de flecha simplificada (círculo en destino)
            drawCircle(lineColor, 7f, Offset(endX, endY))
            drawCircle(FondoOscuro, 3f, Offset(endX, endY))
        }
    }
}

// ── KUBO INTERACTIVO ──────────────────────────────────────────

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

    // Escala "squish" durante el drag (efecto goma neumórfico)
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

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    kubo.position.x.roundToInt(),
                    kubo.position.y.roundToInt()
                )
            }
            .size(kuboSizeDp)
            .scale(scale)
            .shadow(
                elevation = if (isDragging) 24.dp else 8.dp,
                shape     = RoundedCornerShape(16.dp),
                ambientColor = kuboColor.copy(alpha = 0.4f),
                spotColor    = kuboColor.copy(alpha = 0.6f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isActive) kuboColor.copy(alpha = 0.25f)
                else Color(0xFF131924)
            )
            .border(
                width = if (isActive || isDragging) 1.5.dp else 1.dp,
                color = when {
                    isError    -> Color(0xFFEF4444)
                    isActive   -> kuboColor
                    isDragging -> kuboColor.copy(alpha = 0.8f)
                    else       -> kuboColor.copy(alpha = 0.35f)
                },
                shape = RoundedCornerShape(16.dp)
            )
            .pointerInput(kubo.kuboId) {
                detectDragGestures(
                    onDragEnd    = { onDrop() },
                    onDragCancel = { onDrop() }
                ) { change, dragAmount ->
                    change.consume()
                    onMove(dragAmount.x, dragAmount.y)
                }
            }
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        // Figura geométrica centrada (60% del tamaño del Kubo)
        Box(modifier = Modifier.size(kuboSizeDp * 0.55f)) {
            RenderKuboFigure(figureId = figureId, color = BlancoHueso)
        }

        // Indicador de error (esquina superior derecha)
        if (isError) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444))
            )
        }
    }
}

// ── PANEL DETALLE ─────────────────────────────────────────────

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
            .background(Color(0xFF0D1117))
            .border(
                width = 1.dp,
                color = Color(0xFF1A2035),
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text       = kubo.kuboId.uppercase(),
                color      = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 11.sp,
                letterSpacing = 2.sp
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1F2937))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Text("×", color = Color(0xFF6B7280), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Color badge del tipo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(kuboColor)
            )
            Text(kubo.type.uppercase(), color = kuboColor, fontSize = 9.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }

        // Slot dominante
        KuboDetailRow("SLOT",    kubo.dominantSlot)
        KuboDetailRow("ESTADO",  kubo.state.name)
        KuboDetailRow("POS X",   "%.0f".format(kubo.position.x))
        KuboDetailRow("POS Y",   "%.0f".format(kubo.position.y))

        Spacer(Modifier.weight(1f))

        Text(
            text  = "Kit · Kubo Map 1.0",
            color = Color(0xFF1F2937),
            fontSize = 9.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun KuboDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF111827))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFF374151), fontSize = 9.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text(value, color = Color(0xFF9CA3AF), fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

// ── EXTENSIÓN HELPER ──────────────────────────────────────────

private fun androidx.compose.ui.unit.IntSize.toSize() =
    Size(width.toFloat(), height.toFloat())
