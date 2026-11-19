package lv.zole.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.random.Random

private fun c(s: String): Card {
    val suit = when (s.last()) { '♣' -> Suit.CLUBS; '♠' -> Suit.SPADES; '♥' -> Suit.HEARTS; '♦' -> Suit.DIAMONDS; else -> error(s) }
    val rank = Rank.entries.first { it.label == s.dropLast(1) }
    return Card(suit, rank)
}

class DeckTest {
    @Test fun `deck has 26 cards, 120 points and 14 trumps`() {
        assertEquals(26, Deck.all.size)
        assertEquals(26, Deck.all.toSet().size)
        assertEquals(120, Deck.all.sumOf { it.points })
        assertEquals(14, Deck.all.count { it.isTrump })
        assertEquals(Card.TRUMP_ORDER.toSet(), Deck.all.filter { it.isTrump }.toSet())
    }

    @Test fun `jack of spades is a trump, not a spade`() {
        assertEquals(EffectiveSuit.TRUMP, c("J♠").effectiveSuit)
        assertEquals(EffectiveSuit.SPADES, c("A♠").effectiveSuit)
    }

    @Test fun `hand sorts trumps strongest first then suits`() {
        val hand = listOf(c("K♥"), c("A♠"), c("9♦"), c("Q♥"), c("A♦"), c("Q♣"), c("10♦"), c("J♠"))
        assertEquals(listOf("Q♣", "Q♥", "J♠", "A♦", "10♦", "9♦", "A♠", "K♥"), hand.sortedForHand().map { it.toString() })
    }
}

class TrickTest {
    private fun winner(vararg cards: String) = cards[TrickRules.winnerIndex(cards.map { c(it) })]

    @Test fun `highest of led suit wins`() = assertEquals("A♠", winner("K♠", "A♠", "10♠"))
    @Test fun `off suit card does not win`() = assertEquals("9♠", winner("9♠", "A♥", "A♣"))
    @Test fun `any trump beats non trump`() = assertEquals("7♦", winner("A♠", "7♦", "10♠"))
    @Test fun `queen of clubs is highest trump`() = assertEquals("Q♣", winner("J♣", "Q♣", "Q♠"))
    @Test fun `jack beats diamond ace`() = assertEquals("J♦", winner("A♦", "J♦", "10♦"))
    @Test fun `jack of spades on spade lead is a trump and wins`() = assertEquals("J♠", winner("A♠", "J♠", "K♠"))

    @Test fun `must follow effective suit`() {
        val hand = listOf(c("J♠"), c("9♠"), c("A♥"))
        assertEquals(listOf(c("9♠")), TrickRules.legalCards(hand, listOf(c("A♠"))))
        assertEquals(listOf(c("J♠")), TrickRules.legalCards(hand, listOf(c("7♦"))))
        assertEquals(hand, TrickRules.legalCards(hand, listOf(c("A♣"))))
    }
}

class ScoringTest {
    private val rules = Rules()

    /** Builds a finished NORMAL deal where the declarer (seat 0) took [declarerTricks] tricks worth exactly [points]. */
    private fun normalResult(points: Int, tricks: Int, contract: Contract = Contract.NORMAL): DealResult {
        // Construct with synthetic tricks: declarer gets one trick with all its points, the rest are empty.
        val cards = Deck.all
        val pointCards = cards.filter { it.points > 0 }.sortedByDescending { it.points }
        fun search(i: Int, left: Int): List<Card>? {
            if (left == 0) return emptyList()
            if (i >= pointCards.size || left < 0) return null
            search(i + 1, left - pointCards[i].points)?.let { return listOf(pointCards[i]) + it }
            return search(i + 1, left)
        }
        val mine = requireNotNull(search(0, points)) { "cannot build $points" }
        val zero = cards.filter { it.points == 0 }
        val plays = (mine + zero.take((tricks * 3 - mine.size).coerceAtLeast(0))).map { Play(Seat(0), it) }
        val declTricks = plays.chunked(3).take(tricks).map { TakenTrick(Seat(0), it) }
        val rest = cards - plays.flatMap { listOf(it.card) }.toSet()
        val otherTricks = rest.chunked(3).take(8 - tricks).map { ch -> TakenTrick(Seat(1), ch.map { Play(Seat(1), it) }) }
        val state = DealState(
            rules = rules, dealer = Seat(2), hands = Seat.ALL.associateWith { emptyList() }, talon = emptyList(),
            contract = contract, declarer = Seat(0), tricks = declTricks + otherTricks, phase = Phase.Playing(Seat(0), emptyList()),
        )
        return DealResult.of(state)
    }

    @Test fun `normal win 61 gives plus one per opponent`() {
        val r = normalResult(61, 4)
        assertEquals(Outcome.WIN, r.outcome)
        assertEquals(mapOf(Seat(0) to 2, Seat(1) to -1, Seat(2) to -1), r.scores)
    }

    @Test fun `normal win 91 puts defenders in jani`() = assertEquals(Outcome.WIN_JANI, normalResult(91, 5).outcome)
    @Test fun `declarer with 90 is a plain win`() = assertEquals(Outcome.WIN, normalResult(90, 5).outcome)
    @Test fun `declarer with 60 loses`() {
        val r = normalResult(60, 4)
        assertEquals(Outcome.LOSE, r.outcome)
        assertEquals(mapOf(Seat(0) to -4, Seat(1) to 2, Seat(2) to 2), r.scores)
    }
    @Test fun `declarer with 30 is in jani`() = assertEquals(Outcome.LOSE_JANI, normalResult(30, 2).outcome)
    @Test fun `declarer with 31 is not in jani`() = assertEquals(Outcome.LOSE, normalResult(31, 2).outcome)
    @Test fun `zole win scores five per opponent`() {
        val r = normalResult(70, 4, Contract.ZOLE)
        assertEquals(mapOf(Seat(0) to 10, Seat(1) to -5, Seat(2) to -5), r.scores)
    }
}

class DealFlowTest {
    private fun randomPlayout(rules: Rules, seed: Long, rnd: Random): DealState {
        var s = DealState.deal(rules, Seat(rnd.nextInt(3)), seed)
        while (!s.isFinished) {
            s = when (val p = s.phase) {
                is Phase.Bidding -> {
                    val opts = DealEngine.legalActions(s, p.turn)
                    // Pass more often so all branches get exercised.
                    DealEngine.reduce(s, if (rnd.nextInt(4) == 0) opts.random(rnd) else opts.first())
                }
                is Phase.Discarding -> DealEngine.reduce(s, Action.Discard(p.declarer, s.hands.getValue(p.declarer).shuffled(rnd).take(2)))
                is Phase.Playing -> DealEngine.reduce(s, DealEngine.legalActions(s, p.turn).random(rnd))
                is Phase.Finished -> s
            }
            assertCardsConserved(s)
        }
        return s
    }

    private fun assertCardsConserved(s: DealState) {
        val all = s.hands.values.flatten() + s.discarded + s.tricks.flatMap { t -> t.plays.map { it.card } } +
            ((s.phase as? Phase.Playing)?.trick?.map { it.card } ?: emptyList()) +
            (if (s.contract == Contract.NORMAL) emptyList() else s.talon)
        val unique = all.toSet()
        assertEquals(all.size, unique.size, "duplicate card")
        if (s.contract != Contract.SMALL_ZOLE || !s.isFinished) assertEquals(26, all.size, "every card in exactly one place")
    }

    @Test fun `random deals keep invariants`() {
        val rnd = Random(42)
        repeat(3000) { i ->
            val rules = Rules(allPassMode = if (i % 2 == 0) AllPassMode.TABLE else AllPassMode.POOL, smallZoleEnabled = i % 3 == 0)
            val end = randomPlayout(rules, i.toLong(), rnd)
            val result = (end.phase as Phase.Finished).result
            assertEquals(0, result.scores.values.sum(), "scores sum to zero")
            when (result.contract) {
                Contract.NORMAL, Contract.ZOLE -> assertEquals(120, result.declarerPoints + result.defenderPoints)
                Contract.TABLE -> assertEquals(120, result.pointsBySeat.values.sum())
                else -> {}
            }
        }
    }

    @Test fun `illegal moves are rejected`() {
        val s = DealState.deal(Rules(), Seat(2), 7)
        assertThrows<IllegalActionException> { DealEngine.reduce(s, Action.Bid(Seat(1), BidChoice.PASS)) }
        assertThrows<IllegalActionException> { DealEngine.reduce(s, Action.Bid(Seat(0), BidChoice.SMALL_ZOLE)) }
        val taken = DealEngine.reduce(s, Action.Bid(Seat(0), BidChoice.TAKE))
        assertEquals(10, taken.hands.getValue(Seat(0)).size)
        assertThrows<IllegalActionException> { DealEngine.reduce(taken, Action.Discard(Seat(0), taken.hands.getValue(Seat(0)).take(1))) }
        val discarded = DealEngine.reduce(taken, Action.Discard(Seat(0), taken.hands.getValue(Seat(0)).take(2)))
        assertTrue(discarded.phase is Phase.Playing)
        assertEquals(Seat(0), (discarded.phase as Phase.Playing).turn)
    }

    @Test fun `all pass in table mode starts galdins`() {
        var s = DealState.deal(Rules(allPassMode = AllPassMode.TABLE), Seat(2), 3)
        for (seat in listOf(Seat(0), Seat(1), Seat(2))) s = DealEngine.reduce(s, Action.Bid(seat, BidChoice.PASS))
        assertEquals(Contract.TABLE, s.contract)
        assertTrue(s.phase is Phase.Playing)
    }

    @Test fun `player view hides other hands`() {
        val s = DealState.deal(Rules(), Seat(2), 11)
        val view = PlayerView.of(s, Seat(1))
        assertEquals(s.hands.getValue(Seat(1)), view.hand)
        assertTrue(view.talonInHand.isEmpty())
        assertFalse(view.isMyTurn)
    }
}

class MatchTest {
    @Test fun `replay from snapshot gives same state`() {
        val config = MatchConfig(rules = Rules(allPassMode = AllPassMode.POOL), dealLimit = 6)
        val rnd = Random(5)
        var m = MatchEngine.start(config, 99)
        val actions = mutableListOf<MatchAction>()
        while (!m.isOver) {
            val action: MatchAction = if (m.deal.isFinished) MatchAction.NextDeal else {
                val d = m.deal
                when (val p = d.phase) {
                    is Phase.Bidding -> MatchAction.InDeal(DealEngine.legalActions(d, p.turn).random(rnd))
                    is Phase.Discarding -> MatchAction.InDeal(Action.Discard(p.declarer, d.hands.getValue(p.declarer).take(2)))
                    is Phase.Playing -> MatchAction.InDeal(DealEngine.legalActions(d, p.turn).random(rnd))
                    is Phase.Finished -> error("unreachable")
                }
            }
            actions += action
            m = MatchEngine.reduce(m, action)
        }
        assertEquals(6, m.history.size)
        assertEquals(0, m.totals.values.sum())
        val replayed = MatchEngine.replay(MatchSnapshot(config, 99, Seat(2), actions))
        assertEquals(m, replayed)
    }

    @Test fun `pules ledger`() {
        val rules = Rules(allPassMode = AllPassMode.POOL)
        val passed = DealResult.allPassed()
        val (one, _) = PoolLedger().settle(passed, rules)
        assertEquals(1, one.common)
        val win = DealResult(Contract.NORMAL, Seat(1), Outcome.WIN, 70, 50, emptyMap(), emptyMap(), emptyMap())
        val (afterWin, delta) = one.settle(win, rules)
        assertEquals(0, afterWin.common)
        assertEquals(mapOf(Seat(0) to -1, Seat(1) to 2, Seat(2) to -1), delta)
        val lose = win.copy(outcome = Outcome.LOSE)
        val (afterLose, _) = one.settle(lose, rules)
        assertEquals(listOf(Seat(1)), afterLose.personal)
        val winOther = win.copy(declarer = Seat(2))
        val (_, d2) = afterLose.copy(common = 0).settle(winOther, rules)
        assertEquals(mapOf(Seat(0) to 0, Seat(1) to -2, Seat(2) to 2), d2)
    }
}

class SerializationTest {
    @Test fun `snapshot survives json round trip`() {
        val snap = MatchSnapshot(
            MatchConfig(Rules(allPassMode = AllPassMode.POOL, smallZoleEnabled = true), Difficulty.HARD, null), 123L, Seat(1),
            listOf(
                MatchAction.InDeal(Action.Bid(Seat(2), BidChoice.TAKE)),
                MatchAction.InDeal(Action.Discard(Seat(2), listOf(c("A♠"), c("7♦")))),
                MatchAction.InDeal(Action.PlayCard(Seat(0), c("Q♣"))),
                MatchAction.NextDeal,
            ),
        )
        val json = kotlinx.serialization.json.Json
        val text = json.encodeToString(MatchSnapshot.serializer(), snap)
        assertEquals(snap, json.decodeFromString(MatchSnapshot.serializer(), text))
        val state = MatchEngine.start(MatchConfig(), 1)
        assertEquals(state, json.decodeFromString(MatchState.serializer(), json.encodeToString(MatchState.serializer(), state)))
    }
}
