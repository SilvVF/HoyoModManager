package core.api

import core.model.Character
import core.model.Game
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import net.NetHelper
import org.jsoup.Jsoup
import org.jsoup.select.Elements

object ZZZApi: DataApi {

    private val prydwenUrl = "https://www.prydwen.gg"
    override val game: Game = Game.ZZZ

    override val elements: List<String> = listOf("Electric", "Ether", "Fire", "Ice", "Physical")

    override suspend fun elementList(): List<String> = listOf("Electric", "Ether", "Fire", "Ice", "Physical")

    override suspend fun characterList(): List<Character> {
        val doc = Jsoup.parse(
            NetHelper.client.get("$prydwenUrl/zenless/characters").bodyAsText()
        )

        return doc.getElementsByClass("avatar-card").mapNotNull { element ->
            runCatching {
                val name = element.getElementsByClass("emp-name").text()
                val iconUrl = element.getElementsByClass("gatsby-image-wrapper").select("img[data-src]").first().attr("data-src")


                println(iconUrl)
                val type = element.getElementsByClass("element").firstNotNullOf { element ->
                    element.select("picture img").firstNotNullOf { img -> img.attr("alt") }
                }
                Character(
                    id = name.hashCode(),
                    game = Game.ZZZ,
                    name = name,
                    avatarUrl = prydwenUrl + iconUrl,
                    element = type
                )
            }

                .getOrNull()
         }
    }
}