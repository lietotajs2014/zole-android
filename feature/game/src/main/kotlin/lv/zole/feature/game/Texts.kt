package lv.zole.feature.game

import lv.zole.engine.BidChoice
import lv.zole.engine.Contract
import lv.zole.engine.Outcome
import lv.zole.engine.Seat

internal val NAMES = listOf("Tu", "Anna", "Jānis")
internal fun Seat.displayName() = NAMES[index]
internal fun Seat.initial() = NAMES[index].first().toString()
private val FEMININE = setOf(1)

internal fun BidChoice.label() = when (this) {
    BidChoice.PASS -> "Garām"
    BidChoice.TAKE -> "Ņemu"
    BidChoice.ZOLE -> "Zole"
    BidChoice.SMALL_ZOLE -> "Mazā zole"
}

/** "lielais/lielā" or "mazais/mazā" for the opponents' status line. */
internal fun roleOf(seat: Seat, declarer: Seat?, contract: Contract?): String = when {
    contract == Contract.TABLE -> "galdiņš"
    declarer == null -> ""
    seat == declarer -> if (seat.index in FEMININE) "lielā" else "lielais"
    else -> if (seat.index in FEMININE) "mazā" else "mazais"
}

internal fun cardsWord(n: Int): String = if (n % 10 == 1 && n % 100 != 11) "$n kārts" else "$n kārtis"

internal fun contractWord(contract: Contract?): String = when (contract) {
    Contract.NORMAL -> "Ņemu"
    Contract.ZOLE -> "Zole"
    Contract.SMALL_ZOLE -> "Mazā zole"
    Contract.TABLE -> "Galdiņš"
    null -> "Visi garām"
}

internal fun outcomeWord(outcome: Outcome): String = when (outcome) {
    Outcome.WIN -> "uzvara"
    Outcome.WIN_JANI -> "mazie jaņos"
    Outcome.WIN_ALL_TRICKS -> "visi stiķi"
    Outcome.LOSE -> "zaudējums"
    Outcome.LOSE_JANI -> "jaņos"
    Outcome.LOSE_NO_TRICKS -> "bez stiķa"
    Outcome.SMALL_ZOLE_WIN -> "uzvara"
    Outcome.SMALL_ZOLE_LOSE -> "zaudējums"
    Outcome.TABLE -> "katrs par sevi"
    Outcome.ALL_PASSED -> "pule"
}

internal fun signed(v: Int): String = when {
    v > 0 -> "+$v"
    v < 0 -> "−${-v}"
    else -> "0"
}
