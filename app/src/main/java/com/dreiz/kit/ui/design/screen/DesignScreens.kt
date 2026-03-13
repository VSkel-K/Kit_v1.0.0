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

@Composable
fun EntryScreen(onImageChoice: () -> Unit, onQuestionnaireChoice: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF070A0F)).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("KIT", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text("AI DESIGN SYSTEM FORGE", color = Color(0xFF06B6D4), fontSize = 10.sp, letterSpacing = 4.sp)
        Spacer(Modifier.height(48.dp))
        
        Button(
            onClick = onQuestionnaireChoice,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
        ) {
            Text("Iniciar Cuestionario", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun QuestionnaireScreen(onComplete: (Map<String, List<String>>, String) -> Unit) {
    // Versión simplificada para demo
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF070A0F)).padding(24.dp)) {
        Text("Configura tu App", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Button(onClick = { onComplete(emptyMap(), "My Awesome App") }) {
            Text("Generar con valores por defecto")
        }
    }
}

@Composable
fun DesignSystemSelectorScreen(
    uiState: DesignSystemUiState,
    onVariantSelected: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF070A0F)).padding(24.dp)) {
        Text("Elige un Estilo", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (uiState.isGenerating) {
            CircularProgressIndicator(color = Color(0xFF06B6D4))
        } else {
            uiState.variants.forEach { variant ->
                Button(onClick = { onVariantSelected(variant.id) }) {
                    Text(variant.name)
                }
            }
            if (uiState.selectedVariantId != null) {
                Button(onClick = onContinue) { Text("Continuar") }
            }
        }
    }
}

@Composable
fun BuilderScreen(config: AppForgeConfig) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF070A0F)).padding(24.dp)) {
        Text("PROYECTO LISTO", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("App: ${config.appName}", color = Color.Gray)
        Text("Estilo: ${config.selectedVariant?.name ?: "N/A"}", color = Color.Gray)
    }
}
