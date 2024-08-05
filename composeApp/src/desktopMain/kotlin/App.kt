import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import core.api.DataApi
import core.db.LocalDatabase
import core.model.Game
import core.model.Game.Genshin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lib.voyager.Tab
import lib.voyager.TabDisposable
import lib.voyager.TabNavigator
import org.jetbrains.compose.ui.tooling.preview.Preview
import tab.ReselectTab
import tab.SearchableTab
import tab.game.GameTab
import tab.mod.ModTab
import tab.playlist.PlaylistTab
import ui.AppTheme
import ui.LocalDataApi
import java.io.File
import java.nio.file.Paths


sealed interface SyncRequest {
    data object Startup: SyncRequest
    data class UserInitiated(val network: Boolean): SyncRequest
}

private val gameTabs = Game.entries.map { game ->
    object : GameTab {

        override val key: ScreenKey = game.name

        override val game: Game = game
    }
}

private val otherTabs = listOf(
    PlaylistTab,
    ModTab
)

private val tabs = otherTabs + gameTabs

data class SearchResult(
    val text: String,
    val tab: Tab? = null,
    val route: Screen? = null
)

class SearchState(
    scope: CoroutineScope,
    private val navigator: TabNavigator,
) {

    var query by mutableStateOf(TextFieldValue(""))

    val resultFlow: StateFlow<List<SearchResult>> = snapshotFlow { query.text }
        .debounce(1000)
        .map { getOtherResults(it) }
        .stateIn(
            scope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    private suspend fun getOtherResults(query: String): List<SearchResult> {
        return tabs.filter { navigator.current != it }.mapNotNull {
            (it as? SearchableTab)?.results(query)
        }
            .flatten()
    }

    fun update(string: String) {
        query = query.copy(text = string)
    }
}

val LocalSearchState = staticCompositionLocalOf<SearchState> { error("Not provided") }

@Composable
@Preview
fun App() {
    AppTheme {
        TabNavigator(
            tab = gameTabs.first(),
            tabDisposable = {
                TabDisposable(it, tabs)
            }
        ) { navigator ->

            val scope = rememberCoroutineScope()
            val searchState = remember { SearchState(scope, navigator) }

            Scaffold(
                topBar = {
                    Box(Modifier.fillMaxWidth(0.8f).animateContentSize(), contentAlignment = Alignment.Center) {

                        val results by searchState.resultFlow.collectAsState()

                        Surface(
                            modifier = Modifier.padding(8.dp),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            SearchBar(
                                modifier = Modifier.padding(4.dp)
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .heightIn(max = 120.dp),
                                query = searchState.query.text,
                                onQueryChange =  { searchState.update(it) },
                                onSearch = { searchState.update(it) },
                                onActiveChange = {},
                                active = results.isNotEmpty(),
                            ) {
                                FlowRow {
                                    results.fastForEach { result ->
                                        AssistChip(
                                            onClick = {
                                                when  {
                                                    result.tab != null -> {
                                                        (result.tab as? SearchableTab)?.onResultSelected(result, navigator)
                                                    }
                                                }
                                            },
                                            label = { Text(result.text) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { contentPadding ->
                Box(Modifier.padding(contentPadding), Alignment.Center) {
                    Row {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .padding(start = 12.dp, end = 4.dp)
                                .clip(MaterialTheme.shapes.medium)
                        ) {

                            val handleSelect = { tab: Tab ->

                                if (navigator.current == tab && tab is ReselectTab) {
                                    tab.onReselect()
                                } else {
                                    navigator.current = tab
                                }
                            }

                            gameTabs.fastForEach { tab ->
                                NavigationRailItem(
                                    selected = when (val screen = navigator.current) {
                                        is GameTab -> tab.game == screen.game
                                        else -> false
                                    },
                                    onClick = { handleSelect(tab) },
                                    label = { Text(tab.game.name) },
                                    icon = { tab.Icon() }
                                )
                            }
                            otherTabs.fastForEach { tab ->
                                NavigationRailItem(
                                    selected = navigator.current == tab ,
                                    onClick = { handleSelect(tab) },
                                    label = { Text(tab.toString()) },
                                    icon = { tab.Icon() }
                                )
                            }
                        }
                        CompositionLocalProvider(
                            LocalSearchState provides searchState
                        ) {
                            Surface(
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .padding(start =4.dp, end = 12.dp)
                                    .fillMaxSize(),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                            ) {
                                lib.voyager.FadeTransition(navigator)
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun GenerateButton(
    modifier: Modifier = Modifier,
    scope: CoroutineScope = rememberCoroutineScope(),
    dataApi: DataApi = LocalDataApi.current
) {
    var loading by remember { mutableStateOf(false) }
    val database = LocalDatabase.current

    val generateFiles = { copyAll: Int ->
        val copy = copyAll != 0
        loading = true
        scope.launch(NonCancellable + Dispatchers.IO) {

            val exportDir = File(database.selectMetaData()?.exportModDir?.get(dataApi.game.data) ?: return@launch)
            val selected = database.selectEnabledForGame(dataApi.game.data)

            val ignore = database.selectMetaData()?.keepFilesOnClear.orEmpty()

            exportDir.listFiles()?.forEach { file ->
                when {
                    file.name == "BufferValues" -> Unit
                    file.extension == "exe" -> Unit
                    file.isFile -> Unit
                    ignore.contains(file.path) -> Unit
                    else -> {
                        runCatching {
                            val (id, filename) = file.name.split("_")
                            when  {
                                selected.find { it.id == id.toInt() }?.fileName != filename ->
                                    file.deleteRecursively()
                                else -> Unit
                            }
                        }
                            .onFailure { file.deleteRecursively() }
                    }
                }
            }

            selected.forEach { mod ->
                runCatching {
                    val modFile = Paths.get(CharacterSync.rootDir.path, dataApi.game.name, mod.character, mod.fileName).toFile()

                    modFile.copyRecursively(
                        File(exportDir, "${mod.id}_${mod.fileName}"), overwrite = copy
                    )
                }
            }



            if (dataApi.game == Genshin) {
                val exeFix = exportDir.listFiles()?.find { it.isFile && it.extension == "exe" } ?: return@launch
                Runtime.getRuntime().exec(
                    "cmd.exe /c cd ${exportDir.path} && start ${exeFix.name}",
                    null,
                    File(CharacterSync.rootDir.path)
                )
            }
        }.invokeOnCompletion { loading = false }
    }

    repeat(2) {
        ExtendedFloatingActionButton(
            modifier = modifier,
            shape = if(it == 0) {
                RoundedCornerShape(
                    topStart = 50f,
                    bottomStart = 50f
                )
            } else {
                RoundedCornerShape(topEnd = 50f, bottomEnd = 50f)
            },
            onClick = {
                if (loading)
                    return@ExtendedFloatingActionButton

                generateFiles(it)
            },
            icon = {
                if(it != 0)
                    return@ExtendedFloatingActionButton

                if (loading) {
                    CircularProgressIndicator()
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Generate"
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                alpha = if(loading) 0.5f else 1f
            ),
            text = {
                if (it == 0) {
                    Text("Reload")
                } else {
                    Text("Generate")
                }
            }
        )
    }
}