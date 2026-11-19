package lv.zole.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import lv.zole.engine.AllPassMode
import lv.zole.engine.Difficulty

enum class ThemeSetting { LIGHT, BLACK, SYSTEM }

data class AppSettings(
    val theme: ThemeSetting = ThemeSetting.BLACK,
    val markTrumps: Boolean = true,
    val fastAnimations: Boolean = false,
    val eyeCounter: Boolean = true,
    val vibration: Boolean = true,
    val smallZole: Boolean = false,
    // Defaults for the "Jauna partija" screen.
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val allPassMode: AllPassMode = AllPassMode.TABLE,
    /** 0 means no limit. */
    val dealLimit: Int = 24,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val markTrumps = booleanPreferencesKey("mark_trumps")
        val fastAnimations = booleanPreferencesKey("fast_animations")
        val eyeCounter = booleanPreferencesKey("eye_counter")
        val vibration = booleanPreferencesKey("vibration")
        val smallZole = booleanPreferencesKey("small_zole")
        val difficulty = stringPreferencesKey("difficulty")
        val allPassMode = stringPreferencesKey("all_pass_mode")
        val dealLimit = intPreferencesKey("deal_limit")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        val d = AppSettings()
        AppSettings(
            theme = p[Keys.theme]?.let { runCatching { ThemeSetting.valueOf(it) }.getOrNull() } ?: d.theme,
            markTrumps = p[Keys.markTrumps] ?: d.markTrumps,
            fastAnimations = p[Keys.fastAnimations] ?: d.fastAnimations,
            eyeCounter = p[Keys.eyeCounter] ?: d.eyeCounter,
            vibration = p[Keys.vibration] ?: d.vibration,
            smallZole = p[Keys.smallZole] ?: d.smallZole,
            difficulty = p[Keys.difficulty]?.let { runCatching { Difficulty.valueOf(it) }.getOrNull() } ?: d.difficulty,
            allPassMode = p[Keys.allPassMode]?.let { runCatching { AllPassMode.valueOf(it) }.getOrNull() } ?: d.allPassMode,
            dealLimit = p[Keys.dealLimit] ?: d.dealLimit,
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { p ->
            val current = AppSettings(
                theme = p[Keys.theme]?.let { runCatching { ThemeSetting.valueOf(it) }.getOrNull() } ?: AppSettings().theme,
                markTrumps = p[Keys.markTrumps] ?: true,
                fastAnimations = p[Keys.fastAnimations] ?: false,
                eyeCounter = p[Keys.eyeCounter] ?: true,
                vibration = p[Keys.vibration] ?: true,
                smallZole = p[Keys.smallZole] ?: false,
                difficulty = p[Keys.difficulty]?.let { runCatching { Difficulty.valueOf(it) }.getOrNull() } ?: Difficulty.MEDIUM,
                allPassMode = p[Keys.allPassMode]?.let { runCatching { AllPassMode.valueOf(it) }.getOrNull() } ?: AllPassMode.TABLE,
                dealLimit = p[Keys.dealLimit] ?: 24,
            )
            val n = transform(current)
            p[Keys.theme] = n.theme.name
            p[Keys.markTrumps] = n.markTrumps
            p[Keys.fastAnimations] = n.fastAnimations
            p[Keys.eyeCounter] = n.eyeCounter
            p[Keys.vibration] = n.vibration
            p[Keys.smallZole] = n.smallZole
            p[Keys.difficulty] = n.difficulty.name
            p[Keys.allPassMode] = n.allPassMode.name
            p[Keys.dealLimit] = n.dealLimit
        }
    }
}
