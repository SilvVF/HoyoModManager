import core.api.DataApi
import core.db.AppDatabase
import core.db.Prefs
import core.model.Mod
import core.model.Character
import core.model.Game
import core.model.Game.Genshin
import core.model.Game.StarRail
import core.model.Game.ZZZ
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

object Sync {

    sealed interface Request {
        data object Startup: Request
        data class UserInitiated(val network: Boolean): Request
    }

    private val database = AppDatabase.instance

    private val running = mutableMapOf<Game, Job>()
    private val initialSyncDone = mutableSetOf<Game>()

    val rootDir = File(OS.getDataDir(), "mods")

    fun sync(dataApi: DataApi, request: Request): Job? {
        val initialAlreadyComplete =  request is Request.Startup && initialSyncDone.contains(dataApi.game)
        return when {
            initialAlreadyComplete -> null
            running[dataApi.game]?.isActive == true -> running[dataApi.game]
            else -> internalSync(
                dataApi,
                when (request) {
                    Request.Startup -> false
                    is Request.UserInitiated -> true
                }
            )
                .also { job ->
                    running[dataApi.game] = job
                    job.invokeOnCompletion { running.remove(dataApi.game) }
                }
        }
    }

    private fun internalSync(
        dataApi: DataApi,
        fromNetwork: Boolean = false,
    ): Job = GlobalScope.launch(Dispatchers.IO) {

        val seenMods = mutableSetOf<String>()

        val fetchFromNetwork = suspend {
            dataApi.characterList().also {
                database.updateFromCharacters(it)
            }
        }

        val characters = if (fromNetwork) {
            fetchFromNetwork()
        } else {
            database.selectByGame(dataApi.game).ifEmpty { fetchFromNetwork() }
        }

        val gameDir = File(rootDir, dataApi.game.name)

        if (!gameDir.exists()) {
            gameDir.mkdirs()
        }

        for (c in characters) {

            val file = File(gameDir, c.name)

            if (!file.exists()) {
                file.mkdir()
            }

            val modDirFiles = run {
                val path = when (dataApi.game) {
                    Genshin -> Prefs.genshinDir()
                    StarRail -> Prefs.starRailDir()
                    ZZZ -> Prefs.zenlessDir()
                    Game.Wuwa -> Prefs.wuwaDir()
                }.get()
                File(path).listFiles()?.toList() ?: emptyList()
            }

            val files = file.listFiles() ?: return@launch

            files.onEach { file ->
                updateMod(file, c, modDirFiles)
                seenMods.add(file.name)
            }
                .mapNotNull { file -> file.name }

        }

        database.deleteUnusedMods(used = seenMods.toList(), game = dataApi.game.data)
    }


    private suspend fun updateMod(file: File, character: Character, modDirFiles: List<File>) {
        database.insertOrUpdate(
            Mod(
                characterId = character.id,
                game = character.game.data,
                character = character.name,
                fileName = file.name,
                enabled = modDirFiles.map { it.name }.contains(file.name)
            )
        )
    }
}