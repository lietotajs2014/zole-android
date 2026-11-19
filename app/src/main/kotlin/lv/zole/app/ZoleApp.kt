package lv.zole.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import lv.zole.core.data.HistoryDatabase
import lv.zole.core.data.MatchRepository
import lv.zole.core.data.SettingsRepository

/** Manual dependency container: the app is small enough not to need Hilt. */
class AppContainer(app: Application) {
    val scope = CoroutineScope(SupervisorJob())
    val database: HistoryDatabase by lazy { HistoryDatabase.create(app) }
    val settings = SettingsRepository(app)
    val matches: MatchRepository by lazy { MatchRepository(app, database.history(), scope) }
}

class ZoleApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.scope.launch { container.matches.load() }
    }
}
