// ============================================================
// AppForge — Design System Selector + Logo Picker
// Jetpack Compose · Material3 · Kotlin
// ============================================================
// Archivos:
//   1. DesignSystemData.kt         — modelos de datos
//   2. DesignSystemViewModel.kt    — estado y lógica
//   3. DesignSystemSelectorScreen.kt — pantalla de elección
//   4. LogoPickerScreen.kt         — pantalla de logo
//   5. AppForgeNavigation.kt       — navegación entre pantallas
// ============================================================

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 1. DesignSystemData.kt
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

package com.appforge.ui.design

import androidx.compose.ui.graphics.Color

data class DesignSystemVariant(
    val id: String,
    val name: String,
    val tagline: String,
    val bgColor: Color,
    val surfaceColor: Color,
    val primaryColor: Color,
    val accentColor: Color,
    val textColor: Color,
    val fontFamily: String,
    val borderRadius: Int,        // dp
    val animSpeed: AnimSpeed,
    val shadowStyle: ShadowStyle,
    val spacing: SpacingDensity,
    val mood: String
)

enum class AnimSpeed { INSTANT, SLOW, MEDIUM, FAST }
enum class ShadowStyle { NONE, SOFT, MEDIUM, HARD, GLOW }
enum class SpacingDensity { TIGHT, NORMAL, LOOSE }

sealed class LogoSource {
    object None : LogoSource()
    data class FromGallery(val uri: android.net.Uri) : LogoSource()
    data class FromCamera(val uri: android.net.Uri) : LogoSource()
    data class FromAI(val imageUrl: String, val prompt: String) : LogoSource()
}

data class AppForgeConfig(
    val appName: String = "",
    val selectedVariant: DesignSystemVariant? = null,
    val logoSource: LogoSource = LogoSource.None,
    val screens: List<String> = listOf("Home", "Perfil", "Explorar")
)


// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 2. DesignSystemViewModel.kt
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

package com.appforge.ui.design

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

// ── UI State ──────────────────────────────────────────────
data class DesignSystemUiState(
    val variants: List<DesignSystemVariant> = emptyList(),
    val selectedVariantId: String? = null,
    val isGenerating: Boolean = false,
    val isGeneratingLogo: Boolean = false,
    val logoVariants: List<String> = emptyList(),     // URLs de logos generados
    val selectedLogoUrl: String? = null,
    val selectedLogoUri: Uri? = null,
    val logoPrompt: String = "",
    val errorMessage: String? = null,
    val config: AppForgeConfig = AppForgeConfig()
)

class DesignSystemViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DesignSystemUiState())
    val uiState: StateFlow<DesignSystemUiState> = _uiState.asStateFlow()

    // Clave Grok hardcodeada para demo
    private val grokApiKey = "TU_GROK_API_KEY_AQUI" // Removido por seguridad
    private val claudeApiKey = "TU_CLAUDE_API_KEY_AQUI"   // reemplazar

    // ── Seleccionar variante ───────────────────────────────
    fun selectVariant(variantId: String) {
        _uiState.update { state ->
            val variant = state.variants.find { it.id == variantId }
            state.copy(
                selectedVariantId = variantId,
                config = state.config.copy(selectedVariant = variant)
            )
        }
    }

    // ── Cargar variantes desde Claude API ─────────────────
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

    // ── Generar logos con Grok ────────────────────────────
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

                // Lanzar 4 peticiones en paralelo
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

    // ── Seleccionar logo generado por IA ──────────────────
    fun selectGeneratedLogo(url: String) {
        _uiState.update { state ->
            state.copy(
                selectedLogoUrl = url,
                selectedLogoUri = null,
                config = state.config.copy(logoSource = LogoSource.FromAI(url, state.logoPrompt))
            )
        }
    }

    // ── Seleccionar logo desde galería/cámara ─────────────
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

    // ── Helpers privados ──────────────────────────────────

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
                tagline      = v.optString("tagline", ""),
                bgColor      = Color(android.graphics.Color.parseColor(v.getString("bg"))),
                surfaceColor = Color(android.graphics.Color.parseColor(v.getString("surface"))),
                primaryColor = Color(android.graphics.Color.parseColor(v.getString("primary"))),
                accentColor  = Color(android.graphics.Color.parseColor(v.getString("accent"))),
                textColor    = Color(android.graphics.Color.parseColor(v.getString("text"))),
                fontFamily   = v.optString("fontFamily", "Sora"),
                borderRadius = v.optInt("borderRadius", 12),
                animSpeed    = when(v.optString("animSpeed","medium")) {
                    "slow"    -> AnimSpeed.SLOW
                    "fast"    -> AnimSpeed.FAST
                    "instant" -> AnimSpeed.INSTANT
                    else      -> AnimSpeed.MEDIUM
                },
                shadowStyle  = when(v.optString("shadowStyle","soft")) {
                    "none"   -> ShadowStyle.NONE
                    "medium" -> ShadowStyle.MEDIUM
                    "hard"   -> ShadowStyle.HARD
                    "glow"   -> ShadowStyle.GLOW
                    else     -> ShadowStyle.SOFT
                },
                spacing      = when(v.optString("spacing","normal")) {
                    "tight" -> SpacingDensity.TIGHT
                    "loose" -> SpacingDensity.LOOSE
                    else    -> SpacingDensity.NORMAL
                },
                mood         = v.optString("mood", "")
            )
        }
    }
}


// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 3. DesignSystemSelectorScreen.kt
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

package com.appforge.ui.design

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DesignSystemSelectorScreen(
    uiState: DesignSystemUiState,
    onVariantSelected: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070A0F))
            .padding(horizontal = 20.dp)
    ) {
        // ── Header ────────────────────────────────────────
        Spacer(Modifier.height(24.dp))
        Text(
            text = "ELIGE TU VARIANTE",
            color = Color(0xFF06B6D4),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Tu app en 4 universos visuales",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "La IA generó estas variantes basándose en tu visión",
            color = Color(0xFF444444),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.height(24.dp))

        // ── Grid de variantes ─────────────────────────────
        if (uiState.isGenerating) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = Color(0xFF06B6D4))
                    Text("Generando variantes...", color = Color(0xFF444444), fontSize = 13.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(uiState.variants) { variant ->
                    VariantCard(
                        variant = variant,
                        isSelected = uiState.selectedVariantId == variant.id,
                        onClick = { onVariantSelected(variant.id) }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Botón continuar ───────────────────────────────
        Button(
            onClick = onContinue,
            enabled = uiState.selectedVariantId != null,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7C3AED),
                disabledContainerColor = Color(0xFF111111)
            )
        ) {
            Text(
                text = if (uiState.selectedVariantId != null) "🚀  Siguiente — Elegir Logo" else "Elige una variante primero",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (uiState.selectedVariantId != null) Color.White else Color(0xFF333333)
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ── VariantCard ───────────────────────────────────────────
@Composable
fun VariantCard(
    variant: DesignSystemVariant,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) variant.primaryColor else Color(0xFF1A1F2E)
    val bgColor     = if (isSelected) Color(0xFF0D1A2E)    else Color(0xFF0D1117)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mini phone preview
        MiniPhonePreview(variant = variant, modifier = Modifier.fillMaxWidth().height(140.dp))

        // Name + tagline
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(variant.primaryColor)
            )
            Text(
                text = variant.name,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = variant.tagline,
            color = Color(0xFF444444),
            fontSize = 10.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Color strip
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(variant.bgColor, variant.surfaceColor, variant.primaryColor, variant.accentColor).forEach { color ->
                Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(color))
            }
        }

        // Selected badge
        AnimatedVisibility(visible = isSelected) {
            Text(
                text = "SELECCIONADA ✓",
                color = variant.primaryColor,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

// ── Mini phone preview (simplified) ──────────────────────
@Composable
fun MiniPhonePreview(
    variant: DesignSystemVariant,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(variant.bgColor)
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            // Navbar mock
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(variant.surfaceColor)
            )
            // Heading mock
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(variant.textColor.copy(alpha = 0.7f))
            )
            // Subtext mock
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(7.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(variant.textColor.copy(alpha = 0.3f))
            )
            Spacer(Modifier.height(2.dp))
            // Card mock
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(variant.borderRadius.dp))
                    .background(variant.surfaceColor)
            )
            // Button mock
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(variant.borderRadius.dp))
                    .background(variant.primaryColor)
            )
        }
    }
}


// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 4. LogoPickerScreen.kt
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

package com.appforge.ui.design

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

sealed class LogoMode { object None : LogoMode(); object Upload : LogoMode(); object Generate : LogoMode() }

@Composable
fun LogoPickerScreen(
    uiState: DesignSystemUiState,
    onGenerateLogos: (String) -> Unit,
    onSelectGeneratedLogo: (String) -> Unit,
    onSelectLocalLogo: (Uri) -> Unit,
    onUpdatePrompt: (String) -> Unit,
    onSkip: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = uiState.config.selectedVariant?.primaryColor ?: Color(0xFF7C3AED)
    var mode by remember { mutableStateOf<LogoMode>(LogoMode.None) }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { onSelectLocalLogo(it) } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070A0F))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Header
        Text("LOGO DE LA APP", color = Color(0xFF06B6D4), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Spacer(Modifier.height(6.dp))
        Text("Pon cara a tu app", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text("Sube tu logo o deja que Grok lo genere", color = Color(0xFF444444), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(24.dp))

        // ── Modo selección ────────────────────────────────
        AnimatedVisibility(visible = mode == LogoMode.None) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LogoOptionCard(
                    icon = "📁",
                    title = "Subir mi logo",
                    desc = "Usa un logo existente desde tu galería",
                    primaryColor = primary,
                    onClick = { mode = LogoMode.Upload }
                )
                LogoOptionCard(
                    icon = "✦",
                    title = "Generar con Grok IA",
                    desc = "Describe tu logo y Grok genera 4 variantes",
                    primaryColor = primary,
                    onClick = { mode = LogoMode.Generate }
                )
            }
        }

        // ── Upload mode ───────────────────────────────────
        AnimatedVisibility(visible = mode == LogoMode.Upload, enter = fadeIn(), exit = fadeOut()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                BackButton { mode = LogoMode.None }

                // Preview o placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0D1117))
                        .border(2.dp, if (uiState.selectedLogoUri != null) primary else Color(0xFF1F2937), RoundedCornerShape(16.dp))
                        .clickable { galleryLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.selectedLogoUri != null) {
                        AsyncImage(
                            model = uiState.selectedLogoUri,
                            contentDescription = "Logo",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📁", fontSize = 40.sp)
                            Text("Tap para abrir galería", color = Color(0xFF444444), fontSize = 13.sp)
                            Text("PNG · JPG · WEBP · SVG", color = Color(0xFF2A2A2A), fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // ── Generate mode ─────────────────────────────────
        AnimatedVisibility(visible = mode == LogoMode.Generate, enter = fadeIn(), exit = fadeOut()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BackButton { mode = LogoMode.None }

                // Prompt base (readonly info)
                val variant = uiState.config.selectedVariant
                if (variant != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0A0D12))
                            .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("PROMPT BASE (automático)", color = Color(0xFF2A2A2A), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Text(
                                text = "App icon para \"${uiState.config.appName}\", estilo ${variant.mood}, color primario ${variant.primaryColor}...",
                                color = Color(0xFF444444),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Prompt del usuario
                Text("AÑADE DETALLES", color = Color(0xFF333333), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                OutlinedTextField(
                    value = uiState.logoPrompt,
                    onValueChange = onUpdatePrompt,
                    placeholder = { Text("Forma, símbolo, letra inicial, estilo...", color = Color(0xFF333333), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primary,
                        unfocusedBorderColor = Color(0xFF1F2937),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = primary,
                        focusedContainerColor = Color(0xFF111111),
                        unfocusedContainerColor = Color(0xFF111111)
                    )
                )

                // Botón generar
                Button(
                    onClick = { onGenerateLogos(uiState.logoPrompt) },
                    enabled = !uiState.isGeneratingLogo,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary,
                        disabledContainerColor = Color(0xFF111111)
                    )
                ) {
                    if (uiState.isGeneratingLogo) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF444444), strokeWidth = 2.dp)
                            Text("Grok generando logos...", color = Color(0xFF444444), fontSize = 13.sp)
                        }
                    } else {
                        Text("✦  Generar 4 variantes con Grok", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Grid de logos generados
                AnimatedVisibility(visible = uiState.logoVariants.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("ELIGE UNA VARIANTE", color = Color(0xFF333333), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.height(220.dp)
                        ) {
                            items(uiState.logoVariants) { url ->
                                val isSelected = uiState.selectedLogoUrl == url
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF0A0D12))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) primary else Color(0xFF1F2937),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { onSelectGeneratedLogo(url) }
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = url,
                                        contentDescription = "Logo variant",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(20.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("✓", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Footer buttons ────────────────────────────────
        val hasLogo = uiState.selectedLogoUrl != null || uiState.selectedLogoUri != null
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF1F2937)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF555555))
            ) {
                Text("Omitir por ahora", fontSize = 12.sp)
            }
            Button(
                onClick = onConfirm,
                enabled = hasLogo,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primary,
                    disabledContainerColor = Color(0xFF111111)
                )
            ) {
                Text(
                    text = if (hasLogo) "✓  Usar este logo" else "Elige un logo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (hasLogo) Color.White else Color(0xFF333333)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LogoOptionCard(icon: String, title: String, desc: String, primaryColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF111111))
            .border(1.dp, Color(0xFF1A1F2E), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 28.sp)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(desc, color = Color(0xFF444444), fontSize = 12.sp)
        }
        Spacer(Modifier.weight(1f))
        Text("→", color = primaryColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text("← Atrás", color = Color(0xFF444444), fontSize = 12.sp)
    }
}


// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 5. AppForgeNavigation.kt
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

package com.appforge.ui.design

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*

object AppForgeRoutes {
    const val ENTRY            = "entry"
    const val QUESTIONNAIRE    = "questionnaire"
    const val IMAGE_UPLOAD     = "image_upload"
    const val VARIANT_PICKER   = "variant_picker"
    const val LOGO_PICKER      = "logo_picker"
    const val BUILDER          = "builder"
}

@Composable
fun AppForgeNavHost(
    navController: NavHostController = rememberNavController(),
    viewModel: DesignSystemViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    NavHost(navController = navController, startDestination = AppForgeRoutes.ENTRY) {

        composable(AppForgeRoutes.ENTRY) {
            EntryScreen(
                onImageChoice         = { navController.navigate(AppForgeRoutes.IMAGE_UPLOAD) },
                onQuestionnaireChoice = { navController.navigate(AppForgeRoutes.QUESTIONNAIRE) }
            )
        }

        composable(AppForgeRoutes.QUESTIONNAIRE) {
            QuestionnaireScreen(
                onComplete = { answers, appName ->
                    viewModel.generateVariantsFromAnswers(answers, appName)
                    navController.navigate(AppForgeRoutes.VARIANT_PICKER)
                }
            )
        }

        composable(AppForgeRoutes.IMAGE_UPLOAD) {
            ImageUploadScreen(
                onComplete = { variants, appName ->
                    // cargar variantes en viewModel manualmente
                    navController.navigate(AppForgeRoutes.VARIANT_PICKER)
                }
            )
        }

        composable(AppForgeRoutes.VARIANT_PICKER) {
            DesignSystemSelectorScreen(
                uiState           = uiState,
                onVariantSelected = viewModel::selectVariant,
                onContinue        = { navController.navigate(AppForgeRoutes.LOGO_PICKER) }
            )
        }

        composable(AppForgeRoutes.LOGO_PICKER) {
            LogoPickerScreen(
                uiState                = uiState,
                onGenerateLogos        = viewModel::generateLogosWithGrok,
                onSelectGeneratedLogo  = viewModel::selectGeneratedLogo,
                onSelectLocalLogo      = { uri -> viewModel.selectLocalLogo(uri) },
                onUpdatePrompt         = viewModel::updateLogoPrompt,
                onSkip                 = { navController.navigate(AppForgeRoutes.BUILDER) },
                onConfirm              = { navController.navigate(AppForgeRoutes.BUILDER) }
            )
        }

        composable(AppForgeRoutes.BUILDER) {
            BuilderScreen(config = uiState.config)
        }
    }
}


// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// build.gradle.kts — dependencias necesarias
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
/*
dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Coil (AsyncImage para logos)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
}
*/
