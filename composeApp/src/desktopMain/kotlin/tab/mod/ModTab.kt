package tab.mod

import SearchResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition
import core.api.GenshinApi
import core.api.StarRailApi
import core.api.ZZZApi
import core.db.Prefs
import core.db.prefs.getBlocking
import core.model.Game.Genshin
import core.model.Game.StarRail
import core.model.Game.ZZZ
import kotlinx.coroutines.CoroutineScope
import lib.voyager.LocalTabNavigator
import lib.voyager.Tab
import lib.voyager.TabNavigator
import tab.ComposeReselectTab
import tab.LaunchedOnReselect
import tab.ReselectTab
import tab.SearchableTab
import tab.game.GenshinTab
import tab.game.StarRailTab
import tab.game.ZenlessZoneZeroTab
import ui.LocalDataApi

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

class ModTabStateHolder(
    private val scope: CoroutineScope,
    private val navigator: Navigator,
) {

    var game by mutableStateOf(Prefs.lastModScreen().getBlocking())

    val dataApi by derivedStateOf {
        when(game) {
            Genshin -> GenshinApi
            StarRail -> StarRailApi
            ZZZ -> ZZZApi
        }
    }

    val gameTab = {
        when(game) {
            Genshin -> GenshinTab
            StarRail -> StarRailTab
            ZZZ -> ZenlessZoneZeroTab
        }
    }

    val segments by derivedStateOf {
        when (val screen = navigator.lastItemOrNull) {
            null -> emptyList()
            is ModBrowse -> listOf(
                dataApi.game.name to gameTab(),
                "Mods" to null,
                "Skins" to null
            )
            is ModView -> listOf(
                dataApi.game.name to gameTab(),
                "Mods" to null,
                "Skins" to ModBrowse(dataApi.skinCategoryId),
                "${screen.idRow}" to null
            )
            else -> emptyList()
        }
    }
}

@Composable
private fun ModTabContent(navigator: Navigator) {

    val tabNavigator = LocalTabNavigator.current
    val scope = rememberCoroutineScope()
    val stateHolder = remember(navigator, scope) { ModTabStateHolder(scope, navigator) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val primaryColor = MaterialTheme.colorScheme.primary

                        stateHolder.segments.fastForEachIndexed { i, (path, screen) ->
                            val interactionSource = remember { MutableInteractionSource() }
                            val isLastIndex = stateHolder.segments.lastIndex == i
                            var active by remember { mutableStateOf(false) }
                            Text(
                                text = path,
                                color = if (isLastIndex) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                                modifier = Modifier.clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    when (screen) {
                                        null -> Unit
                                        is Tab -> {
                                            navigator.popUntilRoot()
                                            tabNavigator.current = screen
                                        }
                                        else -> navigator.popUntil { it == screen }.run {
                                            if (!this) navigator.push(screen)
                                        }
                                    }
                                }
                                    .onPointerEvent(PointerEventType.Enter) { active = true }
                                    .onPointerEvent(PointerEventType.Exit) { active = false }
                                    .drawBehind {
                                        if (active) {
                                            drawRoundRect(
                                                color = primaryColor,
                                                cornerRadius = CornerRadius(12f),
                                                size = Size(this.size.width, 4.dp.toPx()),
                                                topLeft = Offset(x = 0f, y = this.size.height)
                                            )
                                        }
                                    }
                            )
                            if (!isLastIndex) {
                                Text(
                                    text = ">",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        LocalContentColor.current.copy(alpha = 0.78f)
                                    )
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
        Box(Modifier.padding(paddingValues)) {
            CompositionLocalProvider(
                LocalDataApi provides stateHolder.dataApi
            ) {
                FadeTransition(navigator)
            }
        }
    }
}

