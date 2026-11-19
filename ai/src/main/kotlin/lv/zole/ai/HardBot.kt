package lv.zole.ai

import lv.zole.engine.Action
import lv.zole.engine.BidChoice
import lv.zole.engine.Card
import lv.zole.engine.Contract
import lv.zole.engine.DealEngine
import lv.zole.engine.DealResult
import lv.zole.engine.DealState
import lv.zole.engine.Phase
import lv.zole.engine.PlayerView
import lv.zole.engine.Seat
import kotlin.random.Random

/**
 * Determinised Monte Carlo bot. It deals the unseen cards in ways that match what it has seen
 * (including suits other players have shown they lack), plays each option out with [MediumBot]
 * for everyone, and picks the option with the best average result.
 */
class HardBot(
    seed: Long,
    private val playoutBudget: Int = 240,
) : BotStrategy {
    private val random = Random(seed)
    private val policy = MediumBot()

    override fun bid(view: PlayerView): BidChoice {
        val baseline = policy.handScore(view.hand)
        if (baseline < 8.0) {
            return if (view.rules.smallZoleEnabled && expected(view, BidChoice.SMALL_ZOLE, 20) > 0.5) BidChoice.SMALL_ZOLE else BidChoice.PASS
        }
        val samples = 24
        val take = expected(view, BidChoice.TAKE, samples)
        val zole = if (baseline >= 14.0) expected(view, BidChoice.ZOLE, samples) else Double.NEGATIVE_INFINITY
        return when {
            zole > 1.2 && zole > take -> BidChoice.ZOLE
            take > 0.15 -> BidChoice.TAKE
            else -> BidChoice.PASS
        }
    }

    override fun discard(view: PlayerView): List<Card> {
        val hand = view.hand
        val pairs = buildList {
            for (i in hand.indices) for (j in i + 1 until hand.size) add(listOf(hand[i], hand[j]))
        }
        val samples = (playoutBudget * 2 / pairs.size).coerceAtLeast(4)
        val worlds = List(samples) { Determinizer.sample(view, random) }
        return pairs.maxBy { pair ->
            worlds.sumOf { world -> utility(playout(DealEngine.reduce(world, Action.Discard(view.seat, pair))), view.seat) }
        }
    }

    override fun play(view: PlayerView): Card {
        val legal = view.legalCards()
        if (legal.size == 1) return legal.first()
        val samples = (playoutBudget / legal.size).coerceIn(10, 60)
        val totals = DoubleArray(legal.size)
        repeat(samples) {
            val world = Determinizer.sample(view, random)
            legal.forEachIndexed { i, card ->
                totals[i] += utility(playout(DealEngine.reduce(world, Action.PlayCard(view.seat, card))), view.seat)
            }
        }
        return legal[totals.indices.maxBy { totals[it] }]
    }

    private fun expected(view: PlayerView, choice: BidChoice, samples: Int): Double {
        var total = 0.0
        repeat(samples) {
            val world = Determinizer.sample(view, random)
            total += utility(playout(DealEngine.reduce(world, Action.Bid(view.seat, choice))), view.seat)
        }
        return total / samples
    }

    private fun playout(start: DealState): DealResult {
        var s = start
        while (true) {
            when (val p = s.phase) {
                is Phase.Finished -> return p.result
                is Phase.Bidding -> s = DealEngine.reduce(s, Action.Bid(p.turn, policy.bid(PlayerView.of(s, p.turn))))
                is Phase.Discarding -> s = DealEngine.reduce(s, Action.Discard(p.declarer, policy.discard(PlayerView.of(s, p.declarer))))
                is Phase.Playing -> s = DealEngine.reduce(s, Action.PlayCard(p.turn, policy.play(PlayerView.of(s, p.turn))))
            }
        }
    }

    private fun utility(result: DealResult, me: Seat): Double {
        val score = result.scores.getValue(me).toDouble()
        val eyes = when (result.contract) {
            Contract.TABLE -> -result.pointsBySeat.getValue(me)
            null -> 0
            else -> if (result.declarer == me) result.declarerPoints else result.defenderPoints
        }
        return score + eyes * 0.01
    }
}

/** Builds a full [DealState] that matches a seat's [PlayerView]. */
internal object Determinizer {
    fun sample(view: PlayerView, random: Random): DealState {
        val unseen = Knowledge.unseen(view)
        val others = Seat.ALL.filter { it != view.seat }
        val voids = Knowledge.voids(view)
        val counts = others.associateWith { view.cardsLeft.getValue(it) }
        val extraCount = unseen.size - counts.values.sum()

        var hands: Map<Seat, List<Card>>? = null
        var extra: List<Card> = emptyList()
        repeat(40) {
            if (hands != null) return@repeat
            val pool = unseen.shuffled(random).toMutableList()
            val assigned = mutableMapOf<Seat, List<Card>>()
            // Most constrained seat first.
            for (seat in others.sortedByDescending { voids.getValue(it).size }) {
                val allowed = pool.filter { it.effectiveSuit !in voids.getValue(seat) }
                val need = counts.getValue(seat)
                if (allowed.size < need) return@repeat
                val picked = allowed.take(need)
                assigned[seat] = picked
                pool.removeAll(picked.toSet())
            }
            hands = assigned
            extra = pool
        }
        if (hands == null) {
            val pool = unseen.shuffled(random)
            var i = 0
            hands = others.associateWith { seat -> pool.subList(i, i + counts.getValue(seat)).also { i += it.size } }
            extra = pool.subList(i, pool.size)
        }
        check(extra.size == extraCount)

        val allHands = hands!! + (view.seat to view.hand)
        val declarerTookTalon = view.contract == Contract.NORMAL
        val discarded = when {
            !declarerTookTalon -> emptyList()
            view.declarer == view.seat -> view.myDiscard
            else -> extra
        }
        val talon = when {
            view.talonInHand.isNotEmpty() -> view.talonInHand
            declarerTookTalon && view.declarer == view.seat -> view.myDiscard
            else -> extra
        }
        return DealState(
            rules = view.rules,
            dealer = view.dealer,
            hands = allHands,
            talon = talon,
            bids = view.bids,
            contract = view.contract,
            declarer = view.declarer,
            discarded = discarded,
            tricks = view.tricks,
            phase = view.phase,
        )
    }
}
