package lv.zole.core.data

data class PlayerStats(
    val deals: Int = 0,
    val asDeclarer: Int = 0,
    val declarerWins: Int = 0,
    val zoles: Int = 0,
    val zoleWins: Int = 0,
    val averageDeclarerPoints: Int = 0,
    val matches: Int = 0,
    val matchesWon: Int = 0,
    val longestWinStreak: Int = 0,
    val totalPoints: Int = 0,
)

private val winOutcomes = setOf("WIN", "WIN_JANI", "WIN_ALL_TRICKS", "SMALL_ZOLE_WIN")

/** Statistics for the human player (seat 0). */
fun computeStats(deals: List<DealEntity>, matches: List<MatchEntity>): PlayerStats {
    val mine = deals.filter { it.declarer == 0 }
    val wins = mine.count { it.outcome in winOutcomes }
    val zoles = mine.filter { it.contract == "ZOLE" }
    var streak = 0
    var best = 0
    for (d in mine) {
        if (d.outcome in winOutcomes) { streak++; best = maxOf(best, streak) } else streak = 0
    }
    val finished = matches.filter { it.endedAt != null }
    val normalPoints = mine.filter { it.contract == "NORMAL" || it.contract == "ZOLE" }.map { it.declarerPoints }
    return PlayerStats(
        deals = deals.size,
        asDeclarer = mine.size,
        declarerWins = wins,
        zoles = zoles.size,
        zoleWins = zoles.count { it.outcome in winOutcomes },
        averageDeclarerPoints = if (normalPoints.isEmpty()) 0 else normalPoints.average().toInt(),
        matches = finished.size,
        matchesWon = finished.count { it.total0 > it.total1 && it.total0 > it.total2 },
        longestWinStreak = best,
        totalPoints = deals.sumOf { it.delta0 },
    )
}
