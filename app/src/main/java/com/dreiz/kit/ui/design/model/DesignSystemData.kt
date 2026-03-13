package com.dreiz.kit.ui.design.model

import androidx.compose.ui.graphics.Color
import android.net.Uri

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
    data class FromGallery(val uri: Uri) : LogoSource()
    data class FromCamera(val uri: Uri) : LogoSource()
    data class FromAI(val imageUrl: String, val prompt: String) : LogoSource()
}

data class AppForgeConfig(
    val appName: String = "",
    val selectedVariant: DesignSystemVariant? = null,
    val logoSource: LogoSource = LogoSource.None,
    val screens: List<String> = listOf("Home", "Perfil", "Explorar")
)
