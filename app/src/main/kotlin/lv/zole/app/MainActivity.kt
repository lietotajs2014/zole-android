package lv.zole.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import lv.zole.core.data.AppSettings
import lv.zole.core.data.LanguageSetting
import lv.zole.core.data.ThemeSetting
import lv.zole.core.ui.BlackColors
import lv.zole.core.ui.CardStyle
import lv.zole.core.ui.Language
import lv.zole.core.ui.LightColors
import lv.zole.core.ui.ThemeMode
import lv.zole.core.ui.ZoleTheme
import lv.zole.engine.MatchConfig
import lv.zole.engine.Rules
import lv.zole.feature.game.GameRoute
import lv.zole.feature.game.GameViewModel
import lv.zole.feature.menu.MainScreen
import lv.zole.feature.menu.NewGameChoice
import lv.zole.feature.menu.NewGameScreen
import lv.zole.feature.menu.RulesScreen
import lv.zole.feature.settings.SettingsRoute
import lv.zole.feature.settings.SettingsViewModel
import lv.zole.feature.stats.ScoreboardRoute
import lv.zole.feature.stats.StatsRoute
import lv.zole.feature.stats.StatsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as ZoleApp).container
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            val mode = when (settings.theme) {
                ThemeSetting.LIGHT -> ThemeMode.LIGHT
                ThemeSetting.BLACK -> ThemeMode.BLACK
                ThemeSetting.SYSTEM -> ThemeMode.SYSTEM
            }
            val language = when (settings.language) {
                LanguageSetting.LV -> Language.LV
                LanguageSetting.EN -> Language.EN
            }
            ZoleTheme(mode = mode, cardStyle = CardStyle(markTrumps = settings.markTrumps), language = language) {
                SystemBars(mode)
                ZoleNavigation(container, settings)
            }
        }
    }

    @Composable
    private fun SystemBars(mode: ThemeMode) {
        val dark = when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.BLACK -> true
            ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        }
        LaunchedEffect(dark) {
            val style = if (dark) SystemBarStyle.dark(BlackColors.background.toArgb())
            else SystemBarStyle.light(LightColors.background.toArgb(), BlackColors.background.toArgb())
            enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        }
    }
}

private object Routes {
    const val MAIN = "main"
    const val NEW_GAME = "new"
    const val GAME = "game"
    const val SETTINGS = "settings"
    const val RULES = "rules"
    const val SCOREBOARD = "scoreboard"
    const val STATS = "stats"
}

@Composable
private fun ZoleNavigation(container: AppContainer, settings: AppSettings) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    NavHost(navController = nav, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            val match by container.matches.state.collectAsStateWithLifecycle()
            val continueDeal = match?.takeIf { !it.isOver }?.dealNumber
            MainScreen(
                continueDeal = continueDeal,
                onPlay = { nav.navigate(Routes.NEW_GAME) },
                onContinue = { nav.navigate(Routes.GAME) },
                onStats = { nav.navigate(Routes.STATS) },
                onRules = { nav.navigate(Routes.RULES) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.NEW_GAME) {
            NewGameScreen(
                initial = NewGameChoice(settings.difficulty, settings.allPassMode, settings.dealLimit, settings.scoring),
                onBack = { nav.popBackStack() },
                onStart = { choice ->
                    scope.launch {
                        container.settings.update {
                            it.copy(difficulty = choice.difficulty, allPassMode = choice.allPassMode, dealLimit = choice.dealLimit, scoring = choice.scoring)
                        }
                    }
                    container.matches.start(
                        MatchConfig(
                            rules = Rules(allPassMode = choice.allPassMode, smallZoleEnabled = settings.smallZole, scoring = choice.scoring),
                            difficulty = choice.difficulty,
                            dealLimit = choice.dealLimit.takeIf { it > 0 },
                        ),
                    )
                    nav.navigate(Routes.GAME) { popUpTo(Routes.MAIN) }
                },
            )
        }
        composable(Routes.GAME) {
            val vm: GameViewModel = viewModel(factory = viewModelFactory { initializer { GameViewModel(container.matches, container.settings) } })
            GameRoute(
                viewModel = vm,
                onMenu = { if (!nav.popBackStack(Routes.MAIN, inclusive = false)) nav.navigate(Routes.MAIN) },
                onScoreboard = { nav.navigate(Routes.SCOREBOARD) },
            )
        }
        composable(Routes.SCOREBOARD) {
            val vm: StatsViewModel = viewModel(factory = viewModelFactory { initializer { StatsViewModel(container.matches, container.database.history()) } })
            ScoreboardRoute(vm, onBack = { nav.popBackStack() }, onBackToTable = {
                if (!nav.popBackStack(Routes.GAME, inclusive = false)) nav.navigate(Routes.GAME) { popUpTo(Routes.MAIN) }
            })
        }
        composable(Routes.STATS) {
            val vm: StatsViewModel = viewModel(factory = viewModelFactory { initializer { StatsViewModel(container.matches, container.database.history()) } })
            StatsRoute(vm, onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(factory = viewModelFactory { initializer { SettingsViewModel(container.settings) } })
            SettingsRoute(vm, BuildConfig.VERSION_NAME, onBack = { nav.popBackStack() })
        }
        composable(Routes.RULES) {
            RulesScreen(settings.scoring, onBack = { nav.popBackStack() })
        }
    }
}
