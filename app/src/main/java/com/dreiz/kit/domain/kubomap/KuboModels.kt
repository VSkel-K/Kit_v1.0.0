// KuboModels.kt — Data classes del mapa Kubo Map 4×8
package com.dreiz.kit.domain.kubomap

data class KuboMapState(
    val projectId:   String,
    val seed:        String,
    val grid:        GridDef             = GridDef(rows = 4, columns = 8),
    val kubos:       List<KuboDef>       = emptyList(),
    val connections: List<ConnectionDef> = emptyList(),
    val slots:       List<SlotDef>       = emptyList()
)
data class GridDef(val rows: Int, val columns: Int)
data class SlotDef(val slotId: String, val row: Int, val column: Int, val figureId: String, val occupied: Boolean = false)
data class KuboPosition(val x: Float, val y: Float)
data class KuboDef(val kuboId: String, val type: String, val position: KuboPosition, val dominantSlot: String, val state: KuboState = KuboState.NORMAL)
enum class KuboState { NORMAL, SELECTED, ACTIVE, ERROR }
data class ConnectionDef(val connectionId: String, val sourceKubo: String, val targetKubo: String, val type: String)
