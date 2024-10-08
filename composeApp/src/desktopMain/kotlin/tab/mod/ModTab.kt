package tab.mod

import SearchResult
import SearchState.Companion.URL_TAG
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
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
import com.eygraber.uri.Url
import core.api.GameBananaApi
import core.api.GenshinApi
import core.api.StarRailApi
import core.api.ZZZApi
import core.db.Prefs
import core.db.prefs.getBlocking
import core.model.Game
import core.model.Game.Genshin
import core.model.Game.StarRail
import core.model.Game.ZZZ
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import lib.voyager.LocalTabNavigator
import lib.voyager.Tab
import lib.voyager.TabNavigator
import tab.ComposeReselectTab
import tab.LaunchedOnReselect
import tab.ReselectTab
import tab.SearchableTab
import tab.game.GenshinTab
import tab.game.StarRailTab
import tab.game.WutheringWavesTab
import tab.game.ZenlessZoneZeroTab
import ui.LocalDataApi

data object ModTab: Tab, ComposeReselectTab by ReselectTab.compose(), SearchableTab {

    private val navChannel = Channel<List<Screen>>(1)

    private val defaultRecommendedUrls = listOf(
        SearchResult(
            search = "https://gamebanana.com/mods/",
            tab = this,
            tags = setOf(URL_TAG),

        ),
        SearchResult(
            search =  "https://gamebanana.com/mods/cats/",
            tab = this,
            tags = setOf(URL_TAG)
        )
    )

    @Composable
    override fun Icon() =
        androidx.compose.material3.Icon(
            imageVector = Icons.Outlined.FavoriteBorder,
            contentDescription = null
        )

    override suspend fun results(
        tags: Set<String>,
        query: String,
        current: Boolean
    ): List<SearchResult> {

        val url = if (tags.isEmpty() || tags.contains(URL_TAG)) {
            Url.parseOrNull(query)
        } else null

        if (url == null) return if (tags.contains(URL_TAG)) defaultRecommendedUrls else emptyList()

        return defaultRecommendedUrls
    }

    suspend fun trySearch(url: Url, tabNavigator: TabNavigator)  {

        if (url.host != "gamebanana.com") return

        val path = url.path.orEmpty().removePrefix("/")

        println("matched host")

        when  {
            Regex("^mods\\/cats\\/\\d+\$").matches(path) -> {

                val skinCats =  Game.entries.map { it.api().skinCategoryId }
                val category = url.pathSegments.last().toInt()

                if (skinCats.contains(category)) {
                    tabNavigator.current = this
                    delay(10)
                    navChannel.trySend(listOf(ModBrowse(category)))
                }
            }
        }
    }

    override fun onResultSelected(result: SearchResult, navigator: TabNavigator) {
        navigator.current = this

        result.route?.let { navChannel.trySend(listOf(it)) }
    }


    @Composable
    override fun Content() {

        val scope = rememberCoroutineScope()

        Navigator(ModBrowse(GenshinApi.skinCategoryId)) { navigator ->
            LaunchedEffect(navChannel) {
                try {
                    for (items in navChannel) {
                        navigator.replaceAll(items)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) { println(e.stackTraceToString()) }
            }

            val stateHolder = remember(navigator, scope) { ModTabStateHolder(scope, navigator) }

            LaunchedOnReselect {
                navigator.popUntilRoot()
            }

            ModTabContent(navigator, stateHolder)
        }
    }
}

class ModTabStateHolder(
    private val scope: CoroutineScope,
    private val navigator: Navigator,
) {

    val game by derivedStateOf {
        Game.entries.first {
            it.api().skinCategoryId == (navigator.items.firstOrNull() as? ModBrowse)?.categoryId
        }
    }

    val dataApi by derivedStateOf { game.api() }

    val gameTab = {
        when(game) {
            Genshin -> GenshinTab
            StarRail -> StarRailTab
            ZZZ -> ZenlessZoneZeroTab
            Game.Wuwa -> WutheringWavesTab
        }
    }

    private val categoryIds = Game.entries.map { it.api().skinCategoryId }

    val segments: List<Pair<String, Screen?>> by derivedStateOf {
        buildList {
            add(dataApi.game.name to gameTab())
            add("Mods" to null)
            navigator.items.forEach { screen ->
                when (val s = screen) {
                    is ModBrowse -> {
                        if (categoryIds.contains(s.categoryId)) {
                            add("Skins" to s)
                        } else {
                            add("${s.name}" to s)
                        }
                    }
                    is ModView -> {
                        add("${s.idRow}" to null)
                    }
                }
            }
        }
    }
}

val LocalSortMode = compositionLocalOf<MutableState<GameBananaApi.Sort?>> { error("Not provided") }

@Composable
private fun ModTabContent(navigator: Navigator, stateHolder: ModTabStateHolder) {

    val tabNavigator = LocalTabNavigator.current
    val sortMode = remember { mutableStateOf<GameBananaApi.Sort?>(null) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    CompositionLocalProvider(
        LocalDataApi provides stateHolder.dataApi,
        LocalSortMode provides sortMode
    ) {
        Scaffold(
            topBar = {
                ModTopAppBar(
                    navigator,
                    tabNavigator,
                    stateHolder,
                    scrollBehavior
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { paddingValues ->
            Box(Modifier.padding(paddingValues)) {
                FadeTransition(navigator)
            }
        }
    }
}

@Composable
private fun ModTopAppBar(
    navigator: Navigator,
    tabNavigator: TabNavigator,
    stateHolder: ModTabStateHolder,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier
) {
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
                                else -> navigator.popUntil { it.key == screen.key }
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
        actions = {
            var sortMode by LocalSortMode.current
            Row {
                GameBananaApi.Sort.entries.fastForEach {
                    val text = remember {
                        buildString {
                            for((i, c) in it.name.withIndex()) {
                                if (i == 0){
                                    append(c)
                                    continue
                                }
                                if (c.isUpperCase()) append(' ')
                                append(c)
                            }
                        }
                    }
                    FilterChip(
                        selected = it == sortMode,
                        onClick = { sortMode = it },
                        label = { Text(text) },
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }
        },
        navigationIcon = {
            Row {
                Game.entries.fastForEach {
                    FilterChip(
                        label = { Text(it.name) },
                        onClick = {
                            navigator.replaceAll(
                                ModBrowse(it.api().skinCategoryId)
                            )
                        },
                        selected = stateHolder.game == it,
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
        modifier = modifier
    )
}