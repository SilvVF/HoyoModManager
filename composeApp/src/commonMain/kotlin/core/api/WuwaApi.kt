package core.api

import core.model.Character
import core.model.Game
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import net.NetHelper
import org.jsoup.Jsoup

object WuwaApi: DataApi {

    override val skinCategoryId: Int = 29524
    override val game: Game = Game.Wuwa
    override val elements: List<String> = listOf("Aero", "Electro", "Fusion", "Glacio", "Havoc", "Spectro")

    private const val BASE_URL = "https://www.prydwen.gg"

    override suspend fun elementList(): List<String> = elements

    override suspend fun characterList(): List<Character> {
        val doc = Jsoup.parse(
            NetHelper.client.get("${BASE_URL}/wuthering-waves/characters/").bodyAsText()
        )

        return doc.getElementsByClass("avatar-card").mapNotNull { element ->
            runCatching {
                val name = element.getElementsByClass("emp-name").text()
                val iconUrl = element.getElementsByClass("gatsby-image-wrapper")
                    .select("img[data-src]")
                    .first()
                    .attr("data-src")

                val type = element.getElementsByClass("gatsby-image-wrapper")
                    .takeIf { it.size > 1 }
                    ?.last()
                    ?.select("img[data-main-image]")
                    ?.first()
                    ?.attr("alt")
                    .orEmpty()

                Character(
                    id = name.hashCode(),
                    game = Game.Wuwa,
                    name = name,
                    avatarUrl = BASE_URL + iconUrl,
                    element = type
                )
            }

                .getOrNull()
        }
    }
}