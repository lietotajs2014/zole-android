package lv.zole.feature.game

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lv.zole.core.data.AppSettings
import lv.zole.core.ui.Avatar
import lv.zole.core.ui.CardBack
import lv.zole.core.ui.CardSizes
import lv.zole.core.ui.CardText
import lv.zole.core.ui.Divider
import lv.zole.core.ui.HandFan
import lv.zole.core.ui.IconTap
import lv.zole.core.ui.LinkButton
import lv.zole.core.ui.PlayingCard
import lv.zole.core.ui.PrimaryButton
import lv.zole.core.ui.StatusChip
import lv.zole.core.ui.TurnTimerCircle
import lv.zole.core.ui.ZText
import lv.zole.core.ui.Zole
import lv.zole.core.ui.ZoleIcons
import lv.zole.engine.AllPassMode
import lv.zole.engine.BidChoice
import lv.zole.engine.Card
import lv.zole.engine.Contract
import lv.zole.engine.DealRecord
import lv.zole.engine.MatchState
import lv.zole.engine.Phase
import lv.zole.engine.Play
import lv.zole.engine.PlayerView
import lv.zole.engine.Seat
import lv.zole.engine.TrickRules
import lv.zole.engine.sortedForHand

@Composable
fun GameRoute(viewModel: GameViewModel, onMenu: () -> Unit, onScoreboard: () -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentSettings by rememberUpdatedState(settings)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { vibe -> if (currentSettings.vibration) vibrate(context, vibe) }
    }
    GameScreen(
        ui = ui,
        settings = settings,
        onMenu = onMenu,
        onScoreboard = onScoreboard,
        onBid = viewModel::bid,
        onDiscard = viewModel::discard,
        onPlay = viewModel::play,
        onNextDeal = viewModel::nextDeal,
    )
}

private fun vibrate(context: Context, vibe: Vibe) {
    val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
    if (vibrator == null || !vibrator.hasVibrator()) return
    val effect = when (vibe) {
        Vibe.TURN_START -> VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
        Vibe.HALF_TIME -> VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)
        Vibe.FIVE_LEFT -> VibrationEffect.createWaveform(longArrayOf(0, 60, 90, 60), -1)
        Vibe.TRICK_WON -> VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
    }
    vibrator.vibrate(effect)
}

@Composable
fun GameScreen(
    ui: GameUi,
    settings: AppSettings,
    onMenu: () -> Unit,
    onScoreboard: () -> Unit,
    onBid: (BidChoice) -> Unit,
    onDiscard: (List<Card>) -> Unit,
    onPlay: (Card) -> Unit,
    onNextDeal: () -> Unit,
) {
    val c = Zole.colors
    val match = ui.match
    val view = ui.view
    Box(Modifier.fillMaxSize().background(c.background)) {
        if (match == null || view == null) return@Box
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(bottom = 22.dp)) {
            TableHeader(headerText(match, view), match.totals.getValue(HUMAN), onMenu, onScoreboard)
            when (val phase = view.phase) {
                is Phase.Bidding -> BiddingTable(ui, view, settings, onBid)
                is Phase.Discarding ->
                    if (phase.declarer == HUMAN) DiscardTable(ui, view, match.dealNumber, onDiscard)
                    else BiddingTable(ui, view, settings, onBid)
                is Phase.Playing -> PlayTable(ui, view, settings, onPlay)
                is Phase.Finished -> FinishedTable(view)
            }
        }
        val finished = view.phase as? Phase.Finished
        val record = match.lastRecord
        if (finished != null && record != null && record.number == match.dealNumber) {
            ResultSheet(match, record, onNext = { if (match.isOver) onScoreboard() else onNextDeal() }, onScoreboard = onScoreboard)
        }
    }
}

private fun headerText(match: MatchState, view: PlayerView): String {
    val n = match.dealNumber
    return when (view.phase) {
        is Phase.Bidding -> if (match.config.rules.allPassMode == AllPassMode.POOL) {
            val pools = match.pools.total
            "Dale $n · " + if (pools == 0) "Pules nav" else "Pules: $pools"
        } else "Dale $n · Solīšana"
        is Phase.Discarding -> "Dale $n · ${view.declarer?.displayName()} ņem"
        is Phase.Playing -> "Dale $n · ${minOf(view.tricks.size + 1, 8)}. stiķis no 8"
        is Phase.Finished -> "Dale $n · beigusies"
    }
}

@Composable
private fun TableHeader(center: String, total: Int, onMenu: () -> Unit, onScoreboard: () -> Unit) {
    val c = Zole.colors
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp, start = 17.dp, end = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(56.dp)) { IconTap(ZoleIcons.Menu, "Izvēlne", onMenu) }
        ZText(center, Modifier.weight(1f), size = 14.sp, color = c.textSecondary, align = TextAlign.Center, maxLines = 1)
        Box(
            Modifier
                .width(56.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button, onClick = onScoreboard),
            contentAlignment = Alignment.CenterEnd,
        ) { ZText(signed(total), size = 17.sp, weight = FontWeight.Bold) }
    }
}

// ---------- Opponents ----------

@Composable
private fun Opponents(modifier: Modifier = Modifier, status: @Composable (Seat) -> Unit) {
    Row(modifier.fillMaxWidth().padding(start = 33.dp, end = 33.dp, top = 28.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        for (seat in listOf(Seat(1), Seat(2))) {
            Column(Modifier.width(96.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Avatar(seat.initial())
                ZText(seat.displayName(), size = 15.sp, weight = FontWeight.Medium)
                status(seat)
            }
        }
    }
}

@Composable
private fun BidStatus(seat: Seat, view: PlayerView, thinking: Seat?) {
    Box(Modifier.heightIn(min = 24.dp), contentAlignment = Alignment.Center) {
        val bid = view.bids[seat]
        when {
            bid != null -> StatusChip(bid.label())
            thinking == seat -> StatusChip("Domā…")
        }
    }
}

// ---------- Bidding ----------

@Composable
private fun ColumnScope.BiddingTable(ui: GameUi, view: PlayerView, settings: AppSettings, onBid: (BidChoice) -> Unit) {
    val c = Zole.colors
    val myTurn = view.phase is Phase.Bidding && view.isMyTurn && !ui.busy
    Opponents { BidStatus(it, view, ui.thinking) }
    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (view.phase is Phase.Bidding) {
                repeat(2) { CardBack(Modifier.size(CardSizes.TableWidth, CardSizes.TableHeight)) }
            }
        }
    }
    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().height(32.dp), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            if (myTurn) {
                ZText("Tavs lēmums", size = 22.sp, weight = FontWeight.Bold)
                Timer(ui.turnStartedAt, 30.dp)
            } else {
                val who = view.turn
                val text = when {
                    view.phase is Phase.Discarding -> "${view.declarer?.displayName()} maina kārtis…"
                    who != null && who != HUMAN -> "${who.displayName()} domā…"
                    else -> ""
                }
                ZText(text, size = 17.sp, weight = FontWeight.Medium, color = c.textSecondary)
            }
        }
        val options = buildList {
            add(BidChoice.PASS); add(BidChoice.TAKE); add(BidChoice.ZOLE)
            if (view.rules.smallZoleEnabled) add(BidChoice.SMALL_ZOLE)
        }
        Row(Modifier.fillMaxWidth().alpha(if (myTurn) 1f else 0.4f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { choice ->
                BidButton(choice, enabled = myTurn, compact = options.size > 3, modifier = Modifier.weight(1f)) { onBid(choice) }
            }
        }
    }
    HandFan(
        cards = view.hand,
        modifier = Modifier.offset(y = 6.dp),
        dealKey = ui.match?.dealNumber,
        fastAnimations = settings.fastAnimations,
    )
}

@Composable
private fun BidButton(choice: BidChoice, enabled: Boolean, compact: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Zole.colors
    val shape = RoundedCornerShape(16.dp)
    val (bg, fg, border) = when (choice) {
        BidChoice.PASS -> Triple(c.surface, c.text, c.outlineButton)
        BidChoice.TAKE -> Triple(c.primaryButton, c.onPrimaryButton, c.primaryButton)
        BidChoice.ZOLE, BidChoice.SMALL_ZOLE -> Triple(Color.Transparent, c.accent, c.accent)
    }
    Box(
        modifier
            .height(64.dp)
            .clip(shape)
            .background(bg)
            .border(if (choice == BidChoice.ZOLE || choice == BidChoice.SMALL_ZOLE) 1.5.dp else 1.dp, border, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ZText(choice.label(), size = if (compact) 15.sp else 18.sp, weight = FontWeight.SemiBold, color = fg, align = TextAlign.Center)
    }
}

@Composable
private fun Timer(startedAt: Long?, size: Dp) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAt) {
        while (startedAt != null) {
            withFrameMillis { now = System.currentTimeMillis() }
        }
    }
    val total = TurnTimer.durationMs
    val elapsed = if (startedAt == null) 0 else (now - startedAt).coerceIn(0, total)
    val remaining = 1f - elapsed.toFloat() / total
    val secondsLeft = ((total - elapsed + 999) / 1000).toInt()
    TurnTimerCircle(remaining, secondsLeft, (total / 1000).toInt(), size = size)
}

// ---------- Discard ----------

@Composable
private fun ColumnScope.DiscardTable(ui: GameUi, view: PlayerView, dealNumber: Int, onDiscard: (List<Card>) -> Unit) {
    val c = Zole.colors
    val selected = remember(dealNumber) { mutableStateListOf<Card>() }
    val talon = view.talonInHand
    val rest = (view.hand - talon.toSet()).sortedForHand()
    fun toggle(card: Card) {
        if (card in selected) selected.remove(card)
        else if (selected.size < 2) selected.add(card)
        else { selected.removeAt(0); selected.add(card) }
    }
    Box(Modifier.weight(1f).fillMaxWidth()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth().padding(bottom = 36.dp), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                talon.forEach { card ->
                    SelectableCard(card, card in selected, CardSizes.TalonText, Modifier.size(98.dp, 140.dp)) { toggle(card) }
                }
            }
            rest.chunked(4).forEachIndexed { rowIndex, row ->
                Row(Modifier.fillMaxWidth().padding(top = if (rowIndex > 0) 10.dp else 0.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { card ->
                        SelectableCard(card, card in selected, CardSizes.GridText, Modifier.weight(1f).height(112.dp)) { toggle(card) }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(44.dp, Alignment.CenterHorizontally)) {
                ZText("${selected.size}/2", size = 20.sp, weight = FontWeight.Bold, color = if (selected.size == 2) c.accent else c.text)
                Row {
                    ZText("Punkti - ", size = 20.sp, color = c.textTertiary)
                    ZText(selected.sumOf { it.points }.toString(), size = 20.sp, weight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.weight(0.4f))
            PrimaryButton("Nolikt un sākt", { onDiscard(selected.toList()) }, enabled = selected.size == 2 && !ui.busy)
        }
        Opponents(Modifier.align(Alignment.TopCenter)) { BidStatus(it, view, null) }
    }
}

@Composable
private fun SelectableCard(card: Card, selected: Boolean, text: CardText, modifier: Modifier, onClick: () -> Unit) {
    val lift by animateFloatAsState(if (selected) -4f else 0f, tween(120), label = "lift")
    Box(
        modifier
            .graphicsLayer { translationY = lift * density }
            .clip(RoundedCornerShape(10.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Checkbox, onClick = onClick),
    ) {
        PlayingCard(card, Modifier.fillMaxSize(), text = text, selected = selected)
    }
}

// ---------- Play ----------

@Composable
private fun ColumnScope.PlayTable(ui: GameUi, view: PlayerView, settings: AppSettings, onPlay: (Card) -> Unit) {
    val c = Zole.colors
    val myTurn = view.isMyTurn && !ui.busy
    val legal = if (myTurn) view.legalCards() else emptyList()
    Opponents { seat ->
        val role = roleOf(seat, view.declarer, view.contract)
        ZText(
            listOf(role, cardsWord(view.cardsLeft.getValue(seat))).filter { it.isNotEmpty() }.joinToString("\n"),
            size = 13.sp, color = c.textSecondary, align = TextAlign.Center, lineHeight = 17.sp,
        )
    }
    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        TrickArea(ui.shownTrick, showSlot = !ui.busy && ui.shownTrick.none { it.seat == HUMAN }, revealing = ui.busy)
    }
    Row(
        Modifier.fillMaxWidth().height(32.dp).padding(bottom = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (myTurn) {
            ZText("Tavs gājiens", size = 22.sp, weight = FontWeight.Bold)
            Timer(ui.turnStartedAt, 30.dp)
        } else if (!ui.busy) {
            val who = view.turn
            if (who != null && who != HUMAN) ZText("${who.displayName()} domā…", size = 17.sp, weight = FontWeight.Medium, color = c.textSecondary)
        }
    }
    Spacer(Modifier.height(20.dp))
    HandFan(
        cards = view.hand,
        modifier = Modifier.offset(y = 6.dp),
        playable = if (myTurn) legal.toSet() else null,
        highlight = if (myTurn) legal.singleOrNull() else null,
        onPlay = if (myTurn) onPlay else null,
        fastAnimations = settings.fastAnimations,
    )
}

private data class SlotPos(val x: Dp, val y: Dp, val angle: Float, val fromX: Float, val fromY: Float)

private fun slotOf(seat: Seat) = when (seat.index) {
    1 -> SlotPos(28.dp, 14.dp, -8f, -160f, -120f)
    2 -> SlotPos(148.dp, 14.dp, 8f, 160f, -120f)
    else -> SlotPos(88.dp, 120.dp, 0f, 0f, 260f)
}

@Composable
private fun TrickArea(trick: List<Play>, showSlot: Boolean, revealing: Boolean) {
    val c = Zole.colors
    val winner = if (revealing && trick.size == 3) trick[TrickRules.winnerIndex(trick.map { it.card })].seat else null
    Box(Modifier.size(260.dp, 250.dp)) {
        if (showSlot) {
            val pos = slotOf(HUMAN)
            Box(
                Modifier
                    .offset(pos.x, pos.y)
                    .size(CardSizes.TableWidth, CardSizes.TableHeight)
                    .drawBehind {
                        drawRoundRect(
                            color = c.textSecondary.copy(alpha = 0.6f),
                            cornerRadius = CornerRadius(10.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))),
                        )
                    },
            )
        }
        trick.forEach { play ->
            key(play) {
                val pos = slotOf(play.seat)
                val anim = remember { Animatable(0f) }
                LaunchedEffect(Unit) { anim.animateTo(1f, tween(230)) }
                Box(
                    Modifier
                        .offset(pos.x, pos.y)
                        .graphicsLayer {
                            translationX = (1f - anim.value) * pos.fromX * density
                            translationY = (1f - anim.value) * pos.fromY * density
                            alpha = 0.3f + 0.7f * anim.value
                        }
                        .rotate(pos.angle),
                ) {
                    PlayingCard(
                        play.card,
                        Modifier.size(CardSizes.TableWidth, CardSizes.TableHeight),
                        text = CardSizes.TableText,
                        selected = winner == play.seat,
                    )
                }
            }
        }
    }
}

// ---------- Finished ----------

@Composable
private fun ColumnScope.FinishedTable(view: PlayerView) {
    Opponents { Spacer(Modifier.height(24.dp)) }
    Spacer(Modifier.weight(1f))
}

@Composable
private fun BoxScope.ResultSheet(match: MatchState, record: DealRecord, onNext: () -> Unit, onScoreboard: () -> Unit) {
    val c = Zole.colors
    val r = record.result
    Box(
        Modifier
            .fillMaxSize()
            .background(c.scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    )
    Column(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(c.surface)
            .systemBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 5.dp).clip(RoundedCornerShape(3.dp)).background(c.divider))
        val declarer = r.declarer
        val title = when {
            r.contract == null -> "Visi garām"
            r.contract == Contract.TABLE -> if (r.tableLoser == HUMAN) "Tu zaudēji" else "${r.tableLoser?.displayName()} zaudēja"
            declarer == HUMAN -> if (r.outcome.declarerWon) "Tu uzvarēji!" else "Tu zaudēji"
            r.outcome.declarerWon -> "${declarer?.displayName()} uzvarēja"
            else -> "Jūs uzvarējāt!"
        }
        val label = when (r.contract) {
            null -> "Pule"
            Contract.TABLE -> "Galdiņš"
            else -> "${contractWord(r.contract)} · ${outcomeWord(r.outcome)}"
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ZText(label.uppercase(), size = 13.sp, weight = FontWeight.SemiBold, color = c.accent, letterSpacing = 0.8.sp)
            ZText(title, size = 32.sp, weight = FontWeight.Bold, letterSpacing = (-0.6).sp)
            when (r.contract) {
                null -> ZText("Pieraksta kopējo puli. Pules: ${record.poolsAfter.total}", size = 15.sp, color = c.textSecondary)
                Contract.TABLE -> ZText(
                    "Stiķi: " + Seat.ALL.joinToString(" · ") { "${it.displayName()} ${r.tricksBySeat.getValue(it)}" },
                    size = 15.sp, color = c.textSecondary,
                )
                Contract.SMALL_ZOLE -> ZText(
                    "${declarer?.displayName()}: ${r.tricksBySeat.getValue(declarer ?: HUMAN)} stiķi",
                    size = 15.sp, color = c.textSecondary,
                )
                else -> {}
            }
        }
        if ((r.contract == Contract.NORMAL || r.contract == Contract.ZOLE) && declarer != null) {
            PointsBar(declarer, r.declarerPoints, r.defenderPoints)
        }
        Column {
            Divider()
            Seat.ALL.forEach { seat ->
                val delta = record.deltas.getValue(seat)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ZText(
                        seat.displayName(), Modifier.widthIn(min = 64.dp), size = 16.sp,
                        weight = if (seat == HUMAN) FontWeight.SemiBold else FontWeight.Normal, align = TextAlign.End,
                    )
                    ZText(
                        signed(delta), Modifier.widthIn(min = 64.dp), size = 16.sp,
                        weight = if (seat == HUMAN) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (delta > 0) c.accent else c.text, align = TextAlign.Start,
                    )
                }
                Divider()
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PrimaryButton(if (match.isOver) "Partija beigusies" else "Nākamā dale", onNext)
            LinkButton("Punktu tabula", onScoreboard)
        }
    }
}

@Composable
private fun PointsBar(declarer: Seat, declarerPoints: Int, defenderPoints: Int) {
    val c = Zole.colors
    val fraction = (declarerPoints / 120f).coerceIn(0.02f, 0.98f)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().height(12.dp)) {
            Row(Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Box(Modifier.weight(fraction).fillMaxSize().background(c.accent))
                Box(Modifier.weight(1f - fraction).fillMaxSize().background(c.toggleOff))
            }
            Box(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxSize()) {
                    Spacer(Modifier.weight(61f))
                    Box(Modifier.offset(x = (-1).dp, y = (-5).dp).size(2.dp, 22.dp).clip(RoundedCornerShape(1.dp)).background(c.text))
                    Spacer(Modifier.weight(59f))
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.Bottom) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                ZText("${declarer.displayName()} ", size = 13.sp, color = c.textSecondary)
                ZText(declarerPoints.toString(), size = 18.sp, weight = FontWeight.Bold, color = c.accent)
            }
            ZText("61", size = 12.sp, color = c.textSecondary)
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.Bottom) {
                ZText("Mazie ", size = 13.sp, color = c.textSecondary)
                ZText(defenderPoints.toString(), size = 18.sp, weight = FontWeight.Bold)
            }
        }
    }
}
