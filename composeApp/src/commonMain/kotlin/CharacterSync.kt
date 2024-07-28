import androidx.compose.runtime.mutableStateMapOf
import core.db.DB
import core.db.ModEntity
import core.model.Character
import core.model.Game
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import net.model.genshin.GiCharacter
import okio.FileSystem
import java.io.File

object CharacterSync {

    val stats = MutableStateFlow(emptyMap<Character, List<String>>())

    private val genshinApi = GenshinApi()
    private val modDao = DB.modDao

    val elements = genshinApi.elements

    val rootDir = File(OS.getDataDir(), "mods")

    @OptIn(DelicateCoroutinesApi::class)
    fun sync(): Job = GlobalScope.launch(Dispatchers.IO) {

        val newStats = mutableMapOf<Character, List<String>>()

        val characters = genshinApi.characterList()

        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }

        for (c in characters) {
            val characterData = genshinApi.characterData(c.removeSurrounding("\""))

            val file = File(rootDir, characterData.name)

            if (!file.exists()) {
                file.mkdir()
            }

            val character = Character(
                id = characterData.id,
                name = characterData.name,
                avatarUrl = genshinApi.avatarIconUrl(characterData.name),
                game = Game.fromByte(DB.GENSHIN),
                element = characterData.elementText,
            )

            newStats[character] = file.listFiles()
                ?.onEach { updateMod(file, character) }
                ?.mapNotNull { file -> file.name }
                ?: emptyList()
        }

        stats.emit(newStats)
    }

    private suspend fun updateMod(file: File, character: Character) {
        val mod = modDao.selectByFileName(file.path)
        if (mod != null) {
            modDao.update(
                id = mod.id,
                character = character.name,
                fileName = file.path,
                enabled = null
            )
        } else {
            modDao.insert(
                ModEntity(
                    id = character.id,
                    game = DB.GENSHIN,
                    character = character.name,
                    fileName = file.name,
                    enabled = false
                )
            )
        }
    }
}