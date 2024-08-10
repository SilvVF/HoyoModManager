package core.db

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import core.db.prefs.DatastorePreferenceStore
import core.db.prefs.getEnum
import core.model.Game
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.pathString

fun createDataStore(producePath: () -> okio.Path): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath() }
    )

private const val dataStoreFileName = "hmm.preferences_pb"

object Prefs {

    private val store by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        DatastorePreferenceStore(
            createDataStore {
                Paths.get(
                    OS.getCacheDir().path,
                    dataStoreFileName
                ).toOkioPath()
            }
        )
    }

    private const val APP_PREF = "hmm_pref:"

    fun genshinDir() = store.getString("${APP_PREF}genshin_dir")

    fun starRailDir() = store.getString("${APP_PREF}star_rail_dir")

    fun zenlessDir() = store.getString("${APP_PREF}zenless_dir")

    fun wuwaDir() = store.getString("${APP_PREF}wuwa_dir")

    fun ignoreOnGeneration() = store.getStringSet("${APP_PREF}ignore_on_generation")

    fun lastModScreen() = store.getEnum("${APP_PREF}last_mod_screen", Game.Genshin)
}
