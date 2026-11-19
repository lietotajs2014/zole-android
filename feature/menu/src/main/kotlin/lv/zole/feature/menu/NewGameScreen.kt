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
import androidx.compose.foundation.layout.heightIn
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
import lv.zole.core.ui.ZIcon
import lv.zole.core.ui.ZText
import lv.zole.core.ui.Zole
import lv.zole.core.ui.ZoleIcons
import lv.zole.engine.AllPassMode
import lv.zole.engine.Difficulty

data class NewGameChoice(
    val difficulty: Difficulty,
    val allPassMode: AllPassMode,
    /** 0 = no limit */
    val dealLimit: Int,
)

@Composable
fun NewGameScreen(
    initial: NewGameChoice,
    onBack: () -> Unit,
    onScoringTable: () -> Unit,
    onStart: (NewGameChoice) -> Unit,
) {
    val c = Zole.colors
    var difficulty by rememberSaveable { mutableStateOf(initial.difficulty) }
    var allPass by rememberSaveable { mutableStateOf(initial.allPassMode) }
    var limit by rememberSaveable { mutableStateOf(initial.dealLimit) }

    Column(
        Modifier.fillMaxSize().background(c.background).systemBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
    ) {
        BackHeader("Jauna partija", onBack, Modifier.padding(bottom = 24.dp))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Pretinieki")
                Segmented(listOf("Viegls", "Vidējs", "Grūts"), difficulty.ordinal, { difficulty = Difficulty.entries[it] })
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Ja visi saka garām")
                RadioCard("Pules", "Pieraksta kopējo puli, un dala nākamais", allPass == AllPassMode.POOL) { allPass = AllPassMode.POOL }
                RadioCard("Galdiņš", "Katrs par sevi; zaudē tas, kam visvairāk stiķu", allPass == AllPassMode.TABLE) { allPass = AllPassMode.TABLE }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Partijas garums")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("12 dales", limit == 12) { limit = 12 }
                    Pill("24 dales", limit == 24) { limit = 24 }
                    Pill("Bez limita", limit == 0) { limit = 0 }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Noteikumi")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.surface)
                        .border(1.dp, c.surfaceBorder, RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button, onClick = onScoringTable)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ZText("Punktu tabula", Modifier.weight(1f))
                    ZText("Klasiskā", color = c.textSecondary)
                    ZIcon(ZoleIcons.Chevron, null, Modifier.padding(start = 6.dp), tint = c.textSecondary, size = 16.dp)
                }
            }
        }
        PrimaryButton("Sākt", { onStart(NewGameChoice(difficulty, allPass, limit)) }, Modifier.padding(top = 16.dp))
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
