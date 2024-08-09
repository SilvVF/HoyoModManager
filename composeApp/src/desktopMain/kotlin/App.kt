import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ElevatedFilterChip
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import core.api.DataApi
import core.db.LocalDatabase
import core.db.Prefs
import core.model.Game.Genshin
import core.model.Game.StarRail
import core.model.Game.ZZZ
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import lib.voyager.Tab
import lib.voyager.TabDisposable
import lib.voyager.TabNavigator
import org.jetbrains.compose.ui.tooling.preview.Preview
import tab.ReselectTab
import tab.SearchableTab
import tab.game.GenshinTab
import tab.game.StarRailTab
import tab.game.ZenlessZoneZeroTab
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

val TABS_LIST = listOf(
    GenshinTab,
    StarRailTab,
    ZenlessZoneZeroTab,
    PlaylistTab,
    ModTab
)

@Composable
@Preview
fun App() {
    AppTheme {
        TabNavigator(
            tab = GenshinTab,
            tabDisposable = {
                TabDisposable(it, TABS_LIST)
            }
        ) { navigator ->

            val scope = rememberCoroutineScope()
            val searchState = remember { SearchState(scope, navigator) }

            var searchHovered by remember { mutableStateOf(false) }

            Row {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .padding(start = 12.dp, end = 4.dp)
                        .clip(MaterialTheme.shapes.medium)
                ) {
                    TABS_LIST.fastForEach { tab ->
                        NavigationRailItem(
                            selected = navigator.current == tab ,
                            onClick = {
                                if (navigator.current == tab) {
                                    (tab as? ReselectTab)?.let {
                                        scope.launch { tab.onReselect() }
                                    }
                                } else {
                                    navigator.current = tab
                                }
                            },
                            label = { Text(tab.toString()) },
                            icon = { tab.Icon() }
                        )
                    }
                }
                Scaffold(
                    topBar = {
                        Box(Modifier.fillMaxWidth(0.8f).animateContentSize(), contentAlignment = Alignment.Center) {
                            Surface(
                                modifier = Modifier.padding(8.dp),
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Column(
                                    Modifier.wrapContentHeight()
                                ) {
                                    SearchBar(
                                        query = searchState.query.text,
                                        onQueryChange =  { searchState.update(it) },
                                        onSearch = { searchState.update(it) },
                                        onActiveChange = {},
                                        active = false,
                                        modifier = Modifier.fillMaxWidth(0.8f)
                                    ){}
                                    SearchSuggestions(
                                        searchState = searchState,
                                        navigator = navigator
                                    )
                                }
                            }
                        }
                    }
                ) { contentPadding ->
                    Box(
                        Modifier.padding(contentPadding),
                        Alignment.Center
                    ) {
                        CompositionLocalProvider(
                            LocalSearchState provides searchState
                        ) {
                            Surface(
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .padding(start = 4.dp, end = 12.dp)
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
fun SearchSuggestions(
    searchState: SearchState,
    navigator: TabNavigator,
    modifier: Modifier = Modifier
) {

    var selected by remember {
        mutableStateOf<Tab?>(null)
    }
    val results by searchState.results.collectAsState()
    val grouped = remember(results) { results.groupBy { it.tab } }
    val groupedList = remember(grouped) { grouped.toList() }

    val autocomplete by searchState.autoComplete.collectAsState()

    val auto by remember(autocomplete) {
        derivedStateOf {
            if (autocomplete.size > 5) autocomplete.shuffled().take(5)
            else autocomplete
        }
    }

    Column(modifier.animateContentSize()) {
        auto.fastForEach {
            Text(
                text = it,
                modifier = Modifier.clickable {
                    searchState.update(it)
                }
            )
            Divider()
        }
        if (results.isNotEmpty()) {
            Text("Other results for ${searchState.query.text}")
            FlowRow {
                groupedList.fastForEach { (tab, results) ->
                    val isSelected = tab == selected
                    ElevatedFilterChip(
                        selected = isSelected,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.rotate(if (isSelected) 180f else 0f)
                            )
                        },
                        onClick = { selected = if (isSelected) null else tab },
                        label = { Text(tab.toString()) },
                        trailingIcon = { Text(results.size.toString()) },
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }
            AnimatedContent(selected) { targetState ->

                val resultGroups = remember(grouped) {
                    grouped[targetState]?.groupBy { it.tags }?.toList().orEmpty()
                }

                FlowRow {
                    resultGroups.fastForEach { group ->
                        ElevatedAssistChip(
                            onClick = {
                                (targetState as? SearchableTab)
                                    ?.onResultSelected(
                                        group.second.first(),
                                        navigator
                                    )
                            },
                            label = { Text(group.first.joinToString(", ")) },
                            modifier = Modifier.padding(2.dp)
                        )
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

            val exportDir = File(
                when (dataApi.game) {
                    Genshin -> Prefs.genshinDir()
                    StarRail -> Prefs.starRailDir()
                    ZZZ -> Prefs.zenlessDir()
                }.get()
            )
            val selected = database.selectEnabledForGame(dataApi.game.data)

            val ignore = Prefs.ignoreOnGeneration().get()

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