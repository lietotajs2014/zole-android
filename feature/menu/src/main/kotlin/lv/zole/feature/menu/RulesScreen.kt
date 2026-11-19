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
import lv.zole.engine.Rules

@Composable
fun RulesScreen(onBack: () -> Unit) {
    val c = Zole.colors
    val rules = Rules()
    Column(Modifier.fillMaxSize().background(c.background).systemBarsPadding().padding(horizontal = 20.dp)) {
        BackHeader("Noteikumi", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Block("Kārtis", "26 kārtis, kopā 120 acis. Dūzis 11, desmitnieks 10, kungs 4, dāma 3, kalps 2, pārējās 0.")
            Block(
                "Trumpji",
                "14 trumpji no stiprākā: ♣Q ♠Q ♥Q ♦Q, ♣J ♠J ♥J ♦J, ♦A ♦10 ♦K ♦9 ♦8 ♦7. " +
                    "Pārējos mastos (♣ ♠ ♥) ir A, 10, K, 9.",
            )
            Block(
                "Solīšana",
                "Katram 8 kārtis, 2 galdā. Sāk pirmā roka. Garām: nodod nākamajam. Ņemu: paņem galda kārtis un noliek 2, " +
                    "to acis skaitās tev. Zole: spēlē bez galda kārtīm, tās skaitās mazajiem. Mazā zole (ja ieslēgta): " +
                    "jānepaņem neviens stiķis.",
            )
            Block(
                "Ja visi garām",
                "Galdiņš: katrs par sevi, zaudē tas, kam visvairāk stiķu (vienādi stiķi: vairāk acu). " +
                    "Pules: pieraksta kopējo puli, un dala nākamais.",
            )
            Block(
                "Izspēle",
                "Jāliek tā paša masta kārts (trumpji ir viens masts). Ja tādas nav, drīkst likt jebko. " +
                    "Lielajam vajag vismaz 61 aci.",
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Punktu tabula · no katra pretinieka")
                SurfaceCard {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                        ZText("Rezultāts", Modifier.weight(1f), size = 12.sp, color = c.textSecondary, weight = FontWeight.SemiBold)
                        ZText("Ņemu", Modifier.weight(0.4f), size = 12.sp, color = c.textSecondary, weight = FontWeight.SemiBold, align = TextAlign.End)
                        ZText("Zole", Modifier.weight(0.4f), size = 12.sp, color = c.textSecondary, weight = FontWeight.SemiBold, align = TextAlign.End)
                    }
                    val rows = listOf(
                        Triple("Uzvara, 61–90 acis", rules.normal.win, rules.zole.win),
                        Triple("Uzvara, mazie jaņos (91+)", rules.normal.winJani, rules.zole.winJani),
                        Triple("Uzvara, visi stiķi", rules.normal.winAllTricks, rules.zole.winAllTricks),
                        Triple("Zaudējums, 31–60 acis", rules.normal.lose, rules.zole.lose),
                        Triple("Zaudējums, jaņos (≤30)", rules.normal.loseJani, rules.zole.loseJani),
                        Triple("Zaudējums, bez stiķa", rules.normal.loseNoTricks, rules.zole.loseNoTricks),
                    )
                    rows.forEach { (label, n, z) ->
                        Divider()
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp)) {
                            ZText(label, Modifier.weight(1f), size = 14.sp)
                            ZText(fmt(n), Modifier.weight(0.4f), size = 14.sp, align = TextAlign.End)
                            ZText(fmt(z), Modifier.weight(0.4f), size = 14.sp, align = TextAlign.End)
                        }
                    }
                }
                ZText(
                    "Mazā zole: ${fmt(rules.smallZoleWin)} / ${fmt(rules.smallZoleLose)}. Galdiņš: zaudētājs maksā ${rules.tableLoserPays} katram. " +
                        "Pules: uzvarētājs paņem kopējo puli (+${rules.commonPoolValue} no katra) vai cita personīgo puli (+${rules.personalPoolValue}); " +
                        "zaudētājs, kamēr ir pules, saņem personīgo puli.",
                    size = 13.sp, color = c.textSecondary, lineHeight = 19.sp,
                )
            }
        }
    }
}

private fun fmt(v: Int) = if (v > 0) "+$v" else v.toString().replace("-", "−")

@Composable
private fun Block(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(title)
        ZText(body, size = 15.sp, lineHeight = 22.sp, color = Zole.colors.textTertiary)
    }
}
