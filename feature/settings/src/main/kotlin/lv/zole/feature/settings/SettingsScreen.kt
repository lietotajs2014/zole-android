package lv.zole.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
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
import lv.zole.core.data.AppSettings
import lv.zole.core.data.SettingsRepository
import lv.zole.core.data.ThemeSetting
import lv.zole.core.ui.BackHeader
import lv.zole.core.ui.Divider
import lv.zole.core.ui.SectionLabel
import lv.zole.core.ui.Segmented
import lv.zole.core.ui.SurfaceCard
import lv.zole.core.ui.ZIcon
import lv.zole.core.ui.ZText
import lv.zole.core.ui.Zole
import lv.zole.core.ui.ZoleIcons
import lv.zole.core.ui.ZoleSwitch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val settings: StateFlow<AppSettings> = repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repository.update(transform) }
    }
}

@Composable
fun SettingsRoute(viewModel: SettingsViewModel, versionName: String, onBack: () -> Unit, onScoringTable: () -> Unit) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    SettingsScreen(s, versionName, onBack, onScoringTable) { viewModel.update(it) }
}

@Composable
fun SettingsScreen(
    s: AppSettings,
    versionName: String,
    onBack: () -> Unit,
    onScoringTable: () -> Unit,
    update: ((AppSettings) -> AppSettings) -> Unit,
) {
    val c = Zole.colors
    Column(Modifier.fillMaxSize().background(c.background).systemBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
        BackHeader("Iestatījumi", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 22.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Section("Izskats") {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ZText("Tēma")
                    val order = listOf(ThemeSetting.LIGHT, ThemeSetting.BLACK, ThemeSetting.SYSTEM)
                    Segmented(
                        listOf("Gaiša", "Melna", "Sistēmas"), order.indexOf(s.theme), { i -> update { it.copy(theme = order[i]) } },
                        height = 36.dp, outerRadius = 10.dp, fontSize = 14.sp,
                    )
                    ZText(
                        "Melna (OLED) ir noklusējums. “Sistēmas” seko telefona gaišajam vai tumšajam režīmam.",
                        size = 13.sp, color = c.textSecondary, lineHeight = 18.sp,
                    )
                }
                Divider()
                ToggleRow("Iezīmēt trumpjus", "Mazs punkts kārts stūrī", s.markTrumps) { v -> update { it.copy(markTrumps = v) } }
                Divider()
                ToggleRow("Ātrās animācijas", null, s.fastAnimations) { v -> update { it.copy(fastAnimations = v) } }
            }
            Section("Spēle") {
                ToggleRow("Acu skaitītājs uz galda", null, s.eyeCounter) { v -> update { it.copy(eyeCounter = v) } }
                Divider()
                ToggleRow("Vibrācija", null, s.vibration) { v -> update { it.copy(vibration = v) } }
            }
            Section("Noteikumi") {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onScoringTable).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ZText("Punktu tabula", Modifier.weight(1f))
                    ZText("Klasiskā", color = c.textSecondary)
                    ZIcon(ZoleIcons.Chevron, null, Modifier.padding(start = 6.dp), tint = c.textSecondary, size = 16.dp)
                }
                Divider()
                ToggleRow("Mazā zole", "Nepaņemt nevienu stiķi", s.smallZole) { v -> update { it.copy(smallZole = v) } }
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
