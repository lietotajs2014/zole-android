package lv.zole.feature.game

import lv.zole.core.ui.Strings
import lv.zole.engine.Contract
import lv.zole.engine.Seat

internal fun Seat.displayName(t: Strings) = t.playerNames[index]
/** Avatar letter; "Arvis 1" becomes "A1" so the two bots differ. */
internal fun Seat.initial(t: Strings): String = t.playerNames[index].let { n -> n.first() + n.takeLastWhile { it.isDigit() } }

/** "lielais/lielā" or "mazais/mazā" for the opponents' status line. */
internal fun roleOf(t: Strings, seat: Seat, declarer: Seat?, contract: Contract?): String = when {
    contract == Contract.TABLE -> t.tableRole
    declarer == null -> ""
    else -> t.role(isDeclarer = seat == declarer, feminine = seat.index in t.feminineSeats)
}

internal fun signed(v: Int): String = when {
    v > 0 -> "+$v"
    v < 0 -> "−${-v}"
    else -> "0"
}
