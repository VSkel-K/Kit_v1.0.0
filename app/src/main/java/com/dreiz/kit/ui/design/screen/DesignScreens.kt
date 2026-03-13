package com.dreiz.kit.ui.design.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreiz.kit.ui.design.model.*
import com.dreiz.kit.ui.design.viewmodel.*
import com.dreiz.kit.ui.theme.AzulCobalto
import com.dreiz.kit.ui.theme.FondoOscuroClay
import com.dreiz.kit.ui.theme.Mandarina
import com.dreiz.kit.ui.theme.Hueso
import com.dreiz.kit.ui.theme.GrisArcilla
import com.dreiz.kit.ui.theme.components.ClayContainer

@Composable
fun EntryScreen(onImageChoice: () -> Unit, onQuestionnaireChoice: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(FondoOscuroClay).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("KIT", color = Hueso, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text("AI DESIGN SYSTEM FORGE", color = AzulCobalto, fontSize = 10.sp, letterSpacing = 4.sp)
        Spacer(Modifier.height(48.dp))
        
        ClayContainer(
            onClick = onQuestionnaireChoice,
            backgroundColor = Mandarina,
            cornerRadius = 24.dp,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Iniciar Cuestionario", fontWeight = FontWeight.Bold, color = FondoOscuroClay)
        }
        Spacer(Modifier.height(16.dp))
        ClayContainer(
            onClick = onImageChoice,
            backgroundColor = GrisArcilla,
            cornerRadius = 24.dp,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Subir Imagen", fontWeight = FontWeight.Bold, color = Hueso)
        }
    }
}

@Composable
fun QuestionnaireScreen(onComplete: (Map<String, List<String>>, String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(FondoOscuroClay).padding(24.dp)) {
        Text("Configura tu App", color = Hueso, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        ClayContainer(
            onClick = { onComplete(emptyMap(), "My Awesome App") },
            backgroundColor = AzulCobalto,
            cornerRadius = 24.dp,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Generar con valores por defecto", color = Hueso)
        }
    }
}

@Composable
fun DesignSystemSelectorScreen(
    uiState: DesignSystemUiState,
    onVariantSelected: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(FondoOscuroClay).padding(24.dp)) {
        Text("Elige un Estilo", color = Hueso, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (uiState.isGenerating) {
            CircularProgressIndicator(color = AzulCobalto)
        } else {
            uiState.variants.forEach { variant ->
                ClayContainer(
                    onClick = { onVariantSelected(variant.id) },
                    backgroundColor = GrisArcilla,
                    cornerRadius = 16.dp,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(variant.name, color = Hueso)
                }
                Spacer(Modifier.height(8.dp))
            }
            if (uiState.selectedVariantId != null) {
                Spacer(Modifier.height(16.dp))
                ClayContainer(
                    onClick = onContinue,
                    backgroundColor = Mandarina,
                    cornerRadius = 24.dp,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Continuar", color = FondoOscuroClay)
                }
            }
        }
    }
}

@Composable
fun BuilderScreen(config: AppForgeConfig) {
    Column(modifier = Modifier.fillMaxSize().background(FondoOscuroClay).padding(24.dp)) {
        Text("PROYECTO LISTO", color = Hueso, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("App: ${config.appName}", color = Hueso.copy(alpha = 0.7f))
        Text("Estilo: ${config.selectedVariant?.name ?: "N/A"}", color = Hueso.copy(alpha = 0.7f))
    }
}
