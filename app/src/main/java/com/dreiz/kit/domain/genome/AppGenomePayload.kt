// AppGenomePayload.kt + GitHubDispatchRequest.kt — Contrato de disparo
package com.dreiz.kit.domain.genome

import com.google.gson.annotations.SerializedName

data class AppGenomePayload(
    val appName:           String,
    val aesthetic:         String,
    val isLocalFirst:      Boolean,
    val agentInstructions: String
)

data class GitHubDispatchRequest(
    @SerializedName("event_type") val eventType: String = "iniciar-sintesis-app",
    @SerializedName("client_payload") val payload: AppGenomePayload
)
