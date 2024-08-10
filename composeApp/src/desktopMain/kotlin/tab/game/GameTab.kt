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
import core.db.AppDatabase
import core.model.Game
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import lib.voyager.Tab
import lib.voyager.TabNavigator
import tab.SearchableTab
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

    override suspend fun results(
        tags: Set<String>,
        query: String,
        current: Boolean
    ): List<SearchResult> {
        return supervisorScope {
            val a = async {
                if (tags.isEmpty() || tags.contains(CHARACTER_TAG))
                    getCharacterResults(query)
                else emptyList()
            }
            val b = async {
                if (tags.isEmpty() || tags.contains(MOD_TAG))
                    getModResults(query)
                else emptyList()
            }

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
            LocalDataApi provides remember(game) { game.api() }
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
