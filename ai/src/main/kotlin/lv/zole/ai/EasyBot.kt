package lv.zole.ai

import lv.zole.ai.Knowledge.high
import lv.zole.ai.Knowledge.low
import lv.zole.ai.Knowledge.wouldWin
import lv.zole.engine.BidChoice
import lv.zole.engine.Card
import lv.zole.engine.Contract
import lv.zole.engine.PlayerView
import lv.zole.engine.Rank
import kotlin.random.Random

/** Simple bot: counts trumps, plays the cheapest legal card, gives points to a winning partner. */
class EasyBot(seed: Long) : BotStrategy {
    private val random = Random(seed)

    override fun bid(view: PlayerView): BidChoice {
        val trumps = view.hand.filter { it.isTrump }
        val high = trumps.count { it.rank == Rank.QUEEN || it.rank == Rank.JACK }
        return when {
            trumps.size >= 7 && high >= 4 -> BidChoice.ZOLE
            trumps.size >= 5 && high >= 2 -> BidChoice.TAKE
            trumps.size == 4 && high >= 3 && random.nextInt(3) == 0 -> BidChoice.TAKE
            else -> BidChoice.PASS
        }
    }

    override fun discard(view: PlayerView): List<Card> {
        val nonTrumps = view.hand.filter { !it.isTrump && it.rank != Rank.ACE }.sortedByDescending { it.points }
        val rest = view.hand.filter { it !in nonTrumps }.sortedBy { if (it.isTrump) it.strength + 10 else 0 }
        return (nonTrumps + rest).take(2)
    }

    override fun play(view: PlayerView): Card {
        val legal = view.legalCards()
        if (legal.size == 1) return legal.first()
        val trick = view.currentTrick
        if (view.contract == Contract.TABLE || (view.contract == Contract.SMALL_ZOLE && view.declarer == view.seat)) {
            val under = legal.filter { !wouldWin(it, trick) }
            return if (under.isNotEmpty()) under.maxBy { it.strength } else legal.minBy { it.strength }
        }
        if (trick.isEmpty()) return if (random.nextInt(4) == 0) legal.random(random) else low(legal)
        val winner = Knowledge.currentWinner(trick)
        val partnerWinning = winner.seat != view.seat && view.sameSide(winner.seat, view.seat)
        if (trick.size == 2) {
            if (partnerWinning) return high(legal)
            val winners = legal.filter { wouldWin(it, trick) }
            if (winners.isNotEmpty() && trick.sumOf { it.card.points } >= 10) return winners.minBy { it.strength }
        }
        return low(legal)
    }
}
