import androidx.compose.runtime.mutableStateMapOf
import core.model.Character
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import net.model.genshin.GiCharacter
import okio.FileSystem
import java.io.File

object CharacterSync {

    val stats = mutableStateMapOf<Character, List<String>>()

    private val genshinApi = GenshinApi()

    val elements = genshinApi.elements

    private val rootDir = File(OS.getDataDir(), "mods")

    @OptIn(DelicateCoroutinesApi::class)
    fun sync(): Job = GlobalScope.launch(Dispatchers.IO) {

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

            val character =  Character(
                name = characterData.name,
                avatarUrl = genshinApi.avatarIconUrl(characterData.name),
                element = characterData.elementText,
            )

            println(genshinApi.avatarIconUrl(characterData.name))

            stats[character] = file.listFiles()?.mapNotNull { file -> file.name } ?: emptyList()
        }
    }
}