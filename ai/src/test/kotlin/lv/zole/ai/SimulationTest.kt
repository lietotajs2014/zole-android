package lv.zole.ai

import lv.zole.engine.Action
import lv.zole.engine.AllPassMode
import lv.zole.engine.Contract
import lv.zole.engine.DealEngine
import lv.zole.engine.DealResult
import lv.zole.engine.DealState
import lv.zole.engine.Phase
import lv.zole.engine.PlayerView
import lv.zole.engine.Rules
import lv.zole.engine.Seat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SimulationTest {

    private fun playDeal(state0: DealState, bots: Map<Seat, BotStrategy>): DealResult {
        var s = state0
        while (true) {
            val p = s.phase
            val action = when (p) {
                is Phase.Finished -> return p.result
                is Phase.Bidding -> Action.Bid(p.turn, bots.getValue(p.turn).bid(PlayerView.of(s, p.turn)))
                is Phase.Discarding -> Action.Discard(p.declarer, bots.getValue(p.declarer).discard(PlayerView.of(s, p.declarer)))
                is Phase.Playing -> Action.PlayCard(p.turn, bots.getValue(p.turn).play(PlayerView.of(s, p.turn)))
            }
            s = DealEngine.reduce(s, action) // throws if a bot picks something illegal
        }
    }

    @Test fun `medium and easy bots play thousands of legal deals`() {
        val contracts = mutableMapOf<Contract?, Int>()
        repeat(3000) { i ->
            val rules = Rules(allPassMode = if (i % 2 == 0) AllPassMode.TABLE else AllPassMode.POOL, smallZoleEnabled = i % 4 == 0)
            val bots: Map<Seat, BotStrategy> = mapOf(Seat(0) to MediumBot(), Seat(1) to EasyBot(i.toLong()), Seat(2) to MediumBot())
            val result = playDeal(DealState.deal(rules, Seat(i % 3), i.toLong()), bots)
            assertEquals(0, result.scores.values.sum())
            contracts.merge(result.contract, 1, Int::plus)
        }
        println("Contracts over 3000 deals: $contracts")
        val taken = (contracts[Contract.NORMAL] ?: 0) + (contracts[Contract.ZOLE] ?: 0)
        assertTrue(taken in 900..2900, "bots should bid a sensible share of hands, got $taken")
    }

    @Test fun `hard bot plays legal deals and beats easy bots`() {
        var hard = 0
        var easy = 0
        val deals = 240
        repeat(deals) { i ->
            // Rotate the hard bot through every seat so position does not bias the result.
            val hardSeat = Seat(i % 3)
            val bots = Seat.ALL.associateWith { if (it == hardSeat) HardBot(i.toLong(), playoutBudget = 120) else EasyBot(i * 7L + it.index) }
            val result = playDeal(DealState.deal(Rules(), Seat((i / 3) % 3), 10_000L + i), bots)
            hard += result.scores.getValue(hardSeat)
            easy += Seat.ALL.filter { it != hardSeat }.sumOf { result.scores.getValue(it) }
        }
        println("Hard total $hard, easy total $easy over $deals deals")
        assertTrue(hard > 0, "hard bot should come out ahead of easy bots")
    }

    @Test fun `medium bot beats easy bots`() {
        var medium = 0
        repeat(2000) { i ->
            val seat = Seat(i % 3)
            val bots = Seat.ALL.associateWith { if (it == seat) MediumBot() else EasyBot(i * 3L + it.index) }
            medium += playDeal(DealState.deal(Rules(), Seat((i / 3) % 3), 50_000L + i), bots).scores.getValue(seat)
        }
        println("Medium total vs easy over 2000 deals: $medium")
        assertTrue(medium > 0)
    }
}
