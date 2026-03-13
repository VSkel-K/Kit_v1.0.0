// GenomeEntity.kt — Room Entity para el Córtex de Memoria
package com.dreiz.kit.data.local.cortex

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cortex_memoria")
data class GenomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name:              String,
    val aesthetic:         String,
    val agentInstructions: String,
    val timestamp:         Long    = System.currentTimeMillis(),
    val githubWorkflowId:  String? = null
)
