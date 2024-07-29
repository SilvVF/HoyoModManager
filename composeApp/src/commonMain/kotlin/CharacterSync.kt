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

    val rootDir = File(OS.getDataDir(), "mods")

    @OptIn(DelicateCoroutinesApi::class)
    fun sync(dataApi: DataApi): Job = GlobalScope.launch(Dispatchers.IO) {

        DB.modDao.clear(dataApi.game.data)

        val newStats = mutableMapOf<Character, List<String>>()
        val characters = dataApi.characterList()

        val gameDir = File(rootDir, dataApi.game.name)

        if (!gameDir.exists()) {
            gameDir.mkdirs()
        }

        for (c in characters) {

            val character = dataApi.characterData(c.removeSurrounding("\""))

            val file = File(gameDir, character.name)

            if (!file.exists()) {
                file.mkdir()
            }

            val modDirFiles = run {
                val path = DB.prefsDao.select()?.exportModDir?.get(dataApi.game.data) ?: return@run emptyList<File>()
                File(path).listFiles()?.toList() ?: emptyList()
            }

            newStats[character] = file.listFiles()
                ?.onEach { updateMod(it, character, modDirFiles) }
                ?.mapNotNull { it.name }
                ?: emptyList()
        }

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