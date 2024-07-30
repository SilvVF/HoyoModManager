package core.api

import core.db.DB
import core.model.Character
import core.model.Game
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import net.GET
import net.model.genshin.GiCharacter
import java.io.File
import javax.xml.crypto.Data


object GenshinApi: DataApi {

    override val game: Game = Game.Genshin

    override suspend fun characterList(): List<String> {
        val json = GET<JsonArray>("https://api.github.com/repos/theBowja/genshin-db/contents/src/data/English/characters")
        return List(json.size) {
            json[it].jsonObject["name"]!!.toString()
        }
    }

    override val elements = listOf("Anemo", "Cryo", "Dendro", "Electro", "Geo", "Hydro", "Pyro")

    override suspend fun elementList(): List<String> {
        val json = GET<JsonArray>("https://api.github.com/repos/theBowja/genshin-db/contents/src/data/English/elements")
        return List(json.size) {
            json[it].jsonObject["name"]!!.toString()
        }
    }

    override fun avatarIconUrl(name: String): String {

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

    override suspend fun characterData(path: String): Character {
        val res: GiCharacter = GET("https://raw.githubusercontent.com/theBowja/genshin-db/main/src/data/English/characters/$path")
        return Character(
            id = res.id,
            name = res.name,
            avatarUrl = avatarIconUrl(res.name),
            game = game,
            element = res.elementText,
        )
    }
}