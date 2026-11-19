package lv.zole.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Difficulty { EASY, MEDIUM, HARD }

@Serializable
data class MatchConfig(
    val rules: Rules = Rules(),
    val difficulty: Difficulty = Difficulty.MEDIUM,
    /** Number of deals in the match, or null for no limit. */
    val dealLimit: Int? = 24,
)

/** Common and personal pules ("pules"). They live across deals, so they belong to the match. */
@Serializable
data class PoolLedger(
    val common: Int = 0,
    val personal: List<Seat> = emptyList(),
) {
    val isEmpty: Boolean get() = common == 0 && personal.isEmpty()
    val total: Int get() = common + personal.size

    /**
     * Applies a finished deal to the pules. Returns the new ledger and extra game points per seat.
     * - Everyone passed (pules mode): one more common pule.
     * - Declarer wins: takes a common pule (+value from each player) or else another player's personal
     *   pule (+value from its owner); their own personal pule is simply cleared.
     * - Declarer loses while any pule is open: they get a personal pule.
     */
    fun settle(result: DealResult, rules: Rules): Pair<PoolLedger, Map<Seat, Int>> {
        val zero = Seat.ALL.associateWith { 0 }
        if (rules.allPassMode != AllPassMode.POOL) return this to zero
        if (result.outcome == Outcome.ALL_PASSED) return copy(common = common + 1) to zero
        val declarer = result.declarer ?: return this to zero
        if (isEmpty) return this to zero
        val delta = zero.toMutableMap()
        if (result.outcome.declarerWon) {
            return when {
                common > 0 -> {
                    Seat.ALL.filter { it != declarer }.forEach {
                        delta[it] = delta.getValue(it) - rules.commonPoolValue
                        delta[declarer] = delta.getValue(declarer) + rules.commonPoolValue
                    }
                    copy(common = common - 1) to delta
                }
                personal.any { it != declarer } -> {
                    val owner = personal.first { it != declarer }
                    delta[owner] = -rules.personalPoolValue
                    delta[declarer] = rules.personalPoolValue
                    copy(personal = personal - owner) to delta
                }
                else -> copy(personal = personal - declarer) to delta
            }
        }
        return copy(personal = personal + declarer) to delta
    }
}

/** One row of the score sheet ("pieraksts"). */
@Serializable
data class DealRecord(
    val number: Int,
    val dealer: Seat,
    val result: DealResult,
    /** Game points for this deal including pules. */
    val deltas: Map<Seat, Int>,
    /** Running totals after this deal. */
    val totals: Map<Seat, Int>,
    val poolsAfter: PoolLedger,
)

@Serializable
sealed interface MatchAction {
    @Serializable @SerialName("deal")
    data class InDeal(val action: Action) : MatchAction

    @Serializable @SerialName("next")
    data object NextDeal : MatchAction
}

@Serializable
data class MatchState(
    val config: MatchConfig,
    val seed: Long,
    val dealNumber: Int,
    val deal: DealState,
    val totals: Map<Seat, Int>,
    val pools: PoolLedger,
    val history: List<DealRecord>,
) {
    val isOver: Boolean get() = config.dealLimit != null && history.size >= config.dealLimit && deal.isFinished
    val lastRecord: DealRecord? get() = history.lastOrNull()
}

/** Everything needed to restore a match: config, seed and the list of actions. */
@Serializable
data class MatchSnapshot(
    val config: MatchConfig,
    val seed: Long,
    val firstDealer: Seat,
    val actions: List<MatchAction>,
)

object MatchEngine {

    fun start(config: MatchConfig, seed: Long, firstDealer: Seat = Seat(2)): MatchState = MatchState(
        config = config,
        seed = seed,
        dealNumber = 1,
        deal = DealState.deal(config.rules, firstDealer, dealSeed(seed, 1)),
        totals = Seat.ALL.associateWith { 0 },
        pools = PoolLedger(),
        history = emptyList(),
    )

    fun reduce(state: MatchState, action: MatchAction): MatchState = when (action) {
        is MatchAction.InDeal -> {
            val deal = DealEngine.reduce(state.deal, action.action)
            val next = state.copy(deal = deal)
            val phase = deal.phase
            if (phase is Phase.Finished) record(next, phase.result) else next
        }
        MatchAction.NextDeal -> {
            if (!state.deal.isFinished) throw IllegalActionException("Deal is not finished")
            if (state.isOver) throw IllegalActionException("Match is over")
            val number = state.dealNumber + 1
            state.copy(
                dealNumber = number,
                deal = DealState.deal(state.config.rules, state.deal.dealer.next, dealSeed(state.seed, number)),
            )
        }
    }

    fun replay(snapshot: MatchSnapshot): MatchState =
        snapshot.actions.fold(start(snapshot.config, snapshot.seed, snapshot.firstDealer)) { s, a -> reduce(s, a) }

    private fun record(state: MatchState, result: DealResult): MatchState {
        val (pools, poolDelta) = state.pools.settle(result, state.config.rules)
        val deltas = Seat.ALL.associateWith { result.scores.getValue(it) + poolDelta.getValue(it) }
        val totals = Seat.ALL.associateWith { state.totals.getValue(it) + deltas.getValue(it) }
        val record = DealRecord(state.dealNumber, state.deal.dealer, result, deltas, totals, pools)
        return state.copy(totals = totals, pools = pools, history = state.history + record)
    }

    fun dealSeed(matchSeed: Long, dealNumber: Int): Long = matchSeed * 6_364_136_223_846_793_005L + dealNumber * 1_442_695_040_888_963_407L
}
