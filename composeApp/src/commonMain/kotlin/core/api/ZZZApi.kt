package core.api

import core.model.Character
import core.model.Game
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import net.NetHelper
import org.jsoup.Jsoup

object ZZZApi: DataApi {

    override val skinCategoryId: Int = 30305

    private const val BASE_URL = "https://www.prydwen.gg"

    override val game: Game = Game.ZZZ

    override val elements: List<String> = listOf("Electric", "Ether", "Fire", "Ice", "Physical")

    override suspend fun elementList(): List<String> = listOf("Electric", "Ether", "Fire", "Ice", "Physical")

    override suspend fun characterList(): List<Character> {
        val doc = Jsoup.parse(
            NetHelper.client.get("$BASE_URL/zenless/characters").bodyAsText()
        )

        return doc.getElementsByClass("avatar-card").mapNotNull { element ->
            runCatching {
                val name = element.getElementsByClass("emp-name").text()
                val iconUrl = element.getElementsByClass("gatsby-image-wrapper")
                    .select("img[data-src]")
                    .first()
                    .attr("data-src")

                val type = element.getElementsByClass("element").firstNotNullOf { element ->
                    element.select("picture img").firstNotNullOf { img -> img.attr("alt") }
                }
                Character(
                    id = name.hashCode(),
                    game = Game.ZZZ,
                    name = name,
                    avatarUrl = BASE_URL + iconUrl,
                    element = type
                )
            }

                .getOrNull()
         }
    }
}