package lv.zole.feature.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import lv.zole.core.data.HistoryDao
import lv.zole.core.data.MatchRepository
import lv.zole.core.data.PlayerStats
import lv.zole.core.data.computeStats
import lv.zole.core.ui.BackHeader
import lv.zole.core.ui.Divider
import lv.zole.core.ui.PrimaryButton
import lv.zole.core.ui.Segmented
import lv.zole.core.ui.SurfaceCard
import lv.zole.core.ui.ZText
import lv.zole.core.ui.Zole
import lv.zole.engine.Contract
import lv.zole.engine.DealRecord
import lv.zole.engine.MatchState
import lv.zole.engine.Seat

val PLAYER_NAMES = listOf("Tu", "Anna", "Jānis")

fun signed(v: Int, plus: Boolean = false): String = when {
    v > 0 && plus -> "+$v"
    v < 0 -> "−${-v}"
    else -> v.toString()
}

class StatsViewModel(matches: MatchRepository, dao: HistoryDao) : ViewModel() {
    val match: StateFlow<MatchState?> = matches.state
    val stats: StateFlow<PlayerStats> = combine(dao.deals(), dao.matches()) { d, m -> computeStats(d, m) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerStats())
}

@Composable
fun ScoreboardRoute(viewModel: StatsViewModel, onBack: () -> Unit, onBackToTable: () -> Unit, initialTab: Int = 0) {
    val match by viewModel.match.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    ScoreboardScreen(match, stats, onBack, onBackToTable, initialTab)
}

private fun DealRecord.label(): String {
    val r = result
    val who = r.declarer?.let { PLAYER_NAMES[it.index] }
    return when (r.contract) {
        null -> "Pule"
        Contract.TABLE -> "Galdiņš"
        Contract.NORMAL -> "$who ņem"
        Contract.ZOLE -> "$who zole"
        Contract.SMALL_ZOLE -> "$who mazā zole"
    }
}

@Composable
fun ScoreboardScreen(match: MatchState?, stats: PlayerStats, onBack: () -> Unit, onBackToTable: () -> Unit, initialTab: Int = 0) {
    val c = Zole.colors
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    val canReturn = match != null && !match.isOver
    Column(
        Modifier.fillMaxSize().background(c.background).systemBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        BackHeader("Partija", onBack) {
            if (match != null) {
                val limit = match.config.dealLimit
                val n = match.dealNumber
                ZText(if (limit != null) "$n. dale no $limit" else "$n. dale", Modifier.padding(end = 0.dp), size = 14.sp, color = c.textSecondary)
            }
        }
        Segmented(listOf("Pieraksts", "Statistika"), tab, { tab = it }, height = 40.dp)
        Column(Modifier.weight(1f)) {
            if (tab == 0) ScoreSheet(match) else StatsList(stats)
        }
        if (canReturn) PrimaryButton("Atpakaļ pie galda", onBackToTable)
    }
}

@Composable
private fun ScoreSheet(match: MatchState?) {
    val c = Zole.colors
    if (match == null || match.history.isEmpty()) {
        ZText("Vēl nav nevienas nospēlētas dales.", Modifier.fillMaxWidth().padding(top = 24.dp), color = c.textSecondary, align = TextAlign.Center)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Seat.ALL.forEach { seat ->
                SurfaceCard(
                    Modifier.weight(1f),
                    borderColor = if (seat.index == 0) c.accent else c.surfaceBorder,
                    borderWidth = if (seat.index == 0) 1.5.dp else 1.dp,
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        ZText(PLAYER_NAMES[seat.index], size = 13.sp, color = c.textSecondary)
                        ZText(signed(match.totals.getValue(seat), plus = true), size = 26.sp, weight = FontWeight.Bold)
                    }
                }
            }
        }
        SurfaceCard {
            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 10.dp)) {
                Head("#", Modifier.weight(0.5f))
                Head("Spēle", Modifier.weight(2f))
                PLAYER_NAMES.forEach { Head(it, Modifier.weight(1f), TextAlign.End) }
            }
            val rows = match.history.reversed()
            LazyColumn {
                items(rows, key = { it.number }) { record ->
                    val latest = record.number == match.history.last().number
                    val pule = record.result.contract == null
                    val zole = record.result.contract == Contract.ZOLE || record.result.contract == Contract.SMALL_ZOLE
                    val color = if (pule) c.textSecondary else c.text
                    Divider()
                    Row(
                        Modifier.fillMaxWidth().background(if (latest) c.rowHighlight else Color.Transparent)
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                    ) {
                        ZText(record.number.toString(), Modifier.weight(0.5f), color = c.textSecondary)
                        ZText(record.label(), Modifier.weight(2f), color = color, weight = if (zole) FontWeight.SemiBold else FontWeight.Normal)
                        Seat.ALL.forEach { seat ->
                            ZText(
                                signed(record.totals.getValue(seat)), Modifier.weight(1f), color = color,
                                weight = if (latest) FontWeight.SemiBold else FontWeight.Normal, align = TextAlign.End,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.Head(text: String, modifier: Modifier, align: TextAlign = TextAlign.Start) {
    ZText(
        text.uppercase(), modifier, size = 12.sp, weight = FontWeight.SemiBold, color = Zole.colors.textSecondary,
        letterSpacing = 0.6.sp, align = align,
    )
}

@Composable
private fun StatsList(s: PlayerStats) {
    val c = Zole.colors
    fun pct(a: Int, b: Int) = if (b == 0) "–" else "${a * 100 / b}%"
    val rows = listOf(
        "Nospēlētas dales" to s.deals.toString(),
        "Lielais" to "${s.asDeclarer} reizes",
        "Uzvaras kā lielajam" to "${s.declarerWins} · ${pct(s.declarerWins, s.asDeclarer)}",
        "Zoles" to if (s.zoles == 0) "0" else "${s.zoleWins}/${s.zoles} · ${pct(s.zoleWins, s.zoles)}",
        "Vidējās acis kā lielajam" to if (s.asDeclarer == 0) "–" else s.averageDeclarerPoints.toString(),
        "Garākā uzvaru sērija" to s.longestWinStreak.toString(),
        "Pabeigtas partijas" to s.matches.toString(),
        "Uzvarētas partijas" to "${s.matchesWon} · ${pct(s.matchesWon, s.matches)}",
        "Punkti kopā" to signed(s.totalPoints, plus = true),
    )
    Column(Modifier.verticalScroll(rememberScrollState())) {
        SurfaceCard {
            rows.forEachIndexed { i, (label, value) ->
                if (i > 0) Divider()
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp)) {
                    ZText(label, Modifier.weight(1f))
                    ZText(value, weight = FontWeight.SemiBold)
                }
            }
        }
        ZText(
            "Statistika skaitās tikai par tevi un krājas no visām partijām.",
            Modifier.padding(top = 12.dp, start = 4.dp), size = 13.sp, color = c.textSecondary,
        )
    }
}
