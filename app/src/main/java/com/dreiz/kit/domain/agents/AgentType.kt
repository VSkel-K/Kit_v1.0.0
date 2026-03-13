// AgentType.kt + AgentLog.kt — Tipos de agentes y mensajes
package com.dreiz.kit.domain.agents

enum class AgentType { ARCHITECT, SECURITY, SYSTEM }

data class AgentLog(
    val id:         String,
    val agent:      AgentType,
    val message:    String,
    val isCritical: Boolean = false
)
