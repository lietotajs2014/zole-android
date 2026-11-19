package lv.zole.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import lv.zole.engine.Card
import kotlin.math.cos
import kotlin.math.sin

/** Fan geometry from the design: 86 x 124 cards rotating around a point 460 dp below the card top. */
private object Fan {
    const val STEP_DEG = 36f / 7f
    val CardW = 86.dp
    val CardH = 124.dp
    val Top = 30.dp
    val Pivot = 460.dp
    val Lift = 32.dp
    val Height = 186.dp
}

/**
 * The player's hand held as a fan, rightmost card on top. Holding a finger on a card lifts it along its
 * own axis; sliding moves the lift to the next card. Releasing on a playable card plays it.
 *
 * @param playable legal cards when it is the player's turn to play (others are dimmed); null otherwise.
 * @param highlight a card shown lifted with a blue border (the only legal card).
 */
@Composable
fun HandFan(
    cards: List<Card>,
    modifier: Modifier = Modifier,
    playable: Set<Card>? = null,
    highlight: Card? = null,
    onPlay: ((Card) -> Unit)? = null,
    dealKey: Any? = null,
    fastAnimations: Boolean = false,
) {
    var held by remember { mutableStateOf<Card?>(null) }
    val currentCards by rememberUpdatedState(cards)
    val currentPlayable by rememberUpdatedState(playable)
    val currentOnPlay by rememberUpdatedState(onPlay)
    val density = LocalDensity.current

    BoxWithConstraints(modifier.fillMaxWidth().height(Fan.Height)) {
        val widthPx = constraints.maxWidth.toFloat()
        val n = cards.size

        fun angleOf(i: Int, count: Int) = (i - (count - 1) / 2f) * Fan.STEP_DEG

        fun cardAt(point: Offset): Card? = with(density) {
            val list = currentCards
            val w = Fan.CardW.toPx()
            val h = Fan.CardH.toPx()
            val left = widthPx / 2 - w / 2
            val top = Fan.Top.toPx()
            val pivot = Offset(widthPx / 2, top + Fan.Pivot.toPx())
            for (i in list.indices.reversed()) {
                val a = Math.toRadians(angleOf(i, list.size).toDouble())
                val lifted = list[i] == held || list[i] == highlight
                val lift = if (lifted) Fan.Lift.toPx() else 0f
                val p = Offset(point.x - (sin(a) * lift).toFloat(), point.y + (cos(a) * lift).toFloat()) - pivot
                // rotate by -a
                val x = (p.x * cos(-a) - p.y * sin(-a)).toFloat() + pivot.x
                val y = (p.x * sin(-a) + p.y * cos(-a)).toFloat() + pivot.y
                if (x in left..(left + w) && y in top..(top + h)) return list[i]
            }
            null
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(Fan.Height)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        held = cardAt(down.position)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            val over = cardAt(change.position)
                            if (over != null) held = over
                        }
                        val chosen = held
                        held = null
                        val legal = currentPlayable
                        if (chosen != null && legal != null && chosen in legal) currentOnPlay?.invoke(chosen)
                    }
                },
        ) {
            cards.forEachIndexed { i, card ->
                val angle = angleOf(i, n)
                val isLifted = card == held || card == highlight
                val lift by animateFloatAsState(if (isLifted) 1f else 0f, tween(if (fastAnimations) 60 else 120), label = "lift")
                val appear = remember(dealKey, card) { Animatable(if (dealKey == null) 1f else 0f) }
                LaunchedEffect(dealKey, card) {
                    if (appear.value < 1f) {
                        kotlinx.coroutines.delay((if (fastAnimations) 30L else 70L) * i)
                        appear.animateTo(1f, tween(if (fastAnimations) 120 else 240))
                    }
                }
                val dim = playable != null && card !in playable
                Box(
                    Modifier
                        .zIndex(i.toFloat())
                        .offset(x = with(density) { (widthPx / 2).toDp() } - Fan.CardW / 2, y = Fan.Top)
                        .size(Fan.CardW, Fan.CardH)
                        .graphicsLayer {
                            val a = Math.toRadians(angle.toDouble())
                            val l = Fan.Lift.toPx() * lift
                            transformOrigin = TransformOrigin(0.5f, Fan.Pivot.toPx() / Fan.CardH.toPx())
                            rotationZ = angle
                            translationX = (sin(a) * l).toFloat()
                            translationY = (-cos(a) * l).toFloat() + (1f - appear.value) * 60.dp.toPx()
                            alpha = appear.value
                        }
                        .shadow(6.dp, RoundedCornerShape(10.dp), clip = false, ambientColor = Color.Black, spotColor = Color.Black),
                ) {
                    PlayingCard(
                        card,
                        Modifier.size(Fan.CardW, Fan.CardH),
                        selected = isLifted && (playable == null || card in playable) && (highlight == card || playable != null),
                        dimmed = dim,
                    )
                }
            }
        }
    }
}
