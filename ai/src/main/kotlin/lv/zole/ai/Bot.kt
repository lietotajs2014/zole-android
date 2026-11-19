package lv.zole.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lv.zole.engine.BidChoice
import lv.zole.engine.Card
import lv.zole.engine.Difficulty
import lv.zole.engine.Player
import lv.zole.engine.PlayerView

/** A synchronous bot brain. It only sees a [PlayerView], so it cannot cheat. */
interface BotStrategy {
    fun bid(view: PlayerView): BidChoice
    fun discard(view: PlayerView): List<Card>
    fun play(view: PlayerView): Card
}

object Bots {
    fun strategy(difficulty: Difficulty, seed: Long): BotStrategy = when (difficulty) {
        Difficulty.EASY -> EasyBot(seed)
        Difficulty.MEDIUM -> MediumBot()
        Difficulty.HARD -> HardBot(seed)
    }
}

/** Runs a [BotStrategy] off the main thread. */
class BotPlayer(private val strategy: BotStrategy) : Player {
    override suspend fun bid(view: PlayerView): BidChoice = withContext(Dispatchers.Default) { strategy.bid(view) }
    override suspend fun discard(view: PlayerView): List<Card> = withContext(Dispatchers.Default) { strategy.discard(view) }
    override suspend fun play(view: PlayerView): Card = withContext(Dispatchers.Default) { strategy.play(view) }
}
