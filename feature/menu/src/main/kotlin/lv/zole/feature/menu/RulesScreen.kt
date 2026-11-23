package lv.zole.feature.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lv.zole.core.ui.BackHeader
import lv.zole.core.ui.Divider
import lv.zole.core.ui.SectionLabel
import lv.zole.core.ui.SurfaceCard
import lv.zole.core.ui.ZText
import lv.zole.core.ui.Zole
import lv.zole.core.ui.signedPoints
import lv.zole.engine.Rules
import lv.zole.engine.ScoringVariant

@Composable
fun RulesScreen(scoring: ScoringVariant, onBack: () -> Unit) {
    val c = Zole.colors
    val t = Zole.strings
    val rules = Rules(scoring = scoring)
    Column(Modifier.fillMaxSize().background(c.background).systemBarsPadding().padding(horizontal = 20.dp)) {
        BackHeader(t.rules, onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Block(t.cardsTitle, t.cardsBody)
            Block(t.trumpsTitle, t.trumpsBody)
            Block(t.biddingTitle, t.biddingBody)
            Block(t.allPassTitle, t.allPassBody(scoring))
            Block(t.playTitle, t.playBody)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(t.scoringTableTitle(scoring))
                SurfaceCard {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                        ZText(t.result, Modifier.weight(1f), size = 12.sp, color = c.textSecondary, weight = FontWeight.SemiBold)
                        ZText(t.take, Modifier.weight(0.4f), size = 12.sp, color = c.textSecondary, weight = FontWeight.SemiBold, align = TextAlign.End)
                        ZText(t.zole, Modifier.weight(0.4f), size = 12.sp, color = c.textSecondary, weight = FontWeight.SemiBold, align = TextAlign.End)
                    }
                    val values = listOf(
                        rules.normal.win to rules.zole.win,
                        rules.normal.winJani to rules.zole.winJani,
                        rules.normal.winAllTricks to rules.zole.winAllTricks,
                        rules.normal.lose to rules.zole.lose,
                        rules.normal.loseJani to rules.zole.loseJani,
                        rules.normal.loseNoTricks to rules.zole.loseNoTricks,
                    )
                    t.scoringRows.zip(values).forEach { (label, v) ->
                        Divider()
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp)) {
                            ZText(label, Modifier.weight(1f), size = 14.sp)
                            ZText(signedPoints(v.first), Modifier.weight(0.4f), size = 14.sp, align = TextAlign.End)
                            ZText(signedPoints(v.second), Modifier.weight(0.4f), size = 14.sp, align = TextAlign.End)
                        }
                    }
                }
                ZText(t.scoringNote(rules), size = 13.sp, color = c.textSecondary, lineHeight = 19.sp)
            }
        }
    }
}

@Composable
private fun Block(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(title)
        ZText(body, size = 15.sp, lineHeight = 22.sp, color = Zole.colors.textTertiary)
    }
}
