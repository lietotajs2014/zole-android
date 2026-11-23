package lv.zole.engine

import kotlinx.serialization.Serializable

@Serializable
enum class Outcome {
    WIN, WIN_JANI, WIN_ALL_TRICKS,
    LOSE, LOSE_JANI, LOSE_NO_TRICKS,
    SMALL_ZOLE_WIN, SMALL_ZOLE_LOSE,
    TABLE, ALL_PASSED;

    val declarerWon: Boolean get() = this == WIN || this == WIN_JANI || this == WIN_ALL_TRICKS || this == SMALL_ZOLE_WIN
}

/** Final result of one deal, including game points for each seat (before pules). */
@Serializable
data class DealResult(
    val contract: Contract?,
    val declarer: Seat?,
    val outcome: Outcome,
    /** Eyes ("acis") of the declarer's side, including the discard or talon where it belongs. */
    val declarerPoints: Int,
    val defenderPoints: Int,
    val tricksBySeat: Map<Seat, Int>,
    val pointsBySeat: Map<Seat, Int>,
    val scores: Map<Seat, Int>,
    /** Galds only: who lost. */
    val tableLoser: Seat? = null,
) {
    companion object {
        fun allPassed(): DealResult = DealResult(
            contract = null, declarer = null, outcome = Outcome.ALL_PASSED,
            declarerPoints = 0, defenderPoints = 0,
            tricksBySeat = Seat.ALL.associateWith { 0 }, pointsBySeat = Seat.ALL.associateWith { 0 },
            scores = Seat.ALL.associateWith { 0 },
        )

        fun of(state: DealState): DealResult {
            val rules = state.rules
            val contract = checkNotNull(state.contract)
            val tricksBySeat = Seat.ALL.associateWith { seat -> state.tricksOf(seat).size }
            val pointsBySeat = Seat.ALL.associateWith { seat -> state.tricksOf(seat).sumOf { it.points } }.toMutableMap()
            if (contract == Contract.TABLE) {
                // Galds: the talon goes to whoever took the first trick.
                state.tricks.firstOrNull()?.let { first ->
                    pointsBySeat[first.winner] = pointsBySeat.getValue(first.winner) + state.talon.sumOf { it.points }
                }
                val seatOrder = compareByDescending<Seat> { (it.index - state.firstHand.index + 3) % 3 }
                val scores: Map<Seat, Int>
                val loser: Seat
                if (rules.scoring == ScoringVariant.RONDO) {
                    // Rondo: most eyes loses; each winner takes 3 (no tricks), 2 (up to 30 eyes) or 1.
                    loser = Seat.ALL.maxWith(
                        compareBy<Seat>({ pointsBySeat.getValue(it) }, { tricksBySeat.getValue(it) }).then(seatOrder),
                    )
                    val won = Seat.ALL.filter { it != loser }.associateWith { seat ->
                        when {
                            tricksBySeat.getValue(seat) == 0 -> rules.rondoNoTricks
                            pointsBySeat.getValue(seat) <= Rules.DECLARER_JANI_LOSE -> rules.rondoJani
                            else -> rules.rondoWin
                        }
                    }
                    scores = Seat.ALL.associateWith { won[it] ?: -won.values.sum() }
                } else {
                    loser = Seat.ALL.maxWith(
                        compareBy<Seat>({ tricksBySeat.getValue(it) }, { pointsBySeat.getValue(it) }).then(seatOrder),
                    )
                    scores = Seat.ALL.associateWith { if (it == loser) -2 * rules.tableLoserPays else rules.tableLoserPays }
                }
                return DealResult(
                    contract, null, Outcome.TABLE, 0, 0, tricksBySeat, pointsBySeat, scores, tableLoser = loser,
                )
            }
            val declarer = checkNotNull(state.declarer)
            val declarerTricks = tricksBySeat.getValue(declarer)
            val totalTricks = state.tricks.size
            var declarerPoints = pointsBySeat.getValue(declarer) + state.discarded.sumOf { it.points }
            if (contract == Contract.NORMAL) pointsBySeat[declarer] = declarerPoints
            // In zole and mazā zole the talon belongs to the defenders; in a broken mazā zole the
            // unplayed cards do not matter, so defenders simply get the rest.
            val defenderPoints = Deck.TOTAL_POINTS - declarerPoints
            val outcome = when (contract) {
                Contract.SMALL_ZOLE -> if (declarerTricks == 0) Outcome.SMALL_ZOLE_WIN else Outcome.SMALL_ZOLE_LOSE
                else -> when {
                    declarerTricks == totalTricks && totalTricks == 8 -> Outcome.WIN_ALL_TRICKS
                    declarerPoints >= Rules.DECLARER_JANI_WIN -> Outcome.WIN_JANI
                    declarerPoints >= Rules.WIN_POINTS -> Outcome.WIN
                    declarerTricks == 0 -> Outcome.LOSE_NO_TRICKS
                    declarerPoints <= Rules.DECLARER_JANI_LOSE -> Outcome.LOSE_JANI
                    else -> Outcome.LOSE
                }
            }
            val perOpponent = when (contract) {
                Contract.SMALL_ZOLE -> if (outcome == Outcome.SMALL_ZOLE_WIN) rules.smallZoleWin else rules.smallZoleLose
                Contract.ZOLE -> rules.zole.valueFor(outcome)
                else -> rules.normal.valueFor(outcome)
            }
            val scores = Seat.ALL.associateWith { if (it == declarer) 2 * perOpponent else -perOpponent }
            return DealResult(
                contract, declarer, outcome, declarerPoints, defenderPoints,
                tricksBySeat, pointsBySeat, scores,
            )
        }

        private fun ContractScores.valueFor(outcome: Outcome): Int = when (outcome) {
            Outcome.WIN -> win
            Outcome.WIN_JANI -> winJani
            Outcome.WIN_ALL_TRICKS -> winAllTricks
            Outcome.LOSE -> lose
            Outcome.LOSE_JANI -> loseJani
            Outcome.LOSE_NO_TRICKS -> loseNoTricks
            else -> 0
        }
    }
}
