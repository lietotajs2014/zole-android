package lv.zole.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
enum class BidChoice { PASS, TAKE, ZOLE, SMALL_ZOLE }

@Serializable
enum class Contract { NORMAL, ZOLE, SMALL_ZOLE, TABLE }

@Serializable
data class Play(val seat: Seat, val card: Card)

@Serializable
data class TakenTrick(val winner: Seat, val plays: List<Play>) {
    val points: Int get() = plays.sumOf { it.card.points }
}

@Serializable
sealed interface Phase {
    @Serializable @SerialName("bidding")
    data class Bidding(val turn: Seat) : Phase

    @Serializable @SerialName("discarding")
    data class Discarding(val declarer: Seat) : Phase

    @Serializable @SerialName("playing")
    data class Playing(val turn: Seat, val trick: List<Play>) : Phase

    @Serializable @SerialName("finished")
    data class Finished(val result: DealResult) : Phase
}

@Serializable
sealed interface Action {
    val seat: Seat

    @Serializable @SerialName("bid")
    data class Bid(override val seat: Seat, val choice: BidChoice) : Action

    @Serializable @SerialName("discard")
    data class Discard(override val seat: Seat, val cards: List<Card>) : Action

    @Serializable @SerialName("play")
    data class PlayCard(override val seat: Seat, val card: Card) : Action
}

class IllegalActionException(message: String) : IllegalArgumentException(message)

/** Full state of one deal. Only the engine sees all of it; players get a [PlayerView]. */
@Serializable
data class DealState(
    val rules: Rules,
    val dealer: Seat,
    val hands: Map<Seat, List<Card>>,
    val talon: List<Card>,
    val bids: Map<Seat, BidChoice> = emptyMap(),
    val contract: Contract? = null,
    val declarer: Seat? = null,
    /** Cards the declarer put away after taking the talon; their points count for the declarer. */
    val discarded: List<Card> = emptyList(),
    val tricks: List<TakenTrick> = emptyList(),
    val phase: Phase,
) {
    val firstHand: Seat get() = dealer.next
    val isFinished: Boolean get() = phase is Phase.Finished

    fun tricksOf(seat: Seat): List<TakenTrick> = tricks.filter { it.winner == seat }

    companion object {
        /** Deals a new hand: 8 cards each, 2 to the talon. Deterministic for a given [seed]. */
        fun deal(rules: Rules, dealer: Seat, seed: Long): DealState {
            val cards = Deck.all.shuffled(Random(seed))
            val hands = Seat.ALL.associateWith { seat -> cards.subList(seat.index * 8, seat.index * 8 + 8).sortedForHand() }
            return DealState(
                rules = rules,
                dealer = dealer,
                hands = hands,
                talon = cards.subList(24, 26),
                phase = Phase.Bidding(dealer.next),
            )
        }
    }
}

object DealEngine {

    /** Every action [seat] may take now. Bots and the UI both use this. */
    fun legalActions(state: DealState, seat: Seat): List<Action> = when (val phase = state.phase) {
        is Phase.Bidding -> if (phase.turn != seat) emptyList() else buildList {
            add(Action.Bid(seat, BidChoice.PASS))
            add(Action.Bid(seat, BidChoice.TAKE))
            add(Action.Bid(seat, BidChoice.ZOLE))
            if (state.rules.smallZoleEnabled) add(Action.Bid(seat, BidChoice.SMALL_ZOLE))
        }
        is Phase.Discarding -> emptyList() // any 2 of the 10 cards; too many to list, use [isLegal]
        is Phase.Playing -> if (phase.turn != seat) emptyList() else
            TrickRules.legalCards(state.hands.getValue(seat), phase.trick.map { it.card }).map { Action.PlayCard(seat, it) }
        is Phase.Finished -> emptyList()
    }

    fun legalCards(state: DealState, seat: Seat): List<Card> {
        val phase = state.phase as? Phase.Playing ?: return emptyList()
        if (phase.turn != seat) return emptyList()
        return TrickRules.legalCards(state.hands.getValue(seat), phase.trick.map { it.card })
    }

    fun isLegal(state: DealState, action: Action): Boolean = try {
        reduce(state, action); true
    } catch (e: IllegalActionException) {
        false
    }

    /** Pure state transition. Throws [IllegalActionException] for an illegal action. */
    fun reduce(state: DealState, action: Action): DealState = when (val phase = state.phase) {
        is Phase.Bidding -> bid(state, phase, action as? Action.Bid ?: illegal("Expected a bid"))
        is Phase.Discarding -> discard(state, phase, action as? Action.Discard ?: illegal("Expected a discard"))
        is Phase.Playing -> play(state, phase, action as? Action.PlayCard ?: illegal("Expected a card"))
        is Phase.Finished -> illegal("Deal is finished")
    }

    private fun bid(state: DealState, phase: Phase.Bidding, action: Action.Bid): DealState {
        if (action.seat != phase.turn) illegal("Not ${action.seat}'s turn to bid")
        if (action.choice == BidChoice.SMALL_ZOLE && !state.rules.smallZoleEnabled) illegal("Mazā zole is off")
        val bids = state.bids + (action.seat to action.choice)
        return when (action.choice) {
            BidChoice.PASS -> {
                if (bids.size < 3) {
                    state.copy(bids = bids, phase = Phase.Bidding(action.seat.next))
                } else when (state.rules.allPassMode) {
                    AllPassMode.TABLE -> state.copy(
                        bids = bids,
                        contract = Contract.TABLE,
                        phase = Phase.Playing(state.firstHand, emptyList()),
                    )
                    AllPassMode.POOL -> state.copy(
                        bids = bids,
                        phase = Phase.Finished(DealResult.allPassed()),
                    )
                }
            }
            BidChoice.TAKE -> state.copy(
                bids = bids,
                contract = Contract.NORMAL,
                declarer = action.seat,
                hands = state.hands + (action.seat to (state.hands.getValue(action.seat) + state.talon).sortedForHand()),
                phase = Phase.Discarding(action.seat),
            )
            BidChoice.ZOLE -> state.copy(
                bids = bids,
                contract = Contract.ZOLE,
                declarer = action.seat,
                phase = Phase.Playing(state.firstHand, emptyList()),
            )
            BidChoice.SMALL_ZOLE -> state.copy(
                bids = bids,
                contract = Contract.SMALL_ZOLE,
                declarer = action.seat,
                phase = Phase.Playing(state.firstHand, emptyList()),
            )
        }
    }

    private fun discard(state: DealState, phase: Phase.Discarding, action: Action.Discard): DealState {
        if (action.seat != phase.declarer) illegal("Only the declarer discards")
        val hand = state.hands.getValue(action.seat)
        if (action.cards.size != 2 || action.cards.toSet().size != 2) illegal("Discard exactly 2 cards")
        if (!hand.containsAll(action.cards)) illegal("Cards are not in hand")
        return state.copy(
            hands = state.hands + (action.seat to hand - action.cards.toSet()),
            discarded = action.cards,
            phase = Phase.Playing(state.firstHand, emptyList()),
        )
    }

    private fun play(state: DealState, phase: Phase.Playing, action: Action.PlayCard): DealState {
        if (action.seat != phase.turn) illegal("Not ${action.seat}'s turn to play")
        val hand = state.hands.getValue(action.seat)
        if (action.card !in TrickRules.legalCards(hand, phase.trick.map { it.card })) illegal("${action.card} is not playable")
        val hands = state.hands + (action.seat to hand - action.card)
        val trick = phase.trick + Play(action.seat, action.card)
        if (trick.size < 3) {
            return state.copy(hands = hands, phase = Phase.Playing(action.seat.next, trick))
        }
        val winner = trick[TrickRules.winnerIndex(trick.map { it.card })].seat
        val tricks = state.tricks + TakenTrick(winner, trick)
        val next = state.copy(hands = hands, tricks = tricks, phase = Phase.Playing(winner, emptyList()))
        val smallZoleBroken = state.contract == Contract.SMALL_ZOLE && winner == state.declarer
        return if (hands.values.all { it.isEmpty() } || smallZoleBroken) {
            next.copy(phase = Phase.Finished(DealResult.of(next)))
        } else next
    }

    private fun illegal(message: String): Nothing = throw IllegalActionException(message)
}
