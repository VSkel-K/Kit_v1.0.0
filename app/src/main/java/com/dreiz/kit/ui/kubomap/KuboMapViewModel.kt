// ============================================================
// KuboMapViewModel.kt
// Descripción: ViewModel del tablero Kubo Map. Gestiona:
//   · Carga de proyecto con generación determinística de malla 4×8
//   · Shuffle de las 32 figuras usando SHA-256 como semilla
//   · Posicionamiento en tiempo real durante el drag
//   · Soft Snap al soltar: alineación magnética al centro del slot
//   · Actualización del slot dominant y estado "occupied"
//   · Colisión básica: si el slot destino está ocupado, no snapea
// Dónde va: app/src/main/java/com/dreiz/kit/ui/kubomap/KuboMapViewModel.kt
// ============================================================

package com.dreiz.kit.ui.kubomap

import androidx.lifecycle.ViewModel
import com.dreiz.kit.domain.kubomap.ConnectionDef
import com.dreiz.kit.domain.kubomap.GridDef
import com.dreiz.kit.domain.kubomap.KuboDef
import com.dreiz.kit.domain.kubomap.KuboMapState
import com.dreiz.kit.domain.kubomap.KuboPosition
import com.dreiz.kit.domain.kubomap.KuboState
import com.dreiz.kit.domain.kubomap.SlotDef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import kotlin.math.roundToInt

class KuboMapViewModel : ViewModel() {

    private val _mapState = MutableStateFlow(KuboMapState(projectId = "", seed = ""))
    val mapState = _mapState.asStateFlow()

    // ── Carga / inicialización ─────────────────────────────────
    /**
     * Punto de entrada principal del tablero.
     * [seed] se genera en la app como SHA-256 del project_id
     * pero también puede venir del servidor para reproducibilidad.
     */
    fun cargarProyecto(
        proyectoId: String,
        seed: String,
        kubosGuardados: List<KuboDef> = emptyList(),
        conexiones: List<ConnectionDef> = emptyList()
    ) {
        val slots       = generarMallaDeterminista(seed)
        val slotsConKubos = marcarSlotsOcupados(slots, kubosGuardados)

        _mapState.value = KuboMapState(
            projectId   = proyectoId,
            seed        = seed,
            grid        = GridDef(rows = 4, columns = 8),
            kubos       = kubosGuardados,
            connections = conexiones,
            slots       = slotsConKubos
        )
    }

    // ── Generación de malla ────────────────────────────────────
    /**
     * Genera exactamente 32 slots (4 filas × 8 columnas).
     * Las 32 figuras del catálogo se mezclan usando java.util.Random
     * inicializado con los primeros 16 caracteres hexadecimales de la semilla.
     * Mismo seed = mismo layout siempre (determinístico).
     */
    private fun generarMallaDeterminista(seed: String): List<SlotDef> {
        val figuras   = BIBLIOTECA_FIGURAS_KIT.toMutableList()
        val seedLong  = seed.take(16).toLongOrNull(16) ?: 0L
        val random    = java.util.Random(seedLong)

        figuras.shuffle(random)          // shuffle con semilla reproducible

        val slots = mutableListOf<SlotDef>()
        var index = 0
        for (r in 1..4) {
            for (c in 1..8) {
                slots.add(
                    SlotDef(
                        slotId   = "slot_${r}_${c}",
                        row      = r,
                        column   = c,
                        figureId = figuras[index],   // figura determinística
                        occupied = false
                    )
                )
                index++
            }
        }
        return slots
    }

    /** Marca los slots que ya tienen un Kubo guardado como occupied = true. */
    private fun marcarSlotsOcupados(
        slots: List<SlotDef>,
        kubos: List<KuboDef>
    ): List<SlotDef> {
        val ocupados = kubos.map { it.dominantSlot }.toSet()
        return slots.map { slot ->
            slot.copy(occupied = slot.slotId in ocupados)
        }
    }

    // ── Generación de semilla ──────────────────────────────────
    /**
     * Convierte un projectId a su hash SHA-256 hexadecimal.
     * Se usa como semilla para el shuffle de la malla.
     * Expuesta como utilidad para que la ViewModel raíz del proyecto
     * la use antes de llamar a cargarProyecto().
     */
    fun generarSeed(projectId: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(projectId.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // ── Movimiento en tiempo real (drag) ───────────────────────
    /**
     * Se llama en cada frame de detectDragGestures → onDrag.
     * Actualiza solo la posición del kubo (sin snap).
     * El estado es SELECTED mientras se arrastra.
     */
    fun moverKubo(kuboId: String, nuevaX: Float, nuevaY: Float) {
        _mapState.value = _mapState.value.copy(
            kubos = _mapState.value.kubos.map { kubo ->
                if (kubo.kuboId == kuboId) {
                    kubo.copy(
                        position = KuboPosition(nuevaX, nuevaY),
                        state    = KuboState.SELECTED
                    )
                } else kubo
            }
        )
    }

    // ── Soft Snap (onDragEnd) ──────────────────────────────────
    /**
     * Se llama cuando el usuario suelta el Kubo (detectDragGestures → onDragEnd).
     *
     * Lógica:
     *  1. Calcula el slot más cercano al centro del Kubo.
     *  2. Verifica si el slot está ocupado por otro Kubo.
     *  3. Si está libre → hace snap al centro del slot y actualiza dominantSlot.
     *  4. Si está ocupado → devuelve el Kubo a su slot anterior (rebota).
     *
     * [cellWidth] / [cellHeight] se calculan en KuboMapScreen como:
     *     gridSize.width / 8f  y  gridSize.height / 4f
     * [kuboSizePx] es el tamaño del Box del Kubo convertido a px (ej. 80.dp.toPx())
     */
    fun aplicarSoftSnap(
        kuboId: String,
        currentX: Float,
        currentY: Float,
        cellWidth: Float,
        cellHeight: Float,
        kuboSizePx: Float
    ) {
        val state = _mapState.value
        val kubo  = state.kubos.find { it.kuboId == kuboId } ?: return

        // Centro del Kubo en el canvas
        val centerX = currentX + kuboSizePx / 2f
        val centerY = currentY + kuboSizePx / 2f

        // Columna y fila destino (0-indexed)
        val targetCol = (centerX / cellWidth).roundToInt().coerceIn(0, 7)
        val targetRow = (centerY / cellHeight).roundToInt().coerceIn(0, 3)

        val newSlotId = "slot_${targetRow + 1}_${targetCol + 1}"

        // Verificar colisión: si el slot destino ya está ocupado por otro kubo
        val slotOcupadoPorOtro = state.kubos.any { other ->
            other.kuboId != kuboId && other.dominantSlot == newSlotId
        }

        if (slotOcupadoPorOtro) {
            // Rebote: devolver al slot anterior sin moverse
            val slotAnterior = state.slots.find { it.slotId == kubo.dominantSlot }
            val (revertX, revertY) = if (slotAnterior != null) {
                val col0 = slotAnterior.column - 1
                val row0 = slotAnterior.row - 1
                Pair(
                    col0 * cellWidth  + cellWidth  / 2f - kuboSizePx / 2f,
                    row0 * cellHeight + cellHeight / 2f - kuboSizePx / 2f
                )
            } else Pair(currentX, currentY)

            _mapState.value = state.copy(
                kubos = state.kubos.map { k ->
                    if (k.kuboId == kuboId) k.copy(
                        position = KuboPosition(revertX, revertY),
                        state    = KuboState.NORMAL
                    ) else k
                }
            )
            return
        }

        // Snap exitoso: calcular posición centrada en la celda
        val snapX = targetCol * cellWidth  + cellWidth  / 2f - kuboSizePx / 2f
        val snapY = targetRow * cellHeight + cellHeight / 2f - kuboSizePx / 2f

        // Actualizar slot anterior como libre y nuevo slot como ocupado
        val oldSlotId = kubo.dominantSlot
        val slotsActualizados = state.slots.map { slot ->
            when (slot.slotId) {
                oldSlotId -> slot.copy(occupied = false)
                newSlotId -> slot.copy(occupied = true)
                else      -> slot
            }
        }

        _mapState.value = state.copy(
            kubos = state.kubos.map { k ->
                if (k.kuboId == kuboId) k.copy(
                    position     = KuboPosition(snapX, snapY),
                    dominantSlot = newSlotId,
                    state        = KuboState.NORMAL
                ) else k
            },
            slots = slotsActualizados
        )
    }

    // ── Selección de Kubo ──────────────────────────────────────
    /** Marca un Kubo como ACTIVE (panel de detalle abierto). */
    fun seleccionarKubo(kuboId: String) {
        _mapState.value = _mapState.value.copy(
            kubos = _mapState.value.kubos.map { kubo ->
                kubo.copy(state = if (kubo.kuboId == kuboId) KuboState.ACTIVE else KuboState.NORMAL)
            }
        )
    }

    /** Deselecciona todos los Kubos. */
    fun deseleccionarTodos() {
        _mapState.value = _mapState.value.copy(
            kubos = _mapState.value.kubos.map { it.copy(state = KuboState.NORMAL) }
        )
    }

    // ── Agregar Kubo ───────────────────────────────────────────
    /**
     * Agrega un nuevo Kubo al primer slot libre que encuentre.
     * Si no hay slots libres, no hace nada.
     */
    fun agregarKubo(nuevoKubo: KuboDef) {
        val state      = _mapState.value
        val slotLibre  = state.slots.firstOrNull { !it.occupied } ?: return

        val cellWidth  = 1f   // placeholder; se recalcula en Screen
        val cellHeight = 1f

        val kuboPosicionado = nuevoKubo.copy(dominantSlot = slotLibre.slotId)
        val slotsActualizados = state.slots.map { slot ->
            if (slot.slotId == slotLibre.slotId) slot.copy(occupied = true) else slot
        }

        _mapState.value = state.copy(
            kubos = state.kubos + kuboPosicionado,
            slots = slotsActualizados
        )
    }

    // ── Conexiones ─────────────────────────────────────────────
    /** Agrega una conexión entre dos Kubos si no existe ya. */
    fun agregarConexion(sourceId: String, targetId: String, tipo: String = "dependency") {
        val state = _mapState.value
        val yaExiste = state.connections.any {
            it.sourceKubo == sourceId && it.targetKubo == targetId
        }
        if (yaExiste) return

        val nuevaConexion = ConnectionDef(
            connectionId = "conn_${System.currentTimeMillis()}",
            sourceKubo   = sourceId,
            targetKubo   = targetId,
            type         = tipo
        )
        _mapState.value = state.copy(connections = state.connections + nuevaConexion)
    }
}
