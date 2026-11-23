package lv.zole.ai

import lv.zole.ai.Knowledge.high
import lv.zole.ai.Knowledge.isMaster
import lv.zole.ai.Knowledge.low
import lv.zole.ai.Knowledge.strongest
import lv.zole.ai.Knowledge.weakest
import lv.zole.ai.Knowledge.wouldWin
import lv.zole.engine.AllPassMode
import lv.zole.engine.BidChoice
import lv.zole.engine.Card
import lv.zole.engine.Contract
import lv.zole.engine.EffectiveSuit
import lv.zole.engine.PlayerView
import lv.zole.engine.Rank
import lv.zole.engine.Suit

/** Heuristic bot: hand evaluation for bidding, simple card-play rules from common Zole strategy. */
open class MediumBot : BotStrategy {

    /** Rough hand value used for bidding. */
    fun handScore(hand: List<Card>): Double {
        var score = 0.0
        val trumps = hand.filter { it.isTrump }
        for (t in trumps) {
            score += when {
                t.rank == Rank.QUEEN -> if (t.suit == Suit.CLUBS) 3.5 else 3.0
                t.rank == Rank.JACK -> 2.0
                t.rank == Rank.ACE || t.rank == Rank.TEN -> 1.5
                else -> 1.0
            }
        }
        for (suit in listOf(EffectiveSuit.CLUBS, EffectiveSuit.SPADES, EffectiveSuit.HEARTS)) {
            val cards = hand.filter { it.effectiveSuit == suit }
            if (cards.any { it.rank == Rank.ACE }) {
                score += 2.0
                if (cards.any { it.rank == Rank.TEN }) score += 1.0
            }
            if (cards.isEmpty() && trumps.size >= 4) score += 1.0
        }
        return score
    }

    override fun bid(view: PlayerView): BidChoice {
        val hand = view.hand
        val trumps = hand.count { it.isTrump }
        val score = handScore(hand)
        val lastAfterPasses = view.bids.size == 2 && view.bids.values.all { it == BidChoice.PASS }
        // A strong-ish hand is a liability in galds, so the last bidder takes a little more readily.
        val takeThreshold = if (lastAfterPasses && view.rules.allPassMode == AllPassMode.TABLE) 11.0 else 12.5
        return when {
            trumps >= 6 && score >= 19.0 -> BidChoice.ZOLE
            trumps >= 4 && score >= takeThreshold -> BidChoice.TAKE
            view.rules.smallZoleEnabled && isSmallZoleHand(hand) -> BidChoice.SMALL_ZOLE
            else -> BidChoice.PASS
        }
    }

    protected fun isSmallZoleHand(hand: List<Card>): Boolean {
        val trumps = hand.filter { it.isTrump }
        if (trumps.size > 2 || trumps.any { it.strength > 3 }) return false
        return hand.count { !it.isTrump && (it.rank == Rank.ACE || it.rank == Rank.TEN) } == 0
    }

    override fun discard(view: PlayerView): List<Card> {
        val hand = view.hand
        val nonTrumps = hand.filter { !it.isTrump }
        if (nonTrumps.size < 2) {
            val lowTrumps = hand.filter { it.isTrump }.sortedBy { it.strength }
            return (nonTrumps + lowTrumps).take(2)
        }
        fun priority(c: Card): Double {
            val suitCards = nonTrumps.filter { it.effectiveSuit == c.effectiveSuit }
            if (c.rank == Rank.ACE) return -100.0
            var p = c.points.toDouble()
            if (suitCards.size <= 2) p += 8.0 // helps make a void
            if (c.rank == Rank.TEN && suitCards.any { it.rank == Rank.ACE }) p -= 6.0
            return p
        }
        val ranked = nonTrumps.sortedByDescending { priority(it) }
        val chosen = ranked.take(2)
        if (chosen.any { it.rank == Rank.ACE }) {
            // Prefer a low trump over putting away an ace.
            val lowTrumps = hand.filter { it.isTrump }.sortedBy { it.strength }
            return (chosen.filter { it.rank != Rank.ACE } + lowTrumps).take(2)
        }
        return chosen
    }

    override fun play(view: PlayerView): Card {
        val legal = view.legalCards()
        if (legal.size == 1) return legal.first()
        return when (view.contract) {
            Contract.TABLE -> avoidTricks(view, legal)
            Contract.SMALL_ZOLE -> if (view.declarer == view.seat) avoidTricks(view, legal) else smallZoleDefence(view, legal)
            else -> if (view.currentTrick.isEmpty()) lead(view, legal) else follow(view, legal)
        }
    }

    protected fun lead(view: PlayerView, legal: List<Card>): Card {
        val unseen = Knowledge.unseen(view)
        val trumps = legal.filter { it.isTrump }
        val nonTrumps = legal.filter { !it.isTrump }
        val aces = nonTrumps.filter { it.rank == Rank.ACE }
        if (view.declarer == view.seat) {
            val unseenTrumps = unseen.count { it.isTrump }
            val top = trumps.maxByOrNull { it.strength }
            if (top != null && unseenTrumps > 0 && isMaster(top, unseen)) return top
            if (aces.isNotEmpty()) return aces.first()
            if (trumps.size > unseenTrumps && trumps.isNotEmpty()) return trumps.minBy { it.strength }
            return if (nonTrumps.isNotEmpty()) low(nonTrumps) else trumps.minBy { it.strength }
        }
        // Defender: cash aces, otherwise lead a low card in a side suit.
        val voids = Knowledge.voids(view)
        val declarer = view.declarer
        val safeAces = aces.filter { declarer == null || it.effectiveSuit !in voids.getValue(declarer) }
        if (safeAces.isNotEmpty()) return safeAces.first()
        val sideLow = nonTrumps.filter { it.rank == Rank.NINE || it.rank == Rank.KING }
        if (sideLow.isNotEmpty()) return low(sideLow)
        if (nonTrumps.isNotEmpty()) return low(nonTrumps)
        return trumps.minBy { it.strength }
    }

    protected fun follow(view: PlayerView, legal: List<Card>): Card {
        val trick = view.currentTrick
        val unseen = Knowledge.unseen(view)
        val winner = Knowledge.currentWinner(trick)
        val partnerWinning = winner.seat != view.seat && view.sameSide(winner.seat, view.seat)
        val winners = legal.filter { wouldWin(it, trick) }
        val isLast = trick.size == 2
        if (isLast) {
            if (partnerWinning) return high(legal)
            if (winners.isNotEmpty()) return winners.maxWith(compareBy<Card>({ it.points }, { -it.strength }))
            return low(legal)
        }
        val lastSeat = view.seat.next
        val partnerIsLast = view.sameSide(lastSeat, view.seat)
        if (partnerWinning) {
            // Partner led. Smear points if partner's card should hold.
            return if (isMaster(winner.card, unseen)) high(legal) else low(legal)
        }
        val masters = winners.filter { isMaster(it, unseen) }
        if (partnerIsLast) {
            return if (masters.isNotEmpty() && trick.sumOf { it.card.points } >= 10) weakest(masters) else low(legal)
        }
        if (masters.isNotEmpty()) return masters.minWith(compareBy<Card>({ it.strength }, { -it.points }))
        val trickPoints = trick.sumOf { it.card.points }
        if (winners.isNotEmpty() && trickPoints >= 10) return strongest(winners)
        return low(legal)
    }

    /** Galds and the mazā zole declarer: take as few tricks as possible. */
    protected fun avoidTricks(view: PlayerView, legal: List<Card>): Card {
        val trick = view.currentTrick
        if (trick.isEmpty()) return weakest(legal)
        val under = legal.filter { !wouldWin(it, trick) }
        if (under.isNotEmpty()) return strongest(under)
        return strongest(legal)
    }

    protected fun smallZoleDefence(view: PlayerView, legal: List<Card>): Card {
        val trick = view.currentTrick
        if (trick.isEmpty()) return weakest(legal)
        val declarer = view.declarer
        val winner = Knowledge.currentWinner(trick)
        if (winner.seat == declarer) {
            val under = legal.filter { !wouldWin(it, trick) }
            if (under.isNotEmpty()) return strongest(under)
        }
        return weakest(legal)
    }
}
