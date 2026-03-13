// WarRoomState.kt + WarRoomViewModel.kt — Consola de agentes
package com.dreiz.kit.ui.warroom

import androidx.lifecycle.ViewModel
import com.dreiz.kit.domain.agents.AgentLog
import com.dreiz.kit.domain.agents.AgentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WarRoomState(
    val isBuilding:       Boolean        = false,
    val consensusReached: Boolean        = false,
    val logs:             List<AgentLog> = emptyList(),
    val appGenomeConfig:  String         = "Estóico / Minimalista"
)

class WarRoomViewModel : ViewModel() {
    private val _state = MutableStateFlow(WarRoomState())
    val state: StateFlow<WarRoomState> = _state.asStateFlow()

    fun startAgentConsensus() { _state.value = _state.value.copy(isBuilding = true) }

    fun addLog(agent: AgentType, message: String, isCritical: Boolean = false) {
        val log = AgentLog(System.currentTimeMillis().toString(), agent, message, isCritical)
        _state.value = _state.value.copy(logs = _state.value.logs + log)
    }
}
