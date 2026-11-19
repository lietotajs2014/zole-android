package lv.zole.engine

import kotlinx.serialization.Serializable

/**
 * What one seat is allowed to see. Bots and the UI work only with this, so they cannot see
 * other players' cards. The same view will later be sent over the network.
 */
@Serializable
data class PlayerView(
    val seat: Seat,
    val rules: Rules,
    val dealer: Seat,
    val phase: Phase,
    val hand: List<Card>,
    val bids: Map<Seat, BidChoice>,
    val contract: Contract?,
    val declarer: Seat?,
    /** The two talon cards while this seat (as declarer) is choosing what to put away. */
    val talonInHand: List<Card>,
    /** Cards this seat put away as declarer; empty otherwise. */
    val myDiscard: List<Card>,
    val tricks: List<TakenTrick>,
    val cardsLeft: Map<Seat, Int>,
) {
    val firstHand: Seat get() = dealer.next
    val currentTrick: List<Play> get() = (phase as? Phase.Playing)?.trick ?: emptyList()
    val turn: Seat? get() = when (val p = phase) {
        is Phase.Bidding -> p.turn
        is Phase.Playing -> p.turn
        is Phase.Discarding -> p.declarer
        is Phase.Finished -> null
    }
    val isMyTurn: Boolean get() = turn == seat

    /** Cards already played this deal, including the current trick. */
    val playedCards: List<Card> get() = tricks.flatMap { t -> t.plays.map { it.card } } + currentTrick.map { it.card }

    fun legalCards(): List<Card> {
        if (phase !is Phase.Playing || phase.turn != seat) return emptyList()
        return TrickRules.legalCards(hand, phase.trick.map { it.card })
    }

    /** True if [a] and [b] play on the same side this deal. */
    fun sameSide(a: Seat, b: Seat): Boolean {
        if (a == b) return true
        if (contract == Contract.TABLE || declarer == null) return false
        return a != declarer && b != declarer
    }

    /** Eyes taken so far by [seat]'s side (tricks only, plus own discard if known). */
    fun sidePoints(of: Seat): Int {
        val side = tricks.filter { sameSide(it.winner, of) }.sumOf { it.points }
        return if (of == seat || sameSide(of, seat)) side + myDiscard.sumOf { it.points } else side
    }

    companion object {
        fun of(state: DealState, seat: Seat): PlayerView {
            val discarding = state.phase is Phase.Discarding && state.declarer == seat
            return PlayerView(
                seat = seat,
                rules = state.rules,
                dealer = state.dealer,
                phase = state.phase,
                hand = state.hands.getValue(seat),
                bids = state.bids,
                contract = state.contract,
                declarer = state.declarer,
                talonInHand = if (discarding) state.talon else emptyList(),
                myDiscard = if (state.declarer == seat) state.discarded else emptyList(),
                tricks = state.tricks,
                cardsLeft = Seat.ALL.associateWith { state.hands.getValue(it).size },
            )
        }
    }
}
