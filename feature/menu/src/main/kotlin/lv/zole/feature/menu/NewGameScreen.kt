package lv.zole.feature.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lv.zole.core.ui.BackHeader
import lv.zole.core.ui.PrimaryButton
import lv.zole.core.ui.SectionLabel
import lv.zole.core.ui.Segmented
import lv.zole.core.ui.ZText
import lv.zole.core.ui.Zole
import lv.zole.engine.AllPassMode
import lv.zole.engine.Difficulty
import lv.zole.engine.ScoringVariant

data class NewGameChoice(
    val difficulty: Difficulty,
    val allPassMode: AllPassMode,
    /** 0 = no limit */
    val dealLimit: Int,
    val scoring: ScoringVariant,
)

@Composable
fun NewGameScreen(
    initial: NewGameChoice,
    onBack: () -> Unit,
    onStart: (NewGameChoice) -> Unit,
) {
    val c = Zole.colors
    val t = Zole.strings
    var difficulty by rememberSaveable { mutableStateOf(initial.difficulty) }
    var allPass by rememberSaveable { mutableStateOf(initial.allPassMode) }
    var limit by rememberSaveable { mutableStateOf(initial.dealLimit) }
    var scoring by rememberSaveable { mutableStateOf(initial.scoring) }

    Column(
        Modifier.fillMaxSize().background(c.background).systemBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
    ) {
        BackHeader(t.newMatch, onBack, Modifier.padding(bottom = 24.dp))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(t.opponents)
                Segmented(t.difficulties, difficulty.ordinal, { difficulty = Difficulty.entries[it] })
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(t.ifAllPass)
                RadioCard(t.pools, t.poolsHint, allPass == AllPassMode.POOL) { allPass = AllPassMode.POOL }
                RadioCard(t.table, t.tableHint(scoring), allPass == AllPassMode.TABLE) { allPass = AllPassMode.TABLE }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(t.matchLength)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(t.deals(12), limit == 12) { limit = 12 }
                    Pill(t.deals(24), limit == 24) { limit = 24 }
                    Pill(t.noLimit, limit == 0) { limit = 0 }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(t.scoringTable)
                Segmented(ScoringVariant.entries.map { t.scoringName(it) }, scoring.ordinal, { scoring = ScoringVariant.entries[it] })
                ZText(t.scoringHint(scoring), Modifier.padding(horizontal = 4.dp), size = 13.sp, color = c.textSecondary, lineHeight = 18.sp)
            }
        }
        PrimaryButton(t.start, { onStart(NewGameChoice(difficulty, allPass, limit, scoring)) }, Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun RadioCard(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val c = Zole.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) c.accent else c.surfaceBorder, RoundedCornerShape(14.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .padding(top = 2.dp)
                .size(18.dp)
                .clip(CircleShape)
                .border(if (selected) 5.dp else 1.5.dp, if (selected) c.accent else c.textSecondary, CircleShape),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ZText(title, size = 16.sp, weight = FontWeight.SemiBold)
            ZText(subtitle, size = 14.sp, color = c.textSecondary, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun Pill(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = Zole.colors
    Box(
        Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) c.primaryButton else c.surface)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) c.primaryButton else c.surfaceBorder, RoundedCornerShape(22.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        ZText(label, size = 15.sp, weight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) c.onPrimaryButton else c.text)
    }
}
