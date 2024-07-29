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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Card
import androidx.compose.material.Chip
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import core.db.DB
import core.db.ModWithTags
import core.model.Character
import core.model.Game
import core.model.Tag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlin.math.exp

@Composable
fun CharacterToggleList(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    charactersWithMods: List<Pair<Character, List<String>>>,
    game: Game,
) {
    val lazyGridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()


    Box(modifier) {
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
                        if (maxWidth < 400.dp) {
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
                                    fileNames = files,
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                                    scope = scope,
                                    game = game
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
                                    scope = scope,
                                    game = game
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FileToggles(
    fileNames: List<String>,
    game: Game,
    modifier: Modifier = Modifier,
    scope: CoroutineScope = rememberCoroutineScope(),
) {
    if (fileNames.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            Text("No mods", Modifier.align(Alignment.Center))
        }
        return
    }

    val lazyColState = rememberLazyListState()

    val mods by produceState<List<ModWithTags>>(emptyList()) {
        snapshotFlow { fileNames }.flatMapLatest { files ->
            DB.modDao.observeModsWithTags(files,  game.data)
        }.collect { modsWithTags ->
            value = modsWithTags
        }
    }

    Box(modifier) {
        LazyColumn(Modifier.fillMaxSize(), state = lazyColState) {
            mods.fastForEach { (mod, tags) ->
                item(key = mod.fileName) {

                    val checked by produceState(false) {
                        DB.modDao.observeByFileName(mod.fileName).collect { value = it?.enabled ?: false }
                    }
                    var dropDownExpanded by remember { mutableStateOf(false) }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mod.fileName,
                                modifier = Modifier.weight(1f)
                            )
                            Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                                IconButton(
                                    onClick = { dropDownExpanded = !dropDownExpanded },
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Menu,
                                        contentDescription = null
                                    )
                                }
                                DropdownMenu(
                                    expanded = dropDownExpanded,
                                    onDismissRequest = { dropDownExpanded = false},
                                ) {
                                    IconButton(
                                        onClick = { },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = null
                                        )
                                    }
                                    IconButton(
                                        onClick = {},
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Edit,
                                            contentDescription = null
                                        )
                                    }
                                    IconButton(
                                        onClick = {},
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Add,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                            Switch(
                                checked = checked,
                                onCheckedChange = {
                                    scope.launch(Dispatchers.IO) {
                                        toggleModEnabled(fileName = mod.fileName, enabled = it)
                                    }
                                },
                            )
                        }
                        Box {
                            val lazyRowState = rememberLazyListState()
                            LazyRow(
                                Modifier.fillMaxWidth(),
                                state = lazyRowState,
                                contentPadding = PaddingValues(horizontal = 32.dp)
                            ) {
                                items(
                                    listOf(
                                        Tag("dkjalksdf", "kdjfklajdfl"),
                                        Tag("dkjasdf", "kdjfajdfl"),
                                        Tag("dkjalksdf", "jfklajdfl"),
                                        Tag("dkksdf", "kdjajdfl"),
                                        Tag("dkjaksdf", "kdjfkldfl"),
                                    ),
                                    key = { it.name + it.fileName }
                                ) { tag ->
                                    Chip(
                                        onClick = {},
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    ) {
                                        Text(tag.name)
                                    }
                                }
                            }
                            val iconButton = @Composable { onClick: suspend () -> Unit, icon: ImageVector, align: Alignment ->
                                IconButton(
                                    onClick = {
                                        scope.launch { onClick() }
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .align(align),
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null
                                    )
                                }
                            }
                            iconButton(
                                {
                                    val idx = (lazyRowState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                                    lazyRowState.animateScrollToItem(idx)
                                },
                                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                Alignment.CenterStart
                            )
                            iconButton(
                                {
                                    val idx = (lazyRowState.firstVisibleItemIndex + 1)
                                    lazyRowState.animateScrollToItem(idx)
                                },
                                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                Alignment.CenterEnd
                            )
                        }
                    }
                    Divider(Modifier.fillMaxWidth())
                }
            }
        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(scrollState = lazyColState),
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