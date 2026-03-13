package com.dreiz.kit.ui.design.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreiz.kit.ui.design.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class DesignSystemUiState(
    val variants: List<DesignSystemVariant> = emptyList(),
    val selectedVariantId: String? = null,
    val isGenerating: Boolean = false,
    val isGeneratingLogo: Boolean = false,
    val logoVariants: List<String> = emptyList(),
    val selectedLogoUrl: String? = null,
    val selectedLogoUri: Uri? = null,
    val logoPrompt: String = "",
    val errorMessage: String? = null,
    val config: AppForgeConfig = AppForgeConfig()
)

class DesignSystemViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DesignSystemUiState())
    val uiState: StateFlow<DesignSystemUiState> = _uiState.asStateFlow()

    private val grokApiKey = "TU_GROK_API_KEY_AQUI"
    private val claudeApiKey = "TU_CLAUDE_API_KEY_AQUI"

    fun selectVariant(variantId: String) {
        _uiState.update { state ->
            val variant = state.variants.find { it.id == variantId }
            state.copy(
                selectedVariantId = variantId,
                config = state.config.copy(selectedVariant = variant)
            )
        }
    }

    fun generateVariantsFromAnswers(answers: Map<String, List<String>>, appName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, errorMessage = null) }
            try {
                val prompt = buildDesignSystemPrompt(answers, appName)
                val json = callClaudeApi(prompt)
                val variants = parseVariants(json)
                _uiState.update { state ->
                    state.copy(
                        variants = variants,
                        isGenerating = false,
                        config = state.config.copy(appName = appName)
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGenerating = false, errorMessage = e.message) }
            }
        }
    }

    fun generateLogosWithGrok(userPrompt: String) {
        val variant = _uiState.value.config.selectedVariant ?: return
        val appName = _uiState.value.config.appName

        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingLogo = true, logoVariants = emptyList(), errorMessage = null) }
            try {
                val fullPrompt = """
                    Minimal app icon logo for "$appName". 
                    Style: ${variant.mood}, ${variant.name}. 
                    Primary color: #${variant.primaryColor.value.toString(16).drop(2).take(6).uppercase()}. 
                    Clean, modern, suitable for mobile app icon. 
                    $userPrompt
                """.trimIndent()

                val urls = mutableListOf<String>()
                repeat(4) {
                    val url = callGrokImageApi(fullPrompt)
                    if (url != null) urls.add(url)
                }
                _uiState.update { it.copy(isGeneratingLogo = false, logoVariants = urls) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGeneratingLogo = false, errorMessage = e.message) }
            }
        }
    }

    fun selectGeneratedLogo(url: String) {
        _uiState.update { state ->
            state.copy(
                selectedLogoUrl = url,
                selectedLogoUri = null,
                config = state.config.copy(logoSource = LogoSource.FromAI(url, state.logoPrompt))
            )
        }
    }

    fun selectLocalLogo(uri: Uri, fromCamera: Boolean = false) {
        _uiState.update { state ->
            state.copy(
                selectedLogoUri = uri,
                selectedLogoUrl = null,
                config = state.config.copy(
                    logoSource = if (fromCamera) LogoSource.FromCamera(uri) else LogoSource.FromGallery(uri)
                )
            )
        }
    }

    fun updateLogoPrompt(prompt: String) {
        _uiState.update { it.copy(logoPrompt = prompt) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun buildDesignSystemPrompt(answers: Map<String, List<String>>, appName: String): String = """
        Eres un experto en Design Systems. Genera EXACTAMENTE 4 variantes para "$appName".
        Clima: ${answers["clima"]?.joinToString() ?: "no especificado"}
        Formalidad: ${answers["formalidad"]?.joinToString() ?: "no especificado"}
        Densidad: ${answers["densidad"]?.joinToString() ?: "no especificado"}
        Lenguaje: ${answers["lenguaje"]?.joinToString() ?: "no especificado"}
        Paleta: ${answers["paleta"]?.joinToString() ?: "no especificado"}
        Movimiento: ${answers["movimiento"]?.joinToString() ?: "no especificado"}
        Responde SOLO JSON sin markdown:
        {"variants":[{"id":"A","name":"nombre","tagline":"max 5 palabras","bg":"#hex","surface":"#hex","primary":"#hex","accent":"#hex","text":"#hex","fontFamily":"nombre","borderRadius":12,"animSpeed":"medium","shadowStyle":"soft","spacing":"normal","mood":"palabra"}]}
    """.trimIndent()

    private suspend fun callClaudeApi(prompt: String): String {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val url = URL("https://api.anthropic.com/v1/messages")
            val conn = url.openConnection() as HttpsURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("x-api-key", claudeApiKey)
            conn.setRequestProperty("anthropic-version", "2023-06-01")
            conn.doOutput = true

            val body = """{"model":"claude-sonnet-4-20250514","max_tokens":1500,"messages":[{"role":"user","content":"$prompt"}]}"""
            conn.outputStream.write(body.toByteArray())

            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            response
        }
    }

    private suspend fun callGrokImageApi(prompt: String): String? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = URL("https://api.x.ai/v1/images/generations")
                val conn = url.openConnection() as HttpsURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $grokApiKey")
                conn.doOutput = true

                val body = """{"model":"grok-2-image-1212","prompt":"${prompt.replace("\"","'")}","n":1,"response_format":"url"}"""
                conn.outputStream.write(body.toByteArray())

                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                val json = JSONObject(response)
                json.getJSONArray("data").getJSONObject(0).getString("url")
            } catch (e: Exception) { null }
        }
    }

    private fun parseVariants(json: String): List<DesignSystemVariant> {
        val clean = json.substringAfter("{").let { "{$it" }
        val root = JSONObject(clean)
        val arr = root.getJSONArray("variants")
        return (0 until arr.length()).map { i ->
            val v = arr.getJSONObject(i)
            DesignSystemVariant(
                id           = v.getString("id"),
                name         = v.getString("name"),
                tagline      = v.getString("tagline"),
                bgColor      = Color(android.graphics.Color.parseColor(v.getString("bg"))),
                surfaceColor = Color(android.graphics.Color.parseColor(v.getString("surface"))),
                primaryColor = Color(android.graphics.Color.parseColor(v.getString("primary"))),
                accentColor  = Color(android.graphics.Color.parseColor(v.getString("accent"))),
                textColor    = Color(android.graphics.Color.parseColor(v.getString("text"))),
                fontFamily   = v.getString("fontFamily"),
                borderRadius = v.getInt("borderRadius"),
                animSpeed    = AnimSpeed.valueOf(v.getString("animSpeed").uppercase()),
                shadowStyle  = ShadowStyle.valueOf(v.getString("shadowStyle").uppercase()),
                spacing      = SpacingDensity.valueOf(v.getString("spacing").uppercase()),
                mood         = v.getString("mood")
            )
        }
    }
}
