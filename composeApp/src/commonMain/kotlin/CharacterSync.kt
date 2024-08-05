import core.api.DataApi
import core.db.AppDatabase
import core.model.Mod
import core.model.Character
import core.model.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

object CharacterSync {

    val database = AppDatabase.instance

    val running = mutableMapOf<Game, Job>()
    val initialSyncDone = mutableSetOf<Game>()

    val rootDir = File(OS.getDataDir(), "mods")

    fun sync(
        dataApi: DataApi,
        fromNetwork: Boolean = false,
    ): Job = GlobalScope.launch(Dispatchers.IO) {

        val newStats = mutableMapOf<Character, List<String>>()
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
                val path = database.selectMetaData()?.exportModDir?.get(dataApi.game.data) ?: return@run emptyList<File>()
                File(path).listFiles()?.toList() ?: emptyList()
            }

            val files = file.listFiles() ?: return@launch
            newStats[c] = files.onEach { file ->
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