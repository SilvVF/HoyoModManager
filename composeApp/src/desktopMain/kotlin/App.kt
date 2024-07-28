import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.ChipDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.IconToggleButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.seiko.imageloader.rememberImagePainter
import core.db.DB
import core.db.ModEntity
import dialog.CreateModDialog
import dialog.SettingsDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.io.File

sealed interface Dialog {
    data object Settings: Dialog
    data object AddMod: Dialog
}

fun <T, E> Flow<T>.combineToPair(flow2: Flow<E>): Flow<Pair<T, E>> = this.combine(flow2) { a, b -> a to b }

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
@Preview
fun App() {
    MaterialTheme {

        val scope = rememberCoroutineScope()
        val filters = remember { mutableStateListOf<String>() }

        val characters by produceState(emptyList()) {
            CharacterSync.stats.combineToPair(snapshotFlow { filters.toList() }).collect { (chars, filters) ->
                val full = chars.toList()
                value = if (filters.isEmpty())
                    full
                else
                    full.groupBy { it.first.element }
                        .flatMap { if (it.key !in filters) emptyList() else it.value }
            }
        }

        val syncActive by produceState(true) {
            val job = CharacterSync.sync()
            runCatching { job.join() }
            value = false
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
            },
        ) { paddingValues ->
            Box {
                val lazyGridState = rememberLazyGridState()

                LazyVerticalGrid(
                    state = lazyGridState,
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Adaptive(248.dp),
                    contentPadding = paddingValues
                ) {
                    if (syncActive) {
                        item(
                            span = { GridItemSpan(maxCurrentLineSpan) }
                        ) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(64.dp))
                            }
                        }
                    }
                    items(characters) { (character, files) ->
                        var expanded by remember { mutableStateOf(false) }

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CharacterCard(
                                character = character,
                                modifier = Modifier.fillMaxWidth()
                                    .padding(12.dp)
                                    .aspectRatio(3f / 4f)
                                    .clickable { expanded = !expanded },
                            )
                            AnimatedVisibility(expanded) {
                                Column {
                                    if (files.isEmpty()) {
                                        Text("No mods")
                                    } else {
                                        files.forEach {
                                            Row {
                                                Text(
                                                    text = it,
                                                    maxLines = 1,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Switch(
                                                    checked = false,
                                                    onCheckedChange = {},
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(scrollState = lazyGridState)
                )
            }
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
                                    CharacterSync.rootDir.path + File.separator + character.name + File.separator +
                                            file.path.takeLastWhile { it.isWhitespace() || it.isLetter() || it.isDigit() }
                                )
                                file.copyTo(modFile, overwrite = true)
                                CharacterSync.sync()
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