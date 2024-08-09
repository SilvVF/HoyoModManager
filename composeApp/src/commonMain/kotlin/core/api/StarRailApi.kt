package core.api

import core.model.Character
import core.model.Game
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.GET


object StarRailApi: DataApi {

    override val skinCategoryId: Int = 22832

    override val game: Game = Game.StarRail

    override val elements: List<String> =
        listOf("Ice",  "Physical", "Fire", "Lightning", "Wind", "Quantum", "Imaginary")

    override suspend fun elementList(): List<String> {
        val json = GET<JsonObject>("https://raw.githubusercontent.com/Mar-7th/StarRailRes/master/index_new/en/elements.json")
        return json.keys.toList().filter { it != "Thunder" }
    }

    override suspend fun characterList(): List<Character> {
        val json = GET<JsonObject>("https://raw.githubusercontent.com/Mar-7th/StarRailRes/master/index_new/en/characters.json")
        val icon = { path: String ->
            "https://raw.githubusercontent.com/Mar-7th/StarRailRes/master/$path"
        }
        val characters = ArrayList<Character>(json.keys.size)

        for ((id, jsonElement) in json.entries) {
            val obj = jsonElement.jsonObject
            val character = Character(
                id = id.toInt(),
                game = this.game,
                name = obj["name"]?.jsonPrimitive?.content?.takeIf { it != "{NICKNAME}" } ?: continue,
                avatarUrl =  obj["icon"]?.jsonPrimitive?.content?.let(icon) ?: continue,
                element = obj["element"]?.jsonPrimitive?.content?.run { if (this == "Thunder") "Lightning" else this } ?: continue
            )
            characters.add(character)
        }
        return characters.toList()
    }
}