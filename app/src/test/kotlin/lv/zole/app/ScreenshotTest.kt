package lv.zole.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import lv.zole.core.data.AppSettings
import lv.zole.core.data.PlayerStats
import lv.zole.core.ui.Language
import lv.zole.core.ui.ThemeMode
import lv.zole.core.ui.ZoleTheme
import lv.zole.engine.Action
import lv.zole.engine.AllPassMode
import lv.zole.engine.BidChoice
import lv.zole.engine.DealEngine
import lv.zole.engine.Difficulty
import lv.zole.engine.MatchAction
import lv.zole.engine.MatchConfig
import lv.zole.engine.MatchEngine
import lv.zole.engine.MatchState
import lv.zole.engine.Phase
import lv.zole.engine.PlayerView
import lv.zole.engine.Rules
import lv.zole.engine.ScoringVariant
import lv.zole.engine.Seat
import lv.zole.feature.game.GameScreen
import lv.zole.feature.game.GameUi
import lv.zole.feature.game.TrickTiming
import lv.zole.feature.menu.MainScreen
import lv.zole.feature.menu.NewGameChoice
import lv.zole.feature.menu.NewGameScreen
import lv.zole.feature.menu.RulesScreen
import lv.zole.feature.settings.SettingsScreen
import lv.zole.feature.stats.ScoreboardScreen
import lv.zole.feature.stats.StatsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders every screen on the JVM so the layout can be checked against the designs without a device. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h844dp-xxhdpi", sdk = [34])
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun shot(name: String, mode: ThemeMode = ThemeMode.BLACK, language: Language = Language.LV, advanceMs: Long = 2_000, content: @Composable () -> Unit) {
        // The turn timer animates every frame, so drive the clock by hand.
        compose.mainClock.autoAdvance = false
        compose.setContent { ZoleTheme(mode, language = language) { content() } }
        compose.mainClock.advanceTimeBy(advanceMs)
        compose.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    private val config = MatchConfig(Rules(allPassMode = AllPassMode.POOL), Difficulty.MEDIUM, 24)

    /** Plays a few deals with simple choices to get a realistic match state. */
    private fun matchAfterDeals(deals: Int): MatchState {
        var m = MatchEngine.start(config, 7)
        while (m.history.size < deals) {
            m = if (m.deal.isFinished) MatchEngine.reduce(m, MatchAction.NextDeal) else MatchEngine.reduce(m, MatchAction.InDeal(simpleAction(m)))
        }
        return m
    }

    private fun simpleAction(m: MatchState): Action {
        val d = m.deal
        return when (val p = d.phase) {
            is Phase.Bidding -> Action.Bid(p.turn, if (p.turn.index == (m.dealNumber % 3)) BidChoice.TAKE else BidChoice.PASS)
            is Phase.Discarding -> Action.Discard(p.declarer, d.hands.getValue(p.declarer).takeLast(2))
            is Phase.Playing -> Action.PlayCard(p.turn, DealEngine.legalCards(d, p.turn).first())
            is Phase.Finished -> error("finished")
        }
    }

    private fun ui(m: MatchState) = GameUi(match = m, view = PlayerView.of(m.deal, Seat(0)), shownTrick = (m.deal.phase as? Phase.Playing)?.trick ?: emptyList(), turnStartedAt = System.currentTimeMillis() - 6000)

    @Composable
    private fun Game(m: MatchState) = GameScreen(ui(m), AppSettings(), {}, {}, {}, {}, {}, {})

    @Test fun main() = shot("1_main") { MainScreen(7, {}, {}, {}, {}, {}) }
    @Test fun mainLight() = shot("1_main_light", ThemeMode.LIGHT) { MainScreen(7, {}, {}, {}, {}, {}) }
    @Test fun newGame() = shot("2_new_game") { NewGameScreen(NewGameChoice(Difficulty.MEDIUM, AllPassMode.POOL, 24, ScoringVariant.CLASSIC), {}, {}) }
    @Test fun settings() = shot("3_settings") { SettingsScreen(AppSettings(), "0.1", {}, {}) }
    @Test fun scoreboard() = shot("4_scoreboard") { ScoreboardScreen(matchAfterDeals(7), {}, {}) }
    @Test fun stats() = shot("4b_stats") { StatsScreen(PlayerStats(deals = 40, asDeclarer = 12, declarerWins = 8), {}) }
    @Test fun settingsEnglish() = shot("3_settings_en", language = Language.EN) { SettingsScreen(AppSettings(), "0.1", {}, {}) }
    @Test fun rules() = shot("9_rules") { RulesScreen(ScoringVariant.CLASSIC) {} }
    @Test fun rulesRondoEnglish() = shot("9_rules_rondo_en", language = Language.EN) { RulesScreen(ScoringVariant.RONDO) {} }

    private val debugSettings = AppSettings(debugMode = true, debugShowBotHands = true, debugTimerSeconds = 0)
    @Config(qualifiers = "w390dp-h2200dp-xxhdpi")
    @Test fun settingsDebug() = shot("3_settings_debug") { SettingsScreen(debugSettings, "0.1", {}, {}) }
    @Test fun biddingDebug() = shot("5_bidding_debug") { GameScreen(ui(MatchEngine.start(config, 7)), debugSettings, {}, {}, {}, {}, {}, {}) }

    @Test fun bidding() {
        // Deal 1: dealer is seat 2, so seat 0 bids first.
        val m = MatchEngine.start(config, 7)
        shot("5_bidding") { Game(m) }
    }

    @Test fun discard() {
        var m = MatchEngine.start(config, 7)
        m = MatchEngine.reduce(m, MatchAction.InDeal(Action.Bid(Seat(0), BidChoice.TAKE)))
        shot("6_discard") { Game(m) }
    }

    @Test fun play() = shot("7_play") { Game(playState(config)) }
    @Test fun playHard() = shot("7_play_hard") { Game(playState(config.copy(difficulty = Difficulty.HARD))) }

    /** Halfway through sweeping the first finished trick to its winner. */
    @Test fun trickCollect() {
        val m = playState(config)
        val trick = m.deal.tricks.first().plays
        val ui = GameUi(match = m, view = PlayerView.of(m.deal, Seat(0)), shownTrick = trick, busy = true)
        shot("7b_trick_collect", advanceMs = TrickTiming.HOLD_MS + TrickTiming.COLLECT_MS / 2) { GameScreen(ui, AppSettings(), {}, {}, {}, {}, {}, {}) }
    }

    private fun playState(config: MatchConfig): MatchState {
        var m = MatchEngine.start(config, 7)
        m = MatchEngine.reduce(m, MatchAction.InDeal(Action.Bid(Seat(0), BidChoice.PASS)))
        m = MatchEngine.reduce(m, MatchAction.InDeal(Action.Bid(Seat(1), BidChoice.TAKE)))
        m = MatchEngine.reduce(m, MatchAction.InDeal(Action.Discard(Seat(1), m.deal.hands.getValue(Seat(1)).takeLast(2))))
        // Seat 0 leads the first trick; play one full trick and then two cards of the next.
        repeat(5) {
            val p = m.deal.phase as Phase.Playing
            if (p.turn == Seat(0) && it >= 3) return@repeat
            m = MatchEngine.reduce(m, MatchAction.InDeal(Action.PlayCard(p.turn, DealEngine.legalCards(m.deal, p.turn).last())))
        }
        return m
    }

    @Test fun biddingEnglish() = shot("5_bidding_en", language = Language.EN) { Game(MatchEngine.start(config, 7)) }

    @Test fun result() {
        var m = MatchEngine.start(config, 7)
        m = MatchEngine.reduce(m, MatchAction.InDeal(Action.Bid(Seat(0), BidChoice.TAKE)))
        while (!m.deal.isFinished) m = MatchEngine.reduce(m, MatchAction.InDeal(simpleAction(m)))
        shot("8_result") { Game(m) }
    }
}
