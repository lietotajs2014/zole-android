package lv.zole.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lv.zole.engine.Card

/** Sizes of the text on a card. The defaults match the 86 x 124 hand card. */
data class CardText(val rank: TextUnit = 25.sp, val suit: TextUnit = 21.sp, val bigSuit: TextUnit = 38.sp, val padH: Dp = 9.dp, val padV: Dp = 8.dp)

object CardSizes {
    val HandWidth = 86.dp
    val HandHeight = 124.dp
    val TableWidth = 84.dp
    val TableHeight = 120.dp
    val TableText = CardText(rank = 24.sp, suit = 20.sp, bigSuit = 34.sp)
    val TalonText = CardText(rank = 27.sp, suit = 22.sp, bigSuit = 42.sp, padH = 10.dp, padV = 9.dp)
    val GridText = CardText(rank = 22.sp, suit = 19.sp, bigSuit = 32.sp)
}

/** A flat card: rank and suit top-left, big suit bottom-right, optional trump dot. */
@Composable
fun PlayingCard(
    card: Card,
    modifier: Modifier = Modifier,
    text: CardText = CardText(),
    selected: Boolean = false,
    dimmed: Boolean = false,
    radius: Dp = 10.dp,
) {
    val c = Zole.colors
    val ink = if (card.suit.isRed) c.cardRed else c.text
    val shape = RoundedCornerShape(radius)
    val name = Zole.strings.cardName(card)
    Box(
        modifier
            .clip(shape)
            .background(if (selected) c.accentSoft else c.cardFace)
            .border(if (selected) 2.dp else 1.dp, if (selected) c.accent else c.cardBorder, shape)
            .drawWithContent {
                drawContent()
                if (dimmed) drawRect(Color.Black.copy(alpha = if (c.isDark) 0.55f else 0.35f))
            }
            .semantics { contentDescription = name },
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = text.padH, vertical = text.padV)) {
            ZText(card.rank.label, size = text.rank, weight = FontWeight.SemiBold, color = ink, lineHeight = text.rank)
            ZText(card.suit.symbol, size = text.suit, weight = FontWeight.SemiBold, color = ink, lineHeight = text.suit * 1.15f)
            if (card.isTrump && LocalCardStyle.current.markTrumps) {
                Box(Modifier.padding(top = 5.dp).size(6.dp).clip(CircleShape).background(c.accent))
            }
        }
        ZText(
            card.suit.symbol,
            Modifier.align(Alignment.BottomEnd).padding(horizontal = text.padH, vertical = text.padV),
            size = text.bigSuit, weight = FontWeight.SemiBold, color = ink, lineHeight = text.bigSuit,
        )
    }
}

@Composable
fun CardBack(modifier: Modifier = Modifier, radius: Dp = 10.dp) {
    val c = Zole.colors
    Box(
        modifier
            .clip(RoundedCornerShape(radius))
            .background(c.cardBack)
            .border(1.dp, c.cardBackBorder, RoundedCornerShape(radius))
            .padding(6.dp),
    ) {
        Box(Modifier.fillMaxSize().border(1.dp, c.cardBackInner, RoundedCornerShape(5.dp)))
    }
}
