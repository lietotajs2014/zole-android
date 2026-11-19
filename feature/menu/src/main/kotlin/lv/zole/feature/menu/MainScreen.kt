package lv.zole.feature.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lv.zole.core.ui.OutlineButton
import lv.zole.core.ui.PrimaryButton
import lv.zole.core.ui.ZIcon
import lv.zole.core.ui.ZText
import lv.zole.core.ui.Zole
import lv.zole.core.ui.ZoleIcons

@Composable
fun MainScreen(
    continueDeal: Int?,
    onPlay: () -> Unit,
    onContinue: () -> Unit,
    onStats: () -> Unit,
    onRules: () -> Unit,
    onSettings: () -> Unit,
) {
    val c = Zole.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .systemBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 24.dp),
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically),
        ) {
            HeroCards()
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ZText("Zole", size = 56.sp, weight = FontWeight.Bold, letterSpacing = (-1).sp)
                ZText("bet ✨minimalist✨", size = 15.sp, color = c.textSecondary)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton("Spēlēt", onPlay)
            if (continueDeal != null) {
                OutlineButton(onContinue) {
                    ZText("Turpināt partiju", size = 16.sp, weight = FontWeight.Medium)
                    ZText("· $continueDeal. dale", size = 16.sp, weight = FontWeight.Medium, color = c.textSecondary)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 32.dp)) {
            NavItem(ZoleIcons.Stats, "Statistika", onStats)
            NavItem(ZoleIcons.Rules, "Noteikumi", onRules)
            NavItem(ZoleIcons.Gear, "Iestatījumi", onSettings)
        }
    }
}

@Composable
private fun RowScope.NavItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    val c = Zole.colors
    Column(
        Modifier
            .weight(1f)
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        ZIcon(icon, null, tint = c.textTertiary)
        ZText(label, size = 13.sp, color = c.textTertiary)
    }
}

@Composable
private fun HeroCards() {
    Box(Modifier.size(180.dp, 120.dp)) {
        HeroCard("Q", "♣", red = false, x = 22, y = 18, angle = -12f)
        HeroCard("J", "♠", red = false, x = 58, y = 6, angle = 0f)
        HeroCard("A", "♦", red = true, x = 94, y = 18, angle = 12f)
    }
}

@Composable
private fun HeroCard(rank: String, suit: String, red: Boolean, x: Int, y: Int, angle: Float) {
    val c = Zole.colors
    val ink = if (red) c.cardRed else c.text
    Box(
        Modifier
            .offset(x.dp, y.dp)
            .size(64.dp, 92.dp)
            .rotate(angle)
            .clip(RoundedCornerShape(9.dp))
            .background(c.surface)
            .border(1.dp, c.surfaceBorder, RoundedCornerShape(9.dp))
            .padding(horizontal = 9.dp, vertical = 8.dp),
    ) {
        ZText(rank, size = 22.sp, weight = FontWeight.SemiBold, color = ink)
        ZText(suit, Modifier.align(Alignment.BottomEnd), size = 22.sp, weight = FontWeight.SemiBold, color = ink)
    }
}
