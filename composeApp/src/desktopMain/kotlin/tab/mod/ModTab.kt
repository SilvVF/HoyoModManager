package tab.mod

import SearchResult
import tab.ReselectTab
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.transitions.FadeTransition
import core.api.GenshinApi
import core.api.StarRailApi
import core.api.ZZZApi
import core.model.Game.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import lib.voyager.Tab
import lib.voyager.TabNavigator
import tab.ComposeReselectTab
import tab.LaunchedOnReselect
import tab.SearchableTab
import ui.LocalDataApi
import kotlin.reflect.KClass

data object ModTab: Tab, ComposeReselectTab by ReselectTab.compose(), SearchableTab {

    @Transient
    var nestedNavigator: Navigator? = null

    @Composable
    override fun Icon() =
        androidx.compose.material3.Icon(
            imageVector = Icons.Outlined.FavoriteBorder,
            contentDescription = null
        )

    override suspend fun results(query: String): List<SearchResult> {
        return emptyList()
    }

    override fun onResultSelected(result: SearchResult, navigator: TabNavigator) {
        navigator.current = this
        val route = result.route
        val nav = nestedNavigator
        if (route!= null && nav != null) {
            with(nav) {
                replaceAll(items.filterNot { screen -> screen == route } + route)
            }
        }
    }


    @Composable
    override fun Content() {
        Navigator(ModBrowse(GenshinApi.skinCategoryId)) { navigator ->

            nestedNavigator = navigator

            LaunchedOnReselect {
                navigator.popUntilRoot()
            }

            ModTabContent(navigator)
        }
    }
}

@Composable
fun ModTabContent(navigator: Navigator) {

    val game = remember { Genshin }

    CompositionLocalProvider(
        LocalDataApi provides remember(game) {
            when (game) {
                Genshin -> GenshinApi
                StarRail -> StarRailApi
                ZZZ -> ZZZApi
            }
        }
    ) {
        FadeTransition(navigator)
    }
}

private class ModView(
    val idRow: Int,
): Screen {

    @Composable
    override fun Content() {

        val navigator = LocalNavigator.currentOrThrow

        ModViewContent(
            idRow,
            onBackPressed = { navigator.pop() },
            Modifier.fillMaxSize()
        )
    }
}

private class ModBrowse(
    private val categoryId: Int
): Screen {

    override val key: ScreenKey
        get() = super.key + categoryId


    @Composable
    override fun Content() {

        val navigator = LocalNavigator.currentOrThrow

        ModBrowseContent(
            categoryId = categoryId,
            onModClick = { id -> navigator.push(ModView(id)) }
        )
    }
}
