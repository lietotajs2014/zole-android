package lv.zole.engine

/**
 * Anyone who sits at the table: the human (UI), a bot, and later a remote player.
 * The table only ever hands out a [PlayerView], never the full state.
 */
interface Player {
    suspend fun bid(view: PlayerView): BidChoice
    suspend fun discard(view: PlayerView): List<Card>
    suspend fun play(view: PlayerView): Card
}
