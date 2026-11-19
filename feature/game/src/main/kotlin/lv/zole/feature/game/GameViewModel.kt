package lv.zole.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import lv.zole.ai.BotPlayer
import lv.zole.ai.Bots
import lv.zole.core.data.AppSettings
import lv.zole.core.data.MatchRepository
import lv.zole.core.data.SettingsRepository
import lv.zole.engine.Action
import lv.zole.engine.BidChoice
import lv.zole.engine.Card
import lv.zole.engine.DealState
import lv.zole.engine.MatchAction
import lv.zole.engine.MatchState
import lv.zole.engine.Phase
import lv.zole.engine.Play
import lv.zole.engine.PlayerView
import lv.zole.engine.Seat
import kotlin.random.Random

/**
 * Turn timer for the human player. The length is a plain variable so it is easy to tune while testing;
 * the vibration points (half time, 5 s left) are derived from it.
 */
object TurnTimer {
    var durationMs: Long = 15_000
    const val WARNING_MS: Long = 5_000
}

enum class Vibe { TURN_START, HALF_TIME, FIVE_LEFT, TRICK_WON }

val HUMAN = Seat(0)

data class GameUi(
    val match: MatchState? = null,
    val view: PlayerView? = null,
    /** Trick shown on the table: the trick in progress, or a finished one held for a moment. */
    val shownTrick: List<Play> = emptyList(),
    val thinking: Seat? = null,
    /** When the human's current timed turn started (System.currentTimeMillis), or null. */
    val turnStartedAt: Long? = null,
    val busy: Boolean = false,
)

class GameViewModel(
    private val repository: MatchRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _ui = MutableStateFlow(GameUi())
    val ui: StateFlow<GameUi> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<Vibe>(extraBufferCapacity = 8)
    val events: SharedFlow<Vibe> = _events.asSharedFlow()

    private val actionLock = Mutex()
    private var revealing: List<Play>? = null
    private var thinking: Seat? = null
    private var turnStartedAt: Long? = null
    private var timerJob: Job? = null
    private var timedTurnKey: String? = null
    private val random = Random(System.nanoTime())
    /** Bumped on every state publish so the bot loop can wait for "something changed". */
    private val tick = MutableStateFlow(0L)

    private val fast: Boolean get() = settings.value.fastAnimations

    init {
        viewModelScope.launch {
            repository.load()
            repository.state.collect { publish() }
        }
        viewModelScope.launch {
            repository.loaded.first { it }
            driveBots()
        }
    }

    private fun publish() {
        val match = repository.state.value
        val deal = match?.deal
        val view = deal?.let { PlayerView.of(it, HUMAN) }
        val trick = revealing ?: (deal?.phase as? Phase.Playing)?.trick ?: emptyList()
        _ui.value = GameUi(
            match = match,
            view = view,
            shownTrick = trick,
            thinking = thinking,
            turnStartedAt = turnStartedAt,
            busy = revealing != null,
        )
        if (deal != null) updateHumanTimer(match, deal)
        tick.value = tick.value + 1
    }

    private fun turnOf(deal: DealState): Seat? = when (val p = deal.phase) {
        is Phase.Bidding -> p.turn
        is Phase.Discarding -> p.declarer
        is Phase.Playing -> p.turn
        is Phase.Finished -> null
    }

    /** Main loop: whenever a bot is to act, think for a moment, then act. */
    private suspend fun driveBots() {
        while (true) {
            val seen = tick.value
            val match = repository.state.value
            val deal = match?.deal
            val turn = deal?.let { turnOf(it) }
            if (match == null || deal == null || turn == null || turn == HUMAN || revealing != null) {
                tick.first { it != seen }
                continue
            }
            thinking = turn
            publish()
            delay(((if (deal.phase is Phase.Bidding || deal.phase is Phase.Discarding) 650L else 450L) + random.nextLong(0, 300)).scaled())
            val strategy = Bots.strategy(match.config.difficulty, match.seed * 31 + match.dealNumber * 7L + turn.index + deal.tricks.size * 101L)
            val player = BotPlayer(strategy)
            val view = PlayerView.of(deal, turn)
            val action: Action = when (val p = deal.phase) {
                is Phase.Bidding -> Action.Bid(turn, player.bid(view))
                is Phase.Discarding -> Action.Discard(turn, player.discard(view))
                is Phase.Playing -> Action.PlayCard(turn, player.play(view))
                is Phase.Finished -> continue
            }
            thinking = null
            if (repository.state.value === match) apply(action) else publish()
        }
    }

    private suspend fun apply(action: Action) {
        actionLock.withLock {
            val before = repository.state.value ?: return
            if (before.deal.isFinished) return
            val after = runCatching { repository.apply(MatchAction.InDeal(action)) }.getOrNull() ?: return
            if (action.seat == HUMAN) stopHumanTimer()
            if (after.deal.tricks.size > before.deal.tricks.size) {
                val trick = after.deal.tricks.last()
                revealing = trick.plays
                publish()
                if (trick.winner == HUMAN) _events.tryEmit(Vibe.TRICK_WON)
                delay(if (fast) 450 else 950)
                revealing = null
            }
            publish()
        }
    }

    // ---- Human actions ----

    fun bid(choice: BidChoice) = humanAction(Action.Bid(HUMAN, choice))
    fun discard(cards: List<Card>) = humanAction(Action.Discard(HUMAN, cards))
    fun play(card: Card) = humanAction(Action.PlayCard(HUMAN, card))

    private fun humanAction(action: Action) {
        val deal = repository.state.value?.deal ?: return
        if (revealing != null || turnOf(deal) != HUMAN) return
        viewModelScope.launch { apply(action) }
    }

    fun nextDeal() {
        val match = repository.state.value ?: return
        if (!match.deal.isFinished || match.isOver || revealing != null) return
        runCatching { repository.apply(MatchAction.NextDeal) }
    }

    // ---- Turn timer ----

    private fun updateHumanTimer(match: MatchState, deal: DealState) {
        val phase = deal.phase
        val timed = revealing == null && turnOf(deal) == HUMAN && (phase is Phase.Bidding || phase is Phase.Playing)
        if (!timed) {
            if (timedTurnKey != null) stopHumanTimer()
            return
        }
        val key = "${match.dealNumber}/${deal.bids.size}/${deal.tricks.size}/${(phase as? Phase.Playing)?.trick?.size}"
        if (key == timedTurnKey) return
        timedTurnKey = key
        val start = System.currentTimeMillis()
        turnStartedAt = start
        _ui.value = _ui.value.copy(turnStartedAt = start)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val total = TurnTimer.durationMs
            _events.tryEmit(Vibe.TURN_START)
            val half = total / 2
            val warn = (total - TurnTimer.WARNING_MS).coerceAtLeast(0)
            val marks = if (kotlin.math.abs(half - warn) < 1_000) listOf(warn to Vibe.FIVE_LEFT) else listOf(half to Vibe.HALF_TIME, warn to Vibe.FIVE_LEFT)
            var elapsed = 0L
            for ((at, vibe) in marks.sortedBy { it.first }) {
                if (at <= 0) continue
                delay(at - elapsed)
                elapsed = at
                _events.tryEmit(vibe)
            }
            delay(total - elapsed)
            onTimeout()
        }
    }

    private fun stopHumanTimer() {
        timerJob?.cancel()
        timerJob = null
        timedTurnKey = null
        turnStartedAt = null
    }

    /** Time is up: pass in bidding, or play the cheapest legal card. */
    private fun onTimeout() {
        val deal = repository.state.value?.deal ?: return
        when (deal.phase) {
            is Phase.Bidding -> bid(BidChoice.PASS)
            is Phase.Playing -> {
                val legal = PlayerView.of(deal, HUMAN).legalCards()
                legal.minWithOrNull(compareBy<Card>({ it.points }, { if (it.isTrump) 1 else 0 }, { it.strength }))?.let { play(it) }
            }
            else -> {}
        }
    }

    private fun Long.scaled(): Long = if (fast) this / 2 else this
}
