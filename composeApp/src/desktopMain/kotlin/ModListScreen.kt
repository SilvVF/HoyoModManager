import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import core.db.DB
import core.model.Character
import core.model.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ui.CharacterToggleList
import ui.LocalDataApi
import ui.dialog.CreateModDialog
import ui.dialog.SettingsDialog
import java.io.File

private sealed interface Dialog {
    data object Settings: Dialog
    data class AddMod(val selected: Character? = null): Dialog
}

@Composable
fun GameModListScreen(
    selectedGame: Game,
    modifier: Modifier = Modifier,
) {
    val dataApi = LocalDataApi.current
    val scope = rememberCoroutineScope()
    val syncTrigger = remember { Channel<SyncRequest>() }
    val filters = remember { mutableStateListOf<String>() }
    val snackbarHostState = LocalSnackBarHostState.current

    val characters by produceState<List<Character>>(emptyList()) {
        snapshotFlow { selectedGame }
            .onEach { filters.clear() }
            .flatMapLatest { g ->
                DB.characterDao.observeByGame(g)
            }
            .collect { value = it }
    }

    val filteredCharacters by produceState<List<Character>>(emptyList()) {
        combine(
            snapshotFlow { filters.toList() },
            snapshotFlow { characters },
        ) {  filter, chars ->
            if (filter.isEmpty()) {
                chars
            } else {
                chars.filter { filter.contains(it.element.lowercase()) }
            }
        }
            .collect { state -> value = state }
    }

    var currentDialog by remember { mutableStateOf<Dialog?>(null) }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        topBar = {
            CharacterListTopBar(
                filters = filters,
                setDialog = { currentDialog = it },
                refresh = {
                    syncTrigger.trySend(SyncRequest.UserInitiated(true))
                }
            )
        },
        floatingActionButton = {
            Row {
                GenerateButton()
                Spacer(Modifier.width(22.dp))
                ExtendedFloatingActionButton(
                    onClick = { currentDialog = Dialog.AddMod() },
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
            characters = filteredCharacters,
            onCharacterIconClick = {
                currentDialog = Dialog.AddMod(it)
            },
            game = LocalDataApi.current.game
        )
    }

    val dismiss = { currentDialog = null }
    when(val dialog = currentDialog) {
        null -> Unit
        is Dialog.AddMod -> {
            CreateModDialog(
                initialCharacter = dialog.selected,
                onDismissRequest = dismiss,
                characters = characters,
                createMod = { file, character ->
                    dismiss()
                    scope.launch(NonCancellable + Dispatchers.IO) {
                        try {
                            val path = dataApi.game.toString() + File.separator + character.name + File.separator + file.name
                            val modFile = File(CharacterSync.rootDir, path)
                            file.copyRecursively(modFile, overwrite = true)
                            syncTrigger.send(SyncRequest.UserInitiated(false))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            )
        }
        Dialog.Settings -> {
            SettingsDialog(dismiss)
        }
    }

    LaunchedEffect(dataApi) {
        syncTrigger.receiveAsFlow()
            .onStart {
                if (!CharacterSync.initialSyncDone.contains(dataApi.game)) {
                    emit(SyncRequest.Startup)
                }
            }
            .collectLatest { req ->

                if (CharacterSync.running.contains(selectedGame))
                    return@collectLatest

                val (fromNetwork, onComplete) = when(req) {
                    SyncRequest.Startup -> false to {
                        CharacterSync.running.remove(dataApi.game)
                        CharacterSync.initialSyncDone.add(dataApi.game)
                    }
                    is SyncRequest.UserInitiated -> req.network to {
                        CharacterSync.running.remove(dataApi.game)
                    }
                }

                ensureActive()
                val job = CharacterSync.sync(dataApi, fromNetwork)
                CharacterSync.running[selectedGame] = job

                runCatching {  job.join() }

                onComplete()
            }
    }
}

@Composable
private fun CharacterListTopBar(
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
                items(elements, key = { it }) { element ->
                    val selected by remember(elements, filters) {
                        derivedStateOf { filters.contains(element.lowercase()) }
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { filters.toggle(element.lowercase()) },
                        modifier = Modifier.padding(4.dp).animateItemPlacement(),
                        selectedIcon = {
                            Icon(Icons.Outlined.Check, null)
                        }
                    ) {
                        Text(element)
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