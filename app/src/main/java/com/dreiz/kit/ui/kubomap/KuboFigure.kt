// ============================================================
// KuboFigure.kt
// Descripción: Biblioteca completa de las 32 figuras geométricas
// del catálogo oficial Kit. Cada figura se renderiza con Canvas
// de Jetpack Compose. La figura se asigna a un slot por semilla
// determinística (SHA-256 del project_id). El color se pasa
// desde el Kubo padre (normalmente BlancoHueso sobre fondo oscuro).
// Dónde va: app/src/main/java/com/dreiz/kit/ui/kubomap/KuboFigure.kt
// ============================================================

package com.dreiz.kit.ui.kubomap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

// ── CATÁLOGO OFICIAL (32 figuras, inmutable) ──────────────────
// Este es el único lugar del codebase donde se define la lista.
// El orquestador Python NO asigna figuras; solo asigna slots.
// La UI determina qué figura va en cada slot usando la semilla.
val BIBLIOTECA_FIGURAS_KIT = listOf(
    "circle",           // 00 - Círculo sólido
    "hollow_circle",    // 01 - Aro / círculo hueco
    "square",           // 02 - Cuadrado sólido
    "hollow_square",    // 03 - Cuadrado hueco (marco)
    "triangle",         // 04 - Triángulo apuntando arriba
    "inverted_triangle",// 05 - Triángulo apuntando abajo
    "diamond",          // 06 - Rombo sólido
    "hollow_diamond",   // 07 - Rombo hueco
    "cross",            // 08 - Cruz diagonal (×)
    "plus",             // 09 - Cruz ortogonal (+)
    "double_bar",       // 10 - Dos barras verticales ( ‖ )
    "triple_bar",       // 11 - Tres barras verticales ( ⦀ )
    "vertical_bar",     // 12 - Barra vertical única
    "horizontal_bar",   // 13 - Barra horizontal única
    "split_square",     // 14 - Cuadrado partido en diagonal
    "split_circle",     // 15 - Semicírculo izquierda/derecha
    "hexagon",          // 16 - Hexágono sólido
    "hollow_hexagon",   // 17 - Hexágono hueco
    "octagon",          // 18 - Octágono sólido
    "hollow_octagon",   // 19 - Octágono hueco
    "star_four",        // 20 - Estrella de 4 puntas
    "star_six",         // 21 - Estrella de 6 puntas
    "arrow_up",         // 22 - Flecha arriba
    "arrow_down",       // 23 - Flecha abajo
    "arrow_left",       // 24 - Flecha izquierda
    "arrow_right",      // 25 - Flecha derecha
    "node_cluster",     // 26 - 3 puntos conectados (nodo)
    "ring",             // 27 - Aro con grosor pronunciado
    "dot_quad",         // 28 - 4 puntos en esquinas
    "lattice",          // 29 - Cuadrícula 2×2 interior
    "prism",            // 30 - Prisma / romboide perspectiva
    "cylinder"          // 31 - Cilindro (elipse + rect)
)

// ── HELPERS INTERNOS ──────────────────────────────────────────

/** Devuelve los vértices de un polígono regular centrado en (cx, cy). */
private fun polygonPath(
    cx: Float, cy: Float, radius: Float, sides: Int, rotationDeg: Float = 0f
): Path {
    val path = Path()
    val angleStep = (2 * Math.PI / sides).toFloat()
    val startAngle = Math.toRadians(rotationDeg.toDouble()).toFloat()
    for (i in 0 until sides) {
        val angle = startAngle + i * angleStep
        val x = cx + radius * cos(angle)
        val y = cy + radius * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/** Estrella de N puntas. outerR = radio punta, innerR = radio valle. */
private fun starPath(
    cx: Float, cy: Float, outerR: Float, innerR: Float, points: Int
): Path {
    val path = Path()
    val totalVerts = points * 2
    for (i in 0 until totalVerts) {
        val angle = (Math.PI * i / points - Math.PI / 2).toFloat()
        val r = if (i % 2 == 0) outerR else innerR
        val x = cx + r * cos(angle)
        val y = cy + r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

// ── COMPOSABLE PRINCIPAL ──────────────────────────────────────

/**
 * Renderiza la figura geométrica correspondiente a [figureId]
 * usando el [color] recibido del Kubo padre.
 * Diseñado para ocupar el 100% del Box que lo contiene.
 */
@Composable
fun RenderKuboFigure(figureId: String, color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = w * 0.32f           // radio base
        val sw = w * 0.08f          // stroke width base
        val stroke = Stroke(sw)

        when (figureId) {

            // ── 00 · circle ──────────────────────────────────
            "circle" -> drawCircle(color, radius = r, center = Offset(cx, cy))

            // ── 01 · hollow_circle ───────────────────────────
            "hollow_circle" -> drawCircle(color, radius = r, center = Offset(cx, cy), style = stroke)

            // ── 02 · square ──────────────────────────────────
            "square" -> drawRect(color, Offset(cx - r, cy - r), Size(r * 2, r * 2))

            // ── 03 · hollow_square ───────────────────────────
            "hollow_square" -> drawRect(color, Offset(cx - r, cy - r), Size(r * 2, r * 2), style = stroke)

            // ── 04 · triangle ────────────────────────────────
            "triangle" -> drawPath(polygonPath(cx, cy, r, 3, -90f), color)

            // ── 05 · inverted_triangle ───────────────────────
            "inverted_triangle" -> drawPath(polygonPath(cx, cy, r, 3, 90f), color)

            // ── 06 · diamond ─────────────────────────────────
            "diamond" -> drawPath(polygonPath(cx, cy, r, 4, 0f), color)

            // ── 07 · hollow_diamond ──────────────────────────
            "hollow_diamond" -> drawPath(polygonPath(cx, cy, r, 4, 0f), color, style = stroke)

            // ── 08 · cross (×) ───────────────────────────────
            "cross" -> {
                val d = r * 0.7f
                drawLine(color, Offset(cx - d, cy - d), Offset(cx + d, cy + d), sw)
                drawLine(color, Offset(cx + d, cy - d), Offset(cx - d, cy + d), sw)
            }

            // ── 09 · plus (+) ────────────────────────────────
            "plus" -> {
                drawLine(color, Offset(cx - r, cy), Offset(cx + r, cy), sw)
                drawLine(color, Offset(cx, cy - r), Offset(cx, cy + r), sw)
            }

            // ── 10 · double_bar ──────────────────────────────
            "double_bar" -> {
                val bw = w * 0.13f; val bh = h * 0.55f
                drawRect(color, Offset(cx - bw * 1.6f, cy - bh / 2), Size(bw, bh))
                drawRect(color, Offset(cx + bw * 0.6f, cy - bh / 2), Size(bw, bh))
            }

            // ── 11 · triple_bar ──────────────────────────────
            "triple_bar" -> {
                val bw = w * 0.10f; val bh = h * 0.55f
                val offsets = listOf(-bw * 2.0f, 0f, bw * 2.0f)
                offsets.forEach { dx ->
                    drawRect(color, Offset(cx - bw / 2 + dx, cy - bh / 2), Size(bw, bh))
                }
            }

            // ── 12 · vertical_bar ────────────────────────────
            "vertical_bar" -> drawLine(color, Offset(cx, cy - r), Offset(cx, cy + r), sw * 1.5f)

            // ── 13 · horizontal_bar ──────────────────────────
            "horizontal_bar" -> drawLine(color, Offset(cx - r, cy), Offset(cx + r, cy), sw * 1.5f)

            // ── 14 · split_square ────────────────────────────
            "split_square" -> {
                // Triángulo inferior-izquierdo relleno + hueco superior-derecho
                val path = Path().apply {
                    moveTo(cx - r, cy - r)
                    lineTo(cx + r, cy - r)
                    lineTo(cx + r, cy + r)
                    lineTo(cx - r, cy + r)
                    close()
                }
                drawPath(path, color.copy(alpha = 0.25f))
                drawPath(path, color, style = stroke)
                drawLine(color, Offset(cx - r, cy - r), Offset(cx + r, cy + r), sw)
            }

            // ── 15 · split_circle ────────────────────────────
            "split_circle" -> {
                // Semicírculo izquierdo sólido + derecho hueco
                val left = Path().apply {
                    moveTo(cx, cy - r)
                    arcTo(androidx.compose.ui.geometry.Rect(cx - r, cy - r, cx + r, cy + r), -90f, -180f, false)
                    close()
                }
                drawPath(left, color)
                drawArc(color, -90f, 180f, false,
                    Offset(cx - r, cy - r), Size(r * 2, r * 2), style = stroke)
            }

            // ── 16 · hexagon ─────────────────────────────────
            "hexagon" -> drawPath(polygonPath(cx, cy, r, 6, -90f), color)

            // ── 17 · hollow_hexagon ──────────────────────────
            "hollow_hexagon" -> drawPath(polygonPath(cx, cy, r, 6, -90f), color, style = stroke)

            // ── 18 · octagon ─────────────────────────────────
            "octagon" -> drawPath(polygonPath(cx, cy, r, 8, -22.5f), color)

            // ── 19 · hollow_octagon ──────────────────────────
            "hollow_octagon" -> drawPath(polygonPath(cx, cy, r, 8, -22.5f), color, style = stroke)

            // ── 20 · star_four ───────────────────────────────
            "star_four" -> drawPath(starPath(cx, cy, r, r * 0.4f, 4), color)

            // ── 21 · star_six ────────────────────────────────
            "star_six" -> drawPath(starPath(cx, cy, r, r * 0.5f, 6), color)

            // ── 22 · arrow_up ────────────────────────────────
            "arrow_up" -> {
                val path = Path().apply {
                    moveTo(cx, cy - r)
                    lineTo(cx + r * 0.7f, cy + r * 0.3f)
                    lineTo(cx + r * 0.25f, cy + r * 0.3f)
                    lineTo(cx + r * 0.25f, cy + r)
                    lineTo(cx - r * 0.25f, cy + r)
                    lineTo(cx - r * 0.25f, cy + r * 0.3f)
                    lineTo(cx - r * 0.7f, cy + r * 0.3f)
                    close()
                }
                drawPath(path, color)
            }

            // ── 23 · arrow_down ──────────────────────────────
            "arrow_down" -> {
                val path = Path().apply {
                    moveTo(cx, cy + r)
                    lineTo(cx + r * 0.7f, cy - r * 0.3f)
                    lineTo(cx + r * 0.25f, cy - r * 0.3f)
                    lineTo(cx + r * 0.25f, cy - r)
                    lineTo(cx - r * 0.25f, cy - r)
                    lineTo(cx - r * 0.25f, cy - r * 0.3f)
                    lineTo(cx - r * 0.7f, cy - r * 0.3f)
                    close()
                }
                drawPath(path, color)
            }

            // ── 24 · arrow_left ──────────────────────────────
            "arrow_left" -> {
                val path = Path().apply {
                    moveTo(cx - r, cy)
                    lineTo(cx - r * 0.3f, cy - r * 0.7f)
                    lineTo(cx - r * 0.3f, cy - r * 0.25f)
                    lineTo(cx + r, cy - r * 0.25f)
                    lineTo(cx + r, cy + r * 0.25f)
                    lineTo(cx - r * 0.3f, cy + r * 0.25f)
                    lineTo(cx - r * 0.3f, cy + r * 0.7f)
                    close()
                }
                drawPath(path, color)
            }

            // ── 25 · arrow_right ─────────────────────────────
            "arrow_right" -> {
                val path = Path().apply {
                    moveTo(cx + r, cy)
                    lineTo(cx + r * 0.3f, cy - r * 0.7f)
                    lineTo(cx + r * 0.3f, cy - r * 0.25f)
                    lineTo(cx - r, cy - r * 0.25f)
                    lineTo(cx - r, cy + r * 0.25f)
                    lineTo(cx + r * 0.3f, cy + r * 0.25f)
                    lineTo(cx + r * 0.3f, cy + r * 0.7f)
                    close()
                }
                drawPath(path, color)
            }

            // ── 26 · node_cluster ────────────────────────────
            "node_cluster" -> {
                val nodeR = r * 0.22f
                val positions = listOf(
                    Offset(cx, cy - r * 0.6f),
                    Offset(cx - r * 0.55f, cy + r * 0.35f),
                    Offset(cx + r * 0.55f, cy + r * 0.35f)
                )
                // Líneas de conexión
                positions.forEachIndexed { i, a ->
                    positions.forEachIndexed { j, b ->
                        if (i < j) drawLine(color.copy(alpha = 0.6f), a, b, sw * 0.7f)
                    }
                }
                // Nodos
                positions.forEach { pos -> drawCircle(color, nodeR, pos) }
            }

            // ── 27 · ring ────────────────────────────────────
            "ring" -> drawCircle(color, radius = r, center = Offset(cx, cy), style = Stroke(sw * 2.5f))

            // ── 28 · dot_quad ────────────────────────────────
            "dot_quad" -> {
                val dr = r * 0.2f; val off = r * 0.5f
                listOf(
                    Offset(cx - off, cy - off), Offset(cx + off, cy - off),
                    Offset(cx - off, cy + off), Offset(cx + off, cy + off)
                ).forEach { drawCircle(color, dr, it) }
            }

            // ── 29 · lattice ─────────────────────────────────
            "lattice" -> {
                val half = r * 0.5f
                // 3 líneas verticales + 3 horizontales
                listOf(-half, 0f, half).forEach { offset ->
                    drawLine(color, Offset(cx + offset, cy - r), Offset(cx + offset, cy + r), sw * 0.6f)
                    drawLine(color, Offset(cx - r, cy + offset), Offset(cx + r, cy + offset), sw * 0.6f)
                }
            }

            // ── 30 · prism ───────────────────────────────────
            "prism" -> {
                // Romboide con perspectiva isométrica
                val dx = r * 0.5f; val dy = r * 0.3f
                val path = Path().apply {
                    moveTo(cx, cy - r)                          // cima
                    lineTo(cx + r, cy - dy)                     // derecha arriba
                    lineTo(cx + r, cy + r - dy)                 // derecha abajo
                    lineTo(cx, cy + r)                          // base
                    lineTo(cx - r, cy + r - dy)                 // izquierda abajo
                    lineTo(cx - r, cy - dy)                     // izquierda arriba
                    close()
                }
                drawPath(path, color.copy(alpha = 0.3f))
                drawPath(path, color, style = stroke)
                // Aristas internas
                drawLine(color.copy(alpha = 0.5f), Offset(cx, cy - r), Offset(cx, cy + r), sw * 0.5f)
                drawLine(color.copy(alpha = 0.5f), Offset(cx - r, cy - dy), Offset(cx + r, cy - dy), sw * 0.5f)
            }

            // ── 31 · cylinder ────────────────────────────────
            "cylinder" -> {
                val ew = r * 1.0f; val eh = r * 0.35f  // semi-eje de la elipse
                val bodyH = r * 1.1f
                // Cuerpo del cilindro
                val bodyPath = Path().apply {
                    moveTo(cx - ew, cy - bodyH / 2 + eh / 2)
                    lineTo(cx - ew, cy + bodyH / 2 - eh / 2)
                    arcTo(
                        androidx.compose.ui.geometry.Rect(cx - ew, cy + bodyH / 2 - eh, cx + ew, cy + bodyH / 2 + eh),
                        180f, -180f, false
                    )
                    lineTo(cx + ew, cy - bodyH / 2 + eh / 2)
                }
                drawPath(bodyPath, color.copy(alpha = 0.25f))
                drawPath(bodyPath, color, style = stroke)
                // Tapa superior (elipse)
                drawOval(color, Offset(cx - ew, cy - bodyH / 2 - eh / 2), Size(ew * 2, eh * 2))
            }

            // ── Fallback ─────────────────────────────────────
            else -> {
                // Punto de advertencia: figura no encontrada en catálogo
                drawCircle(color.copy(alpha = 0.4f), r * 0.15f, Offset(cx, cy))
                drawCircle(color, r * 0.15f, Offset(cx, cy), style = stroke)
            }
        }
    }
}
