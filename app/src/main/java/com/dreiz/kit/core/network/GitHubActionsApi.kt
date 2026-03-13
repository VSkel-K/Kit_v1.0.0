// GitHubActionsApi.kt — Interfaz Retrofit para GitHub Actions dispatch
package com.dreiz.kit.core.network

import com.dreiz.kit.domain.genome.GitHubDispatchRequest
import retrofit2.Response
import retrofit2.http.*

interface GitHubActionsApi {
    @POST("repos/{owner}/{repo}/dispatches")
    suspend fun triggerAppCompilation(
        @Header("Authorization") token: String,
        @Header("Accept") accept: String = "application/vnd.github.v3+json",
        @Path("owner") owner: String,
        @Path("repo")  repo:  String,
        @Body request: GitHubDispatchRequest
    ): Response<Unit>
}
