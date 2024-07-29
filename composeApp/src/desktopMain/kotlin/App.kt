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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import core.db.DB
import core.model.Game
import dialog.CreateModDialog
import dialog.SettingsDialog
import kotlinx.coroutines.Dispatchers
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

fun <T, E> Flow<T>.combineToPair(flow2: Flow<E>): Flow<Pair<T, E>> = this.combine(flow2) { a, b -> a to b }

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = darkColors(),
        content = content,
        typography = MaterialTheme.typography,
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
@Preview
fun App() {
    AppTheme {

        val scope = rememberCoroutineScope()
        val filters = remember { mutableStateListOf<String>() }
        val syncTrigger = remember { Channel<Unit>() }

        val characters by produceState(emptyList()) {
            CharacterSync.stats.combineToPair(snapshotFlow { filters.toList() })
                .collect { (chars, filters) ->
                val full = chars.toList()
                value = if (filters.isEmpty())
                    full
                else
                    full.groupBy { it.first.element }
                        .flatMap { if (it.key !in filters) emptyList() else it.value }
            }
        }

        val syncActive by produceState(true) {
            syncTrigger.receiveAsFlow().onStart { emit(Unit) }.collect {
                value = true
                val job = CharacterSync.sync()
                runCatching { job.join() }
                value = false
            }
        }

        var currentDialog by remember { mutableStateOf<Dialog?>(null) }

        Scaffold(
            modifier = Modifier.fillMaxWidth(),
            topBar = {
                TopAppBar(
                    title = { Text("Filters") },
                    actions = {
                        IconButton(onClick = { filters.clear() }) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = "Clear"
                            )
                        }
                        LazyRow(Modifier.weight(1f)) {
                            items(CharacterSync.elements) {
                               FilterChip(
                                   remember {
                                       derivedStateOf { filters.contains(it) }
                                   }.value,
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
                        IconButton(onClick = { currentDialog = Dialog.Settings }) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "settings"
                            )
                        }
                    },
                )
            },
            floatingActionButton = {
                Row {
                    var loading by remember { mutableStateOf(false) }

                    ExtendedFloatingActionButton(
                        onClick = {
                            if (loading)
                                return@ExtendedFloatingActionButton

                            scope.launch(Dispatchers.IO) {
                                loading = true
                                val exportDir = File(DB.prefsDao.select()?.exportModDir ?: return@launch)

                                val selected = DB.modDao.selectEnabledForGame(Game.Genshin.toByte())

                                selected.forEach { mod ->
                                    val modFile = File(CharacterSync.rootDir,mod.character + "\\" + mod.fileName)

                                    modFile.copyRecursively(File(exportDir, mod.fileName), overwrite = true)
                                }

                                exportDir.listFiles()
                                    ?.forEach {
                                        if (it.name == "BufferValues" || it.extension == "exe")
                                            return@forEach

                                        if (it.name !in selected.map { it.fileName }) {
                                            it.deleteRecursively()
                                        }
                                    }
                            }.invokeOnCompletion { loading = false }
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
                    Spacer(Modifier.width(22.dp))
                    ExtendedFloatingActionButton(
                        onClick = { currentDialog = Dialog.AddMod },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "Add"
                            )
                        },
                        text = {
                            Text("Add Mod")
                        }
                    )
                }
            },
        ) { paddingValues ->
            CharacterToggleList(
                modifier = Modifier.fillMaxSize(),
                paddingValues = paddingValues,
                charactersWithMods = characters
            )
        }
        val dismiss = { currentDialog = null }
        when(currentDialog) {
            null -> Unit
            Dialog.AddMod -> {
                CreateModDialog(
                    onDismissRequest = dismiss,
                    characters = derivedStateOf { characters.map { it.first } }.value,
                    createMod = { file, character ->
                        dismiss()
                        scope.launch(Dispatchers.IO) {
                            try {
                                val modFile = File(
                                    CharacterSync.rootDir.path + File.separator + character.name + File.separator + file.name
                                )
                                file.copyRecursively(modFile, overwrite = true)
                                syncTrigger.send(Unit)
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
    }
}