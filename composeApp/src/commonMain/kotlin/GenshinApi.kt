import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import net.model.genshin.GiCharacter

class GenshinApi {

    suspend fun characterList(): List<String> {
        val json = GET<JsonArray>("https://api.github.com/repos/theBowja/genshin-db/contents/src/data/English/characters")
        return List(json.size) {
            json[it].jsonObject["name"]!!.toString()
        }
    }

    val elements = listOf("Anemo", "Cryo", "Dendro", "Electro", "Geo", "Hydro", "Pyro")

    suspend fun elementList(): List<String> {
        val json = GET<JsonArray>("https://api.github.com/repos/theBowja/genshin-db/contents/src/data/English/elements")
        return List(json.size) {
            json[it].jsonObject["name"]!!.toString()
        }
    }

    fun avatarIconUrl(name: String): String {

        val avatar: (String, String) -> String = { folder, path ->
             "https://raw.githubusercontent.com/frzyc/genshin-optimizer/master/libs/gi/assets/src/gen/chars/$folder/UI_AvatarIcon_$path.png"
        }

        return when (name.lowercase()) {
            "aether" -> avatar("TravelerM", "PlayerBoy")
            "lumine" -> avatar("TravelerF", "PlayerGirl")
            "shikanoin heizou" -> avatar("ShikanoinHeizou", "Heizo")
            "raiden shogun" -> avatar("RaidenShogun", "Shougun")
            "yae miko" -> avatar("YaeMiko", "Yae")
            "yun jin" -> avatar("YunJin", "Yunjin")
            "yanfei" -> avatar("Yanfei", "Feiyan")
            "jean" -> avatar("Jean", "Qin")
            "lyney" -> avatar("Lyney", "Liney")
            "xianyun" -> avatar("Xianyun", "Liuyun")
            "thoma" -> avatar("Thoma", "Tohma")
            "hu tao" -> avatar("HuTao", "Hutao")
            "kirara" -> avatar("Kirara", "Momoka")
            "baizhu" -> avatar("Baizhu", "Baizhuer")
            "alhaitham" -> avatar("Alhaitham", "Alhatham")
            "amber" -> avatar("Amber", "Ambor")
            "lynette" -> avatar("Lynette", "Linette")
            "noelle" -> avatar("Noelle", "Noel")
            else -> {
                val split = name.split(' ').last()

                val sanitized = split.first().uppercase() + split.drop(1).filter { !it.isWhitespace() }.lowercase()
                val folder = name.filter { !it.isWhitespace() }

                avatar(folder, sanitized)
            }
        }
    }

    suspend fun characterData(path: String): GiCharacter {
        return GET("https://raw.githubusercontent.com/theBowja/genshin-db/main/src/data/English/characters/$path")
    }
}