package tab.game

import SearchResult
import SearchState.Companion.CHARACTER_TAG
import SearchState.Companion.MOD_TAG
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import lib.voyager.Tab
import lib.voyager.TabNavigator
import tab.SearchableTab
import tab.mod.components.GameModListScreen
import ui.LocalDataApi


interface GameTab: Tab, SearchableTab {

    val game: Game

    override val key: ScreenKey get() = game.name

    private suspend fun getModResults(query: String): List<SearchResult> {

        val matchingMods = database.execute {
            selectModsContaining(query, game)
        }

        return  matchingMods.map {
            SearchResult(
                search = it.fileName,
                tab = this,
                tags = modTags,
            )
        }
    }

    private suspend fun getCharacterResults(query: String): List<SearchResult> {

        val matchingCharacters = database.execute {
            selectCharactersNamesContaining(query, game)
        }

        return  matchingCharacters.map {
            SearchResult(
                search = it.name,
                tab = this,
                tags = characterTags,
            )
        }
    }

    override suspend fun results(query: String): List<SearchResult> {
        return supervisorScope {

            val a = async { getCharacterResults(query) }
            val b = async { getModResults(query) }

            awaitAll(a, b).flatten()
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

    companion object {
        private val database: AppDatabase = AppDatabase.instance

        private val modTags = setOf(MOD_TAG)
        private val characterTags = setOf(CHARACTER_TAG)
    }
}
