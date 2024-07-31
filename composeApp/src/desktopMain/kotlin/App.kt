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
import core.api.StarRailApi
import core.db.DB
import core.model.Character
import core.model.Game
import core.model.Game.*
import core.seperate
import ui.dialog.CreateModDialog
import ui.dialog.SettingsDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.AppTheme
import ui.CharacterToggleList
import ui.LocalDataApi
import java.io.File
import java.nio.file.Paths

sealed interface Dialog {
    data object Settings: Dialog
    data object AddMod: Dialog
}

fun combineCharacterFilterState(
    game: Flow<Game>,
    filters: Flow<List<String>>
) = game.flatMapLatest { g ->
    DB.characterDao.observeByGame(g)
}.combine(filters) { names, filters ->
    if (filters.isEmpty()) {
        names
    } else {
        names
            .groupBy { it.element }
            .flatMap { (element, names) ->
                if (element !in filters) emptyList() else names
            }
    }
}

@Composable
fun AppContent(
    modifier: Modifier = Modifier,
    characters: List<Character>,
    filters: SnapshotStateList<String>,
    createMod: (File, Character) -> Unit,
    refresh: () -> Unit,
) {
    var currentDialog by remember { mutableStateOf<Dialog?>(null) }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(LocalSnackBarHostState.current)
        },
        topBar = {
            CharacterListTopBar(
                filters = filters,
                setDialog = { currentDialog = it },
                refresh = refresh
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
            characters = characters,
            game = LocalDataApi.current.game
        )
    }

    val dismiss = { currentDialog = null }
    when(currentDialog) {
        null -> Unit
        Dialog.AddMod -> {
            CreateModDialog(
                onDismissRequest = dismiss,
                characters = characters,
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
        val syncTrigger = remember { Channel<Boolean>() }
        val snackbarHostState = LocalSnackBarHostState.current
        var game by remember { mutableStateOf(StarRail) }

        val characters by produceState<List<Character>>(emptyList()) {
            combineCharacterFilterState(
                snapshotFlow { game },
                snapshotFlow { filters.toList() }
            )
                .collect { state -> value = state }
        }


        CompositionLocalProvider(
            LocalDataApi provides remember(game) {
                when (game) {
                    Genshin -> GenshinApi
                    StarRail -> StarRailApi
                    ZZZ -> error("")
                }
            }
        ) {
            val dataApi = LocalDataApi.current
            AppContent(
                modifier = Modifier.fillMaxSize(),
                characters = characters,
                filters = filters,
                refresh = {
                    syncTrigger.trySend(true)
                },
                createMod = { file, character ->
                    scope.launch(NonCancellable + Dispatchers.IO) {
                        try {
                            val path = dataApi.game.toString() + File.separator + character.name + File.separator + file.name
                            val modFile = File(CharacterSync.rootDir, path)
                            file.copyRecursively(modFile, overwrite = true)
                            syncTrigger.send(false)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                })

            LaunchedEffect(dataApi) {
                syncTrigger.receiveAsFlow().onStart { emit(false) }
                    .collect { forceNetork ->
                        val job = CharacterSync.sync(dataApi = dataApi, fromNetwork = forceNetork)
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
}

@Composable
fun CharacterListTopBar(
    modifier: Modifier = Modifier,
    filters: SnapshotStateList<String>,
    setDialog: (Dialog) -> Unit,
    refresh: () -> Unit
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
            IconButton(onClick = refresh) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "refresh"
                )
            }
            IconButton(onClick = { setDialog(Dialog.Settings) }) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "settings"
                )
            }
        },
        modifier = modifier
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

            exportDir.listFiles()?.forEach { file ->
                when {
                    file.name == "BufferValues" -> Unit
                    file.extension == "exe" -> Unit
                    file.isFile -> Unit
                    else -> {
                        file.deleteRecursively()
                    }
                }
            }

            selected.forEach { mod ->
                val modFile = Paths.get(CharacterSync.rootDir.path, dataApi.game.name, mod.character, mod.fileName).toFile()
                modFile.copyRecursively(File(exportDir, "${mod.id}_${mod.fileName}"), overwrite = true)
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