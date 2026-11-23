package lv.zole.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Turn timer: a circle that goes from white to black clockwise as time runs out.
 * [remaining] is 1.0 at the start of the turn and 0.0 when time is up.
 */
@Composable
fun TurnTimerCircle(remaining: Float, secondsLeft: Int, totalSeconds: Int, modifier: Modifier = Modifier, size: Dp = 30.dp) {
    val fg = Zole.colors.text
    val bg = Zole.colors.background
    val description = Zole.strings.timer(secondsLeft, totalSeconds)
    Canvas(
        modifier
            .size(size)
            .semantics { contentDescription = description },
    ) {
        val stroke = 2.dp.toPx()
        drawCircle(bg)
        val f = remaining.coerceIn(0f, 1f)
        if (f > 0f) {
            drawArc(fg, startAngle = -90f, sweepAngle = 360f * f, useCenter = true, topLeft = Offset.Zero, size = Size(this.size.width, this.size.height))
        }
        drawCircle(fg, radius = this.size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
    }
}
