package lv.zole.core.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import lv.zole.engine.MatchAction
import lv.zole.engine.MatchConfig
import lv.zole.engine.MatchEngine
import lv.zole.engine.MatchSnapshot
import lv.zole.engine.MatchState
import lv.zole.engine.Seat
import java.io.File
import kotlin.random.Random

/**
 * Holds the active match. After every action it saves the seed and the action list, so the app can be
 * killed at any moment and resume exactly where it was. Finished deals also go to the history database.
 */
class MatchRepository(
    context: Context,
    private val dao: HistoryDao,
    private val scope: CoroutineScope,
) {
    private val file = File(context.filesDir, "active_match.json")
    private val tmp = File(context.filesDir, "active_match.json.tmp")
    private val json = Json { ignoreUnknownKeys = true }
    private val saveLock = Mutex()

    private val _state = MutableStateFlow<MatchState?>(null)
    val state: StateFlow<MatchState?> = _state.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private var snapshot: MatchSnapshot? = null
    private var matchId: String = ""
    private var startedAt: Long = 0

    suspend fun load() {
        if (_loaded.value) return
        withContext(Dispatchers.IO) {
            runCatching {
                if (file.exists()) {
                    val stored = json.decodeFromString(StoredMatch.serializer(), file.readText())
                    val state = MatchEngine.replay(stored.snapshot)
                    snapshot = stored.snapshot
                    matchId = stored.id
                    startedAt = stored.startedAt
                    _state.value = state
                }
            }.onFailure { file.delete() }
        }
        _loaded.value = true
    }

    /** True when there is a match that can be continued. */
    val hasActiveMatch: Boolean get() = _state.value?.let { !it.isOver } ?: false

    fun start(config: MatchConfig): MatchState {
        val seed = Random.nextLong()
        val firstDealer = Seat(2) // so that "Tu" is the first hand in deal 1
        startedAt = System.currentTimeMillis()
        matchId = "m$startedAt"
        snapshot = MatchSnapshot(config, seed, firstDealer, emptyList())
        val state = MatchEngine.start(config, seed, firstDealer)
        _state.value = state
        save()
        scope.launch(Dispatchers.IO) {
            dao.upsertMatch(MatchEntity(matchId, startedAt, null, config.difficulty.name, 0, 0, 0, 0))
        }
        return state
    }

    /** Applies an action to the active match. Throws if it is illegal. */
    fun apply(action: MatchAction): MatchState {
        val current = checkNotNull(_state.value) { "No active match" }
        val next = MatchEngine.reduce(current, action)
        snapshot = snapshot?.let { it.copy(actions = it.actions + action) }
        _state.value = next
        save()
        if (next.history.size > current.history.size) recordDeal(next)
        return next
    }

    fun abandon() {
        _state.value = null
        snapshot = null
        scope.launch(Dispatchers.IO) { saveLock.withLock { file.delete() } }
    }

    private fun save() {
        val snap = snapshot ?: return
        val stored = StoredMatch(matchId, startedAt, snap)
        scope.launch(Dispatchers.IO) {
            saveLock.withLock {
                tmp.writeText(json.encodeToString(StoredMatch.serializer(), stored))
                if (!tmp.renameTo(file)) {
                    file.delete()
                    tmp.renameTo(file)
                }
            }
        }
    }

    private fun recordDeal(state: MatchState) {
        val record = state.history.last()
        val id = matchId
        val begun = startedAt
        scope.launch(Dispatchers.IO) {
            if (dao.dealCount(id, record.number) == 0) {
                dao.insertDeal(
                    DealEntity(
                        matchId = id,
                        number = record.number,
                        playedAt = System.currentTimeMillis(),
                        contract = record.result.contract?.name,
                        declarer = record.result.declarer?.index,
                        outcome = record.result.outcome.name,
                        declarerPoints = record.result.declarerPoints,
                        delta0 = record.deltas.getValue(Seat(0)),
                        delta1 = record.deltas.getValue(Seat(1)),
                        delta2 = record.deltas.getValue(Seat(2)),
                    ),
                )
            }
            dao.upsertMatch(
                MatchEntity(
                    id = id,
                    startedAt = begun,
                    endedAt = if (state.isOver) System.currentTimeMillis() else null,
                    difficulty = state.config.difficulty.name,
                    deals = state.history.size,
                    total0 = state.totals.getValue(Seat(0)),
                    total1 = state.totals.getValue(Seat(1)),
                    total2 = state.totals.getValue(Seat(2)),
                ),
            )
        }
    }
}

@kotlinx.serialization.Serializable
private data class StoredMatch(val id: String, val startedAt: Long, val snapshot: MatchSnapshot)
