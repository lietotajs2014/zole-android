package lv.zole.ai

import lv.zole.engine.Card
import lv.zole.engine.Deck
import lv.zole.engine.EffectiveSuit
import lv.zole.engine.Play
import lv.zole.engine.PlayerView
import lv.zole.engine.Seat
import lv.zole.engine.TrickRules

/** Helpers that derive what a seat can know from its [PlayerView]. */
internal object Knowledge {

    /** Cards this seat has not seen: other hands plus the hidden talon or discard. */
    fun unseen(view: PlayerView): List<Card> {
        val seen = view.hand.toSet() + view.playedCards + view.myDiscard + view.talonInHand
        return Deck.all.filter { it !in seen }
    }

    /** Effective suits each seat has shown it does not have (failed to follow). */
    fun voids(view: PlayerView): Map<Seat, Set<EffectiveSuit>> {
        val result = Seat.ALL.associateWith { mutableSetOf<EffectiveSuit>() }
        val tricks = view.tricks.map { it.plays } + listOf(view.currentTrick).filter { it.isNotEmpty() }
        for (plays in tricks) {
            val led = plays.first().card.effectiveSuit
            for (p in plays.drop(1)) if (p.card.effectiveSuit != led) result.getValue(p.seat) += led
        }
        return result
    }

    fun currentWinner(trick: List<Play>): Play = trick[TrickRules.winnerIndex(trick.map { it.card })]

    fun wouldWin(card: Card, trick: List<Play>): Boolean {
        if (trick.isEmpty()) return true
        val led = trick.first().card.effectiveSuit
        return TrickRules.beats(card, currentWinner(trick).card, led)
    }

    /** No unseen card of the same effective suit is stronger. */
    fun isMaster(card: Card, unseen: List<Card>): Boolean =
        unseen.none { it.effectiveSuit == card.effectiveSuit && it.strength > card.strength }

    fun low(cards: List<Card>): Card = cards.minWith(compareBy<Card>({ it.points }, { if (it.isTrump) 1 else 0 }, { it.strength }))
    fun high(cards: List<Card>): Card = cards.maxWith(compareBy<Card>({ it.points }, { if (it.isTrump) 0 else 1 }, { -it.strength }))
    fun strongest(cards: List<Card>): Card = cards.maxWith(compareBy<Card>({ if (it.isTrump) 1 else 0 }, { it.strength }))
    fun weakest(cards: List<Card>): Card = cards.minWith(compareBy<Card>({ if (it.isTrump) 1 else 0 }, { it.strength }, { it.points }))
}
