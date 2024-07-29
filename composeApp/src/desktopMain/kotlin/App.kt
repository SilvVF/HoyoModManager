import androidx.compose.animation.core.snap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.darkColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import core.api.DataApi
import core.api.GenshinApi
import core.db.DB
import core.model.Character
import core.model.Game
import core.model.Game.*
import dialog.CreateModDialog
import dialog.SettingsDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.CharacterToggleList
import java.io.File

sealed interface Dialog {
    data object Settings: Dialog
    data object AddMod: Dialog
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = darkColors(),
        content = content,
        typography = MaterialTheme.typography,
    )
}

fun combineCharacterFilterState(
    characters: Flow<Map<Game, Map<Character, List<String>>>>,
    game: Flow<Game>,
    filters: Flow<List<String>>
) = combine(characters, game, filters) { c, g, f ->
    val full = c[g]?.toList() ?: emptyList()
    if (f.isEmpty()) {
        full
    } else {
        full.groupBy { it.first.element }
            .flatMap { if (it.key !in f) emptyList() else it.value }
    }
}

val LocalDataApi = compositionLocalOf<DataApi> { error("Not provided") }

@Composable
fun AppContent(
    modifier: Modifier = Modifier,
    characters: List<Pair<Character, List<String>>>,
    filters: SnapshotStateList<String>,
    createMod: (File, Character) -> Unit
) {
    var currentDialog by remember { mutableStateOf<Dialog?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxWidth(),
        snackbarHost = {
            SnackbarHost(LocalSnackBarHostState.current)
        },
        topBar = {
            CharacterListTopBar(
                filters = filters,
                setDialog = { currentDialog = it }
            )
        },
        floatingActionButton = {
            Row {
                GenerateButton()
                Spacer(Modifier.width(22.dp))
                ExtendedFloatingActionButton(
                    onClick = { currentDialog = Dialog.AddMod },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Add"
                        )
                    },
                    text = { Text("Add Mod") }
                )
            }
        },
    ) { paddingValues ->
        CharacterToggleList(
            modifier = Modifier.fillMaxSize(),
            paddingValues = paddingValues,
            charactersWithMods = characters,
            game = LocalDataApi.current.game
        )
    }

    val dismiss = { currentDialog = null }
    when(currentDialog) {
        null -> Unit
        Dialog.AddMod -> {
            val characterList by remember {
                derivedStateOf { characters.map { it.first } }
            }
            CreateModDialog(
                onDismissRequest = dismiss,
                characters = characterList,
                createMod = { file, character ->
                    dismiss()
                    createMod(file, character)
                }
            )
        }
        Dialog.Settings -> {
            SettingsDialog(dismiss)
        }
    }
}

@Composable
@Preview
fun App() {
    AppTheme {

        val scope = rememberCoroutineScope()
        val filters = remember { mutableStateListOf<String>() }
        val syncTrigger = remember { Channel<Unit>() }
        val snackbarHostState = LocalSnackBarHostState.current
        var game by remember { mutableStateOf(Genshin) }

        val characters by produceState<List<Pair<Character, List<String>>>>(emptyList()) {
            combineCharacterFilterState(
                CharacterSync.stats,
                snapshotFlow { game },
                snapshotFlow { filters.toList() }
            )
                .collect { state -> value = state }
        }


        CompositionLocalProvider(
            LocalDataApi provides remember(game) {
                when (game) {
                    Genshin -> GenshinApi
                    StarRail -> error("")
                    ZZZ -> error("")
                }
            }
        ) {
            val dataApi = LocalDataApi.current
            AppContent(
                modifier = Modifier.fillMaxSize(),
                characters = characters,
                filters = filters,
                createMod = { file, character ->
                    scope.launch(NonCancellable + Dispatchers.IO) {
                        try {
                            val path = dataApi.game.toString() + File.separator + character.name + File.separator + file.name
                            val modFile = File(CharacterSync.rootDir, path)
                            file.copyRecursively(modFile, overwrite = true)
                            syncTrigger.send(Unit)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                })
        }
        LaunchedEffect(Unit) {
            syncTrigger.receiveAsFlow().onStart { emit(Unit) }
                .collect {
                    val job = CharacterSync.sync(GenshinApi)
                    launch {
                        snackbarHostState.showSnackbar(
                            "Refreshing",
                            duration = SnackbarDuration.Indefinite
                        )
                    }
                    runCatching { job.join() }

                    if (snackbarHostState.currentSnackbarData?.message == "Refreshing") {
                        snackbarHostState.currentSnackbarData?.dismiss()
                    }
                }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun CharacterListTopBar(
    modifier: Modifier = Modifier,
    filters: SnapshotStateList<String>,
    setDialog: (Dialog) -> Unit
) {
    TopAppBar(
        title = { Text("Filters") },
        actions = {
            val elements = LocalDataApi.current.elements
            IconButton(onClick = { filters.clear() }) {
                Icon(
                    imageVector = Icons.Outlined.Clear,
                    contentDescription = "Clear"
                )
            }
            LazyRow(Modifier.weight(1f)) {
                items(elements) {
                    val enabled by remember {
                        derivedStateOf { filters.contains(it) }
                    }
                    FilterChip(
                        enabled,
                        onClick = { filters.toggle(it) },
                        Modifier.padding(4.dp).animateItemPlacement(),
                        selectedIcon = {
                            Icon(Icons.Outlined.Check, null)
                        }
                    ) {
                        Text(it)
                    }
                }
            }
            IconButton(onClick = { setDialog(Dialog.Settings) }) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "settings"
                )
            }
        },
    )
}

@Composable
fun GenerateButton(
    modifier: Modifier = Modifier,
    scope: CoroutineScope = rememberCoroutineScope(),
    dataApi: DataApi = LocalDataApi.current
) {
    var loading by remember { mutableStateOf(false) }

    val generateFiles = {
        loading = true
        scope.launch(NonCancellable + Dispatchers.IO) {
            val exportDir = File(DB.prefsDao.select()?.exportModDir?.get(dataApi.game.data) ?: return@launch)
            val selected = DB.modDao.selectEnabledForGame(dataApi.game.data)

            selected.forEach { mod ->
                val filepath = dataApi.game.toString() + File.separator + mod.character + File.separator + mod.fileName
                val modFile = File(CharacterSync.rootDir, filepath)
                modFile.copyRecursively(File(exportDir, mod.fileName), overwrite = true)
            }

            exportDir.listFiles()?.forEach { file ->
                when {
                    file.name == "BufferValues" -> Unit
                    file.extension == "exe" -> Unit
                    file.name !in selected.map { it.fileName } -> {
                        file.deleteRecursively()
                    }
                }
            }
        }.invokeOnCompletion { loading = false }
    }

    ExtendedFloatingActionButton(
        modifier = modifier,
        onClick = {
            if (loading)
                return@ExtendedFloatingActionButton

            generateFiles()
        },
        icon = {
            if (loading) {
                CircularProgressIndicator()
            } else {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Generate"
                )
            }
        },
        backgroundColor = MaterialTheme.colors.secondary.copy(
            alpha = if(loading) 0.5f else 1f
        ),
        text = {
            Text("Generate")
        }
    )
}