package lv.zole.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lv.zole.core.data.AnimKind
import lv.zole.core.data.AppSettings
import lv.zole.core.data.LanguageSetting
import lv.zole.core.data.SettingsRepository
import lv.zole.core.data.ThemeSetting
import lv.zole.core.ui.BackHeader
import lv.zole.core.ui.Divider
import lv.zole.core.ui.SectionLabel
import lv.zole.core.ui.Segmented
import lv.zole.core.ui.SurfaceCard
import lv.zole.core.ui.ZText
import lv.zole.core.ui.Zole
import lv.zole.core.ui.ZoleSwitch
import lv.zole.engine.ScoringVariant

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val settings: StateFlow<AppSettings> = repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repository.update(transform) }
    }
}

@Composable
fun SettingsRoute(viewModel: SettingsViewModel, versionName: String, onBack: () -> Unit) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    SettingsScreen(s, versionName, onBack) { viewModel.update(it) }
}

@Composable
fun SettingsScreen(
    s: AppSettings,
    versionName: String,
    onBack: () -> Unit,
    update: ((AppSettings) -> AppSettings) -> Unit,
) {
    val c = Zole.colors
    val t = Zole.strings
    Column(Modifier.fillMaxSize().background(c.background).systemBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
        BackHeader(t.settings, onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 22.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Section(t.appearance) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ZText(t.theme)
                    val order = listOf(ThemeSetting.LIGHT, ThemeSetting.BLACK, ThemeSetting.SYSTEM)
                    Segmented(
                        t.themes, order.indexOf(s.theme), { i -> update { it.copy(theme = order[i]) } },
                        height = 36.dp, outerRadius = 10.dp, fontSize = 14.sp,
                    )
                    ZText(t.themeHint, size = 13.sp, color = c.textSecondary, lineHeight = 18.sp)
                }
                Divider()
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ZText(t.language)
                    Segmented(
                        listOf("Latviešu", "English"), s.language.ordinal, { i -> update { it.copy(language = LanguageSetting.entries[i]) } },
                        height = 36.dp, outerRadius = 10.dp, fontSize = 14.sp,
                    )
                }
                Divider()
                ToggleRow(t.markTrumps, t.markTrumpsHint, s.markTrumps) { v -> update { it.copy(markTrumps = v) } }
                Divider()
                ToggleRow(t.fastAnimations, null, s.fastAnimations) { v -> update { it.copy(fastAnimations = v) } }
                Divider()
                ToggleRow(t.vibration, null, s.vibration) { v -> update { it.copy(vibration = v) } }
            }
            Section(t.game) {
                ToggleRow(t.eyeCounter, null, s.eyeCounter) { v -> update { it.copy(eyeCounter = v) } }
            }
            Section(t.gameRules) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ZText(t.scoringTable)
                    Segmented(
                        ScoringVariant.entries.map { t.scoringName(it) }, s.scoring.ordinal,
                        { i -> update { it.copy(scoring = ScoringVariant.entries[i]) } },
                        height = 36.dp, outerRadius = 10.dp, fontSize = 14.sp,
                    )
                    ZText(t.scoringHint(s.scoring), size = 13.sp, color = c.textSecondary, lineHeight = 18.sp)
                }
                Divider()
                ToggleRow(t.smallZole, t.smallZoleHint, s.smallZole) { v -> update { it.copy(smallZole = v) } }
            }
            Section(t.debug) {
                ToggleRow(t.debugMode, t.debugModeHint, s.debugMode) { v -> update { it.copy(debugMode = v) } }
                if (s.debugMode) {
                    Divider()
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ZText(t.animSpeed, Modifier.weight(1f))
                            if (s.debugAnimSpeeds.isNotEmpty()) {
                                ZText(
                                    t.reset,
                                    Modifier.clickable(role = Role.Button) { update { it.copy(debugAnimSpeeds = emptyMap()) } }.padding(vertical = 4.dp),
                                    size = 14.sp, color = c.accent,
                                )
                            }
                        }
                        AnimKind.entries.forEach { kind ->
                            SpeedSlider(t.animNames[kind.ordinal], s.debugAnimSpeeds[kind] ?: 1f) { v ->
                                update { it.copy(debugAnimSpeeds = it.debugAnimSpeeds + (kind to v)) }
                            }
                        }
                    }
                    Divider()
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ZText(t.turnTimer)
                        val seconds = listOf(0, 5, 15, 30, 60)
                        Segmented(
                            seconds.map { if (it == 0) t.timerOff else "${it}s" }, seconds.indexOf(s.debugTimerSeconds).coerceAtLeast(0),
                            { i -> update { it.copy(debugTimerSeconds = seconds[i]) } },
                            height = 36.dp, outerRadius = 10.dp, fontSize = 14.sp,
                        )
                    }
                    Divider()
                    ToggleRow(t.showBotHands, t.showBotHandsHint, s.debugShowBotHands) { v -> update { it.copy(debugShowBotHands = v) } }
                }
            }
        }
        ZText("Zole V$versionName", Modifier.fillMaxWidth().padding(top = 16.dp), size = 13.sp, color = c.textSecondary, align = TextAlign.Center)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(title, Modifier.padding(horizontal = 4.dp))
        SurfaceCard { content() }
    }
}

/** Debug speed slider: 0.1x-2.0x in 0.1 steps. Saves when the finger lifts. */
@Composable
private fun SpeedSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    val c = Zole.colors
    var current by remember(value) { mutableFloatStateOf(value) }
    Column {
        Row(Modifier.padding(top = 6.dp)) {
            ZText(label, Modifier.weight(1f), size = 14.sp, color = c.textSecondary)
            ZText("%.1f×".format(Locale.US, current), size = 14.sp, weight = FontWeight.SemiBold)
        }
        Slider(
            value = current,
            onValueChange = { current = (it * 10).roundToInt() / 10f },
            onValueChangeFinished = { onChange(current) },
            valueRange = 0.1f..2f,
            steps = 18,
            colors = SliderDefaults.colors(
                thumbColor = c.accent,
                activeTrackColor = c.accent,
                inactiveTrackColor = c.toggleOff,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ZText(title, weight = FontWeight.Normal)
            if (subtitle != null) ZText(subtitle, size = 13.sp, color = Zole.colors.textSecondary)
        }
        ZoleSwitch(checked, onChange, title)
    }
}
