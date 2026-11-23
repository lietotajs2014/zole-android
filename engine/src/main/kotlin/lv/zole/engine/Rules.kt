package lv.zole.engine

import kotlinx.serialization.Serializable

/** What happens when everyone passes. */
@Serializable
enum class AllPassMode { TABLE, POOL }

/** Which points table is used. Rondo changes only how galds is scored. */
@Serializable
enum class ScoringVariant { CLASSIC, RONDO }

/**
 * House rules. All scoring numbers live here so they can be changed without touching the engine.
 * Values are game points per opponent ("no katra pretinieka"), following the Latvian Wikipedia table.
 */
@Serializable
data class Rules(
    val allPassMode: AllPassMode = AllPassMode.TABLE,
    val smallZoleEnabled: Boolean = false,
    val normal: ContractScores = ContractScores(win = 1, winJani = 2, winAllTricks = 3, lose = -2, loseJani = -3, loseNoTricks = -4),
    val zole: ContractScores = ContractScores(win = 5, winJani = 6, winAllTricks = 7, lose = -6, loseJani = -7, loseNoTricks = -8),
    val smallZoleWin: Int = 6,
    val smallZoleLose: Int = -7,
    val scoring: ScoringVariant = ScoringVariant.CLASSIC,
    /** Galds (classic): the loser pays this many points to each other player. */
    val tableLoserPays: Int = 2,
    /** Galds (Rondo): what each winner takes from the loser, by the winner's own result. */
    val rondoNoTricks: Int = 3,
    val rondoJani: Int = 2,
    val rondoWin: Int = 1,
    /** Pules: points a winning declarer takes from each player for a common pule. */
    val commonPoolValue: Int = 1,
    /** Pules: points a winning declarer takes from the owner of a personal pule. */
    val personalPoolValue: Int = 2,
) {
    companion object {
        /** Declarer wins with at least this many eyes. */
        const val WIN_POINTS = 61
        /** Declarer with this many eyes or more puts the defenders "jaņos". */
        const val DECLARER_JANI_WIN = 91
        /** Declarer with this many eyes or fewer is "jaņos". */
        const val DECLARER_JANI_LOSE = 30
    }
}

@Serializable
data class ContractScores(
    val win: Int,
    val winJani: Int,
    val winAllTricks: Int,
    val lose: Int,
    val loseJani: Int,
    val loseNoTricks: Int,
)

object TrickRules {
    /** Index into [trick] of the winning card. The first card leads. */
    fun winnerIndex(trick: List<Card>): Int {
        require(trick.isNotEmpty())
        var best = 0
        for (i in 1 until trick.size) {
            if (beats(trick[i], trick[best], trick[0].effectiveSuit)) best = i
        }
        return best
    }

    /** True if [challenger] beats the currently winning [current] card, given the led effective suit. */
    fun beats(challenger: Card, current: Card, led: EffectiveSuit): Boolean {
        if (challenger.isTrump && !current.isTrump) return true
        if (!challenger.isTrump && current.isTrump) return false
        if (challenger.effectiveSuit != current.effectiveSuit) {
            // Neither is trump and suits differ: only a card of the led suit can be winning.
            return challenger.effectiveSuit == led && current.effectiveSuit != led
        }
        return challenger.strength > current.strength
    }

    /** Cards from [hand] that may be played onto [trick]. */
    fun legalCards(hand: List<Card>, trick: List<Card>): List<Card> {
        if (trick.isEmpty()) return hand
        val led = trick.first().effectiveSuit
        val follow = hand.filter { it.effectiveSuit == led }
        return follow.ifEmpty { hand }
    }
}
