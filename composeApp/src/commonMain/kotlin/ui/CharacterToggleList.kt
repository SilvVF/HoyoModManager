package ui

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.util.fastForEach
import core.db.DB
import core.model.Character
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun CharacterToggleList(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    charactersWithMods: List<Pair<Character, List<String>>>,
) {
    val lazyGridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()


    Box {
        LazyVerticalGrid(
            state = lazyGridState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            columns = GridCells.Fixed(3),
            contentPadding = paddingValues
        ) {
            items(charactersWithMods, key = { it.first.id }) { (character, files) ->

                var expanded by rememberSaveable { mutableStateOf(false) }

                val onBackground = remember { Color(0xff030712) }
                val typeColor = remember { Color(0xff111827) }

                Card(Modifier.padding(8.dp)) {
                    BoxWithConstraints(Modifier.background(Brush.verticalGradient(listOf(onBackground, typeColor)))) {
                        if (maxWidth < 350.dp) {
                            Column(Modifier.fillMaxWidth()) {
                                CharacterImage(
                                    character = character,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(3f / 4f)
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) { expanded = !expanded },
                                )
                                FileToggles(
                                    fileNames = buildList { repeat(4) { addAll(files) } },
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                                    scope = scope
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                CharacterImage(
                                    character = character,
                                    modifier = Modifier
                                        .aspectRatio(3f / 4f)
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) { expanded = !expanded },
                                )
                                FileToggles(
                                    fileNames = files,
                                    modifier = Modifier.fillMaxSize(),
                                    scope = scope
                                )
                            }
                        }
                    }
                }
            }
        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(scrollState = lazyGridState),
            style = LocalScrollbarStyle.current.copy(
                thickness = 8.dp,
                hoverColor = MaterialTheme.colors.primary,
                unhoverColor = MaterialTheme.colors.primary
            )
        )
    }
}

@Composable
fun FileToggles(
    fileNames: List<String>,
    modifier: Modifier = Modifier,
    scope: CoroutineScope = rememberCoroutineScope()
) {
    if (fileNames.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            Text("No mods", Modifier.align(Alignment.Center))
        }
        return
    }

    val lazyListState = rememberLazyListState()

    Box(modifier) {
        LazyColumn(Modifier.fillMaxSize(), state = lazyListState) {
            fileNames.fastForEach { file ->
                item(key = file) {
                    val checked by produceState(false) {
                        DB.modDao.observeByFileName(file).collect { value = it?.enabled ?: false }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = file,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = checked,
                            onCheckedChange = {
                                scope.launch(Dispatchers.IO) {
                                    toggleModEnabled(fileName = file, enabled = it)
                                }
                            },
                        )
                    }
                    Divider(Modifier.fillMaxWidth())
                }
            }
        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(scrollState = lazyListState),
            style = LocalScrollbarStyle.current.copy(
                thickness = 8.dp,
                hoverColor = MaterialTheme.colors.primary,
                unhoverColor = MaterialTheme.colors.primary
            )
        )
    }
}

private suspend fun toggleModEnabled(fileName: String, enabled: Boolean) = with(DB.modDao) {
    selectByFileName(fileName)?.let { mod -> update(mod.copy(enabled = enabled)) }
}