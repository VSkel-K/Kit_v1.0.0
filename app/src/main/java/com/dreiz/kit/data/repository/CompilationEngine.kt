// CompilationEngine.kt — Motor de disparo a GitHub Actions
package com.dreiz.kit.data.repository

import com.dreiz.kit.core.network.GitHubActionsApi
import com.dreiz.kit.domain.genome.AppGenomePayload
import com.dreiz.kit.domain.genome.GitHubDispatchRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CompilationEngine(
    private val api:         GitHubActionsApi,
    private val githubToken: String
) {
    val owner = "TuUsuarioDeGitHub"   // reemplazar
    val repo  = "TuRepoDeKit"         // reemplazar

    suspend fun fireAgents(genome: AppGenomePayload): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.triggerAppCompilation(
                    token   = "Bearer $githubToken",
                    owner   = owner,
                    repo    = repo,
                    request = GitHubDispatchRequest(payload = genome)
                )
                if (response.isSuccessful) Result.success("Señal enviada. Los agentes han despertado.")
                else Result.failure(Exception("Error: ${response.code()} - ${response.message()}"))
            } catch (e: Exception) { Result.failure(e) }
        }
    }
}
