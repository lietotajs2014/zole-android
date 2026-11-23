package lv.zole.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ZText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 15.sp,
    weight: FontWeight = FontWeight.Normal,
    color: Color = Zole.colors.text,
    align: TextAlign? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = size,
        fontWeight = weight,
        fontFamily = Inter,
        color = color,
        textAlign = align,
        letterSpacing = letterSpacing,
        lineHeight = lineHeight,
        maxLines = maxLines,
    )
}

/** Small stroked icons taken from the design SVGs (24x24 viewBox). */
object ZoleIcons {
    private fun stroked(name: String, strokeWidth: Float, vararg paths: String): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        for (d in paths) {
            b.addPath(
                pathData = addPathNodes(d),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    val Back = stroked("back", 1.8f, "M15 5l-7 7 7 7")
    val Chevron = stroked("chevron", 1.8f, "M9 5l7 7-7 7")
    val Menu = stroked("menu", 1.8f, "M4 7h16M4 12h16M4 17h16")
    val Stats = stroked("stats", 1.6f, "M5 20V11M12 20V4M19 20v-6")
    val Rules = stroked("rules", 1.6f, "M5 4h10l4 4v12H5z", "M9 12h6M9 16h6")
    val Gear = stroked(
        "gear", 1.6f,
        "M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z",
        "M15 12a3 3 0 1 1-6 0a3 3 0 1 1 6 0z",
    )
}

@Composable
fun ZIcon(icon: ImageVector, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = Zole.colors.text, size: Dp = 22.dp) {
    Icon(icon, contentDescription, modifier.size(size), tint = tint)
}

@Composable
fun IconTap(icon: ImageVector, contentDescription: String, onClick: () -> Unit, tint: Color = Zole.colors.text) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) { ZIcon(icon, null, tint = tint) }
}

/** Back arrow + page title, as on Settings, New game and Scoreboard. */
@Composable
fun BackHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().padding(top = 16.dp).offset(x = (-8).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTap(ZoleIcons.Back, Zole.strings.back, onBack)
        ZText(title, Modifier.padding(start = 4.dp).weight(1f), size = 22.sp, weight = FontWeight.SemiBold)
        trailing()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    ZText(
        text.uppercase(), modifier, size = 13.sp, weight = FontWeight.SemiBold,
        color = Zole.colors.textSecondary, letterSpacing = 0.8.sp,
    )
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = Zole.colors
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) c.primaryButton else c.disabledButton)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ZText(text, size = 17.sp, weight = FontWeight.SemiBold, color = if (enabled) c.onPrimaryButton else c.onDisabledButton)
    }
}

@Composable
fun OutlineButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Zole.colors.outlineButton, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun LinkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { ZText(text, size = 15.sp, weight = FontWeight.Medium, color = Zole.colors.accent) }
}

/** Segmented control: grey track, selected segment lifted. */
@Composable
fun Segmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    outerRadius: Dp = 12.dp,
    fontSize: TextUnit = 15.sp,
) {
    val c = Zole.colors
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(outerRadius)).background(c.segmentBackground).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            val bg by animateColorAsState(if (on) c.segmentSelected else Color.Transparent, label = "seg")
            Box(
                Modifier
                    .weight(1f)
                    .height(height)
                    .clip(RoundedCornerShape(outerRadius - 3.dp))
                    .background(bg)
                    .clickable(role = Role.Tab) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                ZText(label, size = fontSize, weight = if (on) FontWeight.SemiBold else FontWeight.Normal, color = if (on) c.text else c.textTertiary)
            }
        }
    }
}

/** iOS-like switch from the Settings design (51 x 31). */
@Composable
fun ZoleSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, contentDescription: String) {
    val c = Zole.colors
    val bg by animateColorAsState(if (checked) c.accent else c.toggleOff, label = "switch")
    val knob by animateDpAsState(if (checked) 22.dp else 2.dp, label = "knob")
    Box(
        Modifier
            .size(51.dp, 31.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Switch) { onCheckedChange(!checked) }
            .semantics { this.contentDescription = contentDescription },
    ) {
        Box(Modifier.offset(x = knob, y = 2.dp).size(27.dp).clip(CircleShape).background(Color.White))
    }
}

@Composable
fun SurfaceCard(modifier: Modifier = Modifier, borderColor: Color = Zole.colors.surfaceBorder, borderWidth: Dp = 1.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Zole.colors.surface)
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp)),
        content = content,
    )
}

@Composable
fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Zole.colors.divider))
}

@Composable
fun Avatar(letter: String, modifier: Modifier = Modifier) {
    Box(modifier.size(48.dp).clip(CircleShape).background(Zole.colors.avatar), contentAlignment = Alignment.Center) {
        ZText(letter, size = 18.sp, weight = FontWeight.SemiBold, color = Zole.colors.textTertiary)
    }
}

@Composable
fun StatusChip(text: String, modifier: Modifier = Modifier) {
    val c = Zole.colors
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.surface)
            .border(1.dp, c.surfaceBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) { ZText(text, size = 13.sp, color = c.textSecondary) }
}

@Composable
fun Spacer(height: Dp) = androidx.compose.foundation.layout.Spacer(Modifier.height(height))

@Composable
fun HSpacer(width: Dp) = androidx.compose.foundation.layout.Spacer(Modifier.width(width))

