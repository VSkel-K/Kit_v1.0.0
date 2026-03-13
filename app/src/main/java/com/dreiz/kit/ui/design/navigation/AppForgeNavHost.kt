package com.dreiz.kit.ui.design.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.dreiz.kit.ui.design.screen.*
import com.dreiz.kit.ui.design.viewmodel.*

object AppForgeRoutes {
    const val ENTRY            = "entry"
    const val QUESTIONNAIRE    = "questionnaire"
    const val VARIANT_PICKER   = "variant_picker"
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
                onImageChoice         = { /* TODO: Implementar upload de imagen */ },
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

        composable(AppForgeRoutes.VARIANT_PICKER) {
            DesignSystemSelectorScreen(
                uiState           = uiState,
                onVariantSelected = viewModel::selectVariant,
                onContinue        = { navController.navigate(AppForgeRoutes.BUILDER) }
            )
        }

        composable(AppForgeRoutes.BUILDER) {
            BuilderScreen(config = uiState.config)
        }
    }
}
