import core.api.DataApi
import core.api.GenshinApi
import core.db.DB
import core.db.ModEntity
import core.model.Character
import core.model.Game
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

object CharacterSync {

    val stats = MutableStateFlow<Map<Game, Map<Character, List<String>>>>(emptyMap())

    private val modDao = DB.modDao
    private val characterDao = DB.characterDao

    val rootDir = File(OS.getDataDir(), "mods")

    fun sync(
        dataApi: DataApi,
        fromNetwork: Boolean = false,
    ): Job = GlobalScope.launch(Dispatchers.IO) {

        val newStats = mutableMapOf<Character, List<String>>()
        val seenMods = mutableSetOf<String>()

        val fetchFromNetwork = suspend {
            dataApi.characterList()
                .map { name ->
                    dataApi.characterData(name.removeSurrounding("\""))
                        .also { characterDao.insert(it) }
                }
        }

        val characters = if (fromNetwork) {
            fetchFromNetwork()
        } else {
            characterDao.selectByGame(dataApi.game).ifEmpty { fetchFromNetwork() }
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
                val path = DB.prefsDao.select()?.exportModDir?.get(dataApi.game.data) ?: return@run emptyList<File>()
                File(path).listFiles()?.toList() ?: emptyList()
            }

            val files = file.listFiles() ?: return@launch
            newStats[c] = files.onEach { file ->
                updateMod(file, c, modDirFiles)
                seenMods.add(file.name)
            }
                .mapNotNull { file -> file.name }

        }

        modDao.deleteUnused(used = seenMods.toList(), game = dataApi.game.data)

        stats.update { stats ->
            stats.toMutableMap().apply {
                this[dataApi.game] = newStats
            }
                .toMap()
        }
    }


    private suspend fun updateMod(file: File, character: Character, modDirFiles: List<File>) {
        modDao.insert(
            ModEntity(
                id = character.id,
                game = character.game.data,
                character = character.name,
                fileName = file.name,
                enabled = modDirFiles.map { it.name }.contains(file.name)
            )
        )
    }
}