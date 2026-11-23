package lv.zole.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import lv.zole.engine.AllPassMode
import lv.zole.engine.Difficulty
import lv.zole.engine.ScoringVariant

enum class ThemeSetting { LIGHT, BLACK, SYSTEM }

enum class LanguageSetting { LV, EN }

/** Animations whose speed can be tuned separately in debug mode. */
enum class AnimKind {
    /** Cards flying into the hand at the start of a deal. */
    DEAL,
    /** A card lifting when touched or selected. */
    LIFT,
    /** A played card flying onto the table. */
    PLAY,
    /** How long a finished trick stays on the table. */
    TRICK_HOLD,
    /** The finished trick sweeping over to the winner. */
    TRICK_COLLECT,
    /** Bots' pause before acting. */
    BOT_THINK,
}

data class AppSettings(
    val theme: ThemeSetting = ThemeSetting.BLACK,
    val language: LanguageSetting = LanguageSetting.LV,
    val markTrumps: Boolean = true,
    val fastAnimations: Boolean = false,
    val eyeCounter: Boolean = true,
    val vibration: Boolean = true,
    val smallZole: Boolean = false,
    val scoring: ScoringVariant = ScoringVariant.CLASSIC,
    // Defaults for the "Jauna partija" screen.
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val allPassMode: AllPassMode = AllPassMode.TABLE,
    /** 0 means no limit. */
    val dealLimit: Int = 24,
    // Debug mode: manual tweaks for testing. The values below only apply while [debugMode] is on.
    val debugMode: Boolean = false,
    /** Per-animation speed multipliers (0.1-2.0, 2 = twice as fast); missing = 1. */
    val debugAnimSpeeds: Map<AnimKind, Float> = emptyMap(),
    /** Turn time limit in seconds; 0 = no timer. */
    val debugTimerSeconds: Int = DEFAULT_TURN_SECONDS,
    /** Show the bots' hands (and the talon) face up. The bots still only see their own cards. */
    val debugShowBotHands: Boolean = false,
) {
    /** Speed multiplier of one animation (1 unless tuned in debug mode). */
    fun animSpeed(kind: AnimKind): Float = if (debugMode) debugAnimSpeeds[kind] ?: 1f else 1f

    /** Multiplier for one animation's duration (smaller = faster). */
    fun animScale(kind: AnimKind): Float = (if (fastAnimations) 0.5f else 1f) / animSpeed(kind).coerceAtLeast(0.1f)

    /** Human turn time limit in ms; 0 means no limit. */
    val turnTimerMs: Long get() = (if (debugMode) debugTimerSeconds else DEFAULT_TURN_SECONDS) * 1000L

    val showBotHands: Boolean get() = debugMode && debugShowBotHands

    companion object {
        const val DEFAULT_TURN_SECONDS = 15
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val language = stringPreferencesKey("language")
        val markTrumps = booleanPreferencesKey("mark_trumps")
        val fastAnimations = booleanPreferencesKey("fast_animations")
        val eyeCounter = booleanPreferencesKey("eye_counter")
        val vibration = booleanPreferencesKey("vibration")
        val smallZole = booleanPreferencesKey("small_zole")
        val scoring = stringPreferencesKey("scoring")
        val difficulty = stringPreferencesKey("difficulty")
        val allPassMode = stringPreferencesKey("all_pass_mode")
        val dealLimit = intPreferencesKey("deal_limit")
        val debugMode = booleanPreferencesKey("debug_mode")
        fun animSpeed(kind: AnimKind) = floatPreferencesKey("debug_anim_${kind.name.lowercase()}")
        val debugTimerSeconds = intPreferencesKey("debug_timer_seconds")
        val debugShowBotHands = booleanPreferencesKey("debug_show_bot_hands")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { read(it) }

    private fun read(p: Preferences): AppSettings {
        val d = AppSettings()
        return AppSettings(
            theme = p[Keys.theme]?.let { runCatching { ThemeSetting.valueOf(it) }.getOrNull() } ?: d.theme,
            language = p[Keys.language]?.let { runCatching { LanguageSetting.valueOf(it) }.getOrNull() } ?: d.language,
            markTrumps = p[Keys.markTrumps] ?: d.markTrumps,
            fastAnimations = p[Keys.fastAnimations] ?: d.fastAnimations,
            eyeCounter = p[Keys.eyeCounter] ?: d.eyeCounter,
            vibration = p[Keys.vibration] ?: d.vibration,
            smallZole = p[Keys.smallZole] ?: d.smallZole,
            scoring = p[Keys.scoring]?.let { runCatching { ScoringVariant.valueOf(it) }.getOrNull() } ?: d.scoring,
            difficulty = p[Keys.difficulty]?.let { runCatching { Difficulty.valueOf(it) }.getOrNull() } ?: d.difficulty,
            allPassMode = p[Keys.allPassMode]?.let { runCatching { AllPassMode.valueOf(it) }.getOrNull() } ?: d.allPassMode,
            dealLimit = p[Keys.dealLimit] ?: d.dealLimit,
            debugMode = p[Keys.debugMode] ?: d.debugMode,
            debugAnimSpeeds = AnimKind.entries.mapNotNull { k -> p[Keys.animSpeed(k)]?.let { k to it } }.toMap(),
            debugTimerSeconds = p[Keys.debugTimerSeconds] ?: d.debugTimerSeconds,
            debugShowBotHands = p[Keys.debugShowBotHands] ?: d.debugShowBotHands,
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { p ->
            val n = transform(read(p))
            p[Keys.theme] = n.theme.name
            p[Keys.language] = n.language.name
            p[Keys.markTrumps] = n.markTrumps
            p[Keys.fastAnimations] = n.fastAnimations
            p[Keys.eyeCounter] = n.eyeCounter
            p[Keys.vibration] = n.vibration
            p[Keys.smallZole] = n.smallZole
            p[Keys.scoring] = n.scoring.name
            p[Keys.difficulty] = n.difficulty.name
            p[Keys.allPassMode] = n.allPassMode.name
            p[Keys.dealLimit] = n.dealLimit
            p[Keys.debugMode] = n.debugMode
            AnimKind.entries.forEach { k -> p[Keys.animSpeed(k)] = n.debugAnimSpeeds[k] ?: 1f }
            p[Keys.debugTimerSeconds] = n.debugTimerSeconds
            p[Keys.debugShowBotHands] = n.debugShowBotHands
        }
    }
}
