package tab.game

import SearchResult
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.ScreenKey
import core.api.GenshinApi
import core.api.StarRailApi
import core.api.ZZZApi
import core.db.AppDatabase
import core.model.Game
import core.model.Game.Genshin
import core.model.Game.StarRail
import core.model.Game.ZZZ
import lib.voyager.Tab
import lib.voyager.TabNavigator
import tab.SearchableTab
import tab.mod.GameModListScreen
import ui.LocalDataApi

interface GameTab: Tab, SearchableTab {

    val game: Game

    private val database: AppDatabase get() = AppDatabase.instance

    override val key: ScreenKey get() = super.key + game.name

    override suspend fun results(query: String): List<SearchResult> {
        val matchingCharacters = database.execute {
            selectCharactersNamesContaining(query, game)
        }
        val matchingModCount = database.execute {
            selectCountModsContaining(query, game)
        }

        println("${game.name} $query Characters ${matchingCharacters.size}, Mods $matchingModCount")

        return buildList {
            if (matchingModCount > 0) {
                add(
                    SearchResult(
                        "$matchingModCount Mods",
                        tab = this@GameTab,
                        tags = emptyList(),
                        searchTag = "mod"
                    )
                )
            }
            if (matchingCharacters.isNotEmpty()) {
                add(
                    SearchResult(
                        "${matchingCharacters.size} Characters",
                        tab = this@GameTab,
                        tags = matchingCharacters.map {
                            listOf(Pair("c", it.name), Pair("character", it.name))
                        }
                            .flatten(),
                        searchTag = "character"
                    )
                )
            }
        }
    }

    override fun onResultSelected(result: SearchResult, navigator: TabNavigator) {
        navigator.current = this
    }

    @Composable
    override fun Icon() = game.UiIcon()

    @Composable
    override fun Content() {
        CompositionLocalProvider(
            LocalDataApi provides remember(game) {
                when(game) {
                    Genshin -> GenshinApi
                    StarRail -> StarRailApi
                    ZZZ -> ZZZApi
                }
            }
        ) {
            GameModListScreen(Modifier.fillMaxSize())
        }
    }
}
