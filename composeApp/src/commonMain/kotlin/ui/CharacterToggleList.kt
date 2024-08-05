package ui

import CharacterSync
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import core.FileUtils
import core.db.DB
import core.model.Character
import core.model.CharacterWithModsAndTags
import core.model.Game
import core.model.Mod
import core.model.ModWithTags
import core.model.Tag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import ui.widget.ChangeTextPopup
import java.nio.file.Paths
import kotlin.math.roundToInt

@Composable
fun CharacterToggleList(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    characters: List<CharacterWithModsAndTags>,
    onCharacterIconClick: (character: Character) -> Unit,
    game: Game,
) {
    val lazyGridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    Box(modifier) {
        LazyVerticalGrid(
            state = lazyGridState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            columns = GridCells.Adaptive(500.dp),
            contentPadding = paddingValues
        ) {
            items(characters, key = { it.character.id  }) { (character, mods) ->

                val onBackground = remember { Color(0xff030712) }
                val typeColor = remember { Color(0xff111827) }

                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    BoxWithConstraints(
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .background(
                                Brush.verticalGradient(listOf(onBackground, typeColor))
                            )
                    ) {
                        if (maxWidth > 450.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.Top
                            ) {
                                CharacterImage(
                                    character = character,
                                    onIconClick = { onCharacterIconClick(character) },
                                    modifier = Modifier
                                        .fillMaxWidth(0.3f)
                                        .aspectRatio(3f / 4f),
                                )
                                FileToggles(
                                    mods = mods,
                                    modifier = Modifier.fillMaxSize(),
                                    scope = scope,
                                )
                            }
                        } else {
                            Column(Modifier.wrapContentHeight().fillMaxWidth()) {
                                CharacterImage(
                                    character = character,
                                    onIconClick = { onCharacterIconClick(character) },
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .height(200.dp)
                                        .aspectRatio(3f / 4f),
                                )

                                if (mods.isNotEmpty()) {
                                    FileToggles(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(450.dp),
                                        mods = mods,
                                        scope = scope,
                                    )
                                } else {
                                    Text(
                                        "No Mods",
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .height(120.dp)
                                            .align(Alignment.CenterHorizontally)
                                    )
                                }
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
                hoverColor = MaterialTheme.colorScheme.primary,
                unhoverColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun FileToggles(
    mods: List<ModWithTags>,
    modifier: Modifier = Modifier,
    scope: CoroutineScope = rememberCoroutineScope(),
) {

    if (mods.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            Text("No mods", Modifier.align(Alignment.Center))
        }
        return
    }

    val lazyColState = rememberLazyListState()
    Box(modifier) {
        LazyColumn(Modifier.fillMaxSize(), state = lazyColState) {
            mods.fastForEach { (mod, tags) ->
                item(key = mod.fileName) {

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
                            ModActionDropdownMenu(
                                mod = mod,
                                dropDownExpanded = dropDownExpanded,
                                toggleExpanded = { dropDownExpanded = !dropDownExpanded },
                            )
                            Switch(
                                checked = mod.enabled,
                                onCheckedChange = {
                                    scope.launch(Dispatchers.IO) {
                                        toggleModEnabled(fileName = mod.fileName, enabled = it)
                                    }
                                },
                            )
                        }
                        AnimatedVisibility(tags.isNotEmpty()) {
                            TagsList(
                                tags = tags,
                                scope = scope
                            )
                        }
                    }
                    HorizontalDivider(Modifier.fillMaxWidth())
                }
            }
        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(scrollState = lazyColState),
            style = LocalScrollbarStyle.current.copy(
                thickness = 8.dp,
                hoverColor = MaterialTheme.colorScheme.primary,
                unhoverColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

private sealed interface ModPopup {
    data object EditName: ModPopup
    data object Delete: ModPopup
    data object AddTag: ModPopup
}

@Composable
private fun ModActionDropdownMenu(
    mod: Mod,
    dropDownExpanded: Boolean,
    toggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val iconButton = @Composable { action: () -> Unit, icon: ImageVector ->
        IconButton(
            onClick = action,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        }
    }
    var currentPopup by remember { mutableStateOf<ModPopup?>(null) }
    val dismiss = { currentPopup = null }

    currentPopup?.let { popup ->

        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }

        Popup(
            onDismissRequest = dismiss,
            properties = PopupProperties(
                focusable = true
            ),
            onPreviewKeyEvent = { false },
            onKeyEvent = { false },
            offset = IntOffset(offsetX.roundToInt(), offsetY.roundToInt())
        ) {
            ModActionPopups(
                popup,
                mod,
                dismiss,
                scope,
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    }
            )
        }
    }

    Box(modifier = modifier.wrapContentSize(Alignment.TopStart)) {
        iconButton(
            toggleExpanded,
            Icons.Outlined.Menu
        )
        DropdownMenu(
            expanded = dropDownExpanded,
            onDismissRequest = toggleExpanded,
        ) {
            iconButton(
                {
                    currentPopup = ModPopup.EditName
                    toggleExpanded()
                },
                Icons.Outlined.Edit
            )
            iconButton(
                {
                    currentPopup = ModPopup.AddTag
                    toggleExpanded()
                },
                Icons.Outlined.Add
            )
            iconButton(
                {
                    currentPopup = ModPopup.Delete
                    toggleExpanded()
                },
                Icons.Outlined.Delete
            )
        }
    }
}

@Composable
private fun ModActionPopups(
    popup: ModPopup,
    mod: Mod,
    dismiss: () -> Unit,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    var offset by remember { mutableStateOf(IntOffset.Zero)}
    Popup(
        alignment = Alignment.Center,
        offset = offset,
        properties = PopupProperties(focusable = true),
    ) {
        Box(modifier) {
            when (popup) {
                ModPopup.AddTag -> {
                    var text by remember { mutableStateOf("") }
                    ChangeTextPopup(
                        value = text,
                        modifier = Modifier.draggableXY { offset = it },
                        message = { Message("Add a tag to this mod.") },
                        onValueChange = { text = it },
                        onCancel = dismiss,
                        onConfirm = {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    DB.tagDao.insert(Tag(mod.id, text))
                                } catch (_: Exception) {
                                }
                            }
                            dismiss()
                        }
                    )
                }

                ModPopup.Delete -> {
                    Card(
                        Modifier
                            .draggableXY { offset = it }
                            .padding(22.dp)
                    ) {
                        Column {
                            Text("this will also delete the mod file")
                            Row {
                                TextButton(
                                    onClick = dismiss
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        scope.launch(NonCancellable + Dispatchers.IO) {
                                            try {
                                                val path = Paths.get(
                                                    CharacterSync.rootDir.path,
                                                    Game.fromByte(mod.game).name,
                                                    mod.character,
                                                    mod.fileName
                                                )
                                                path.toFile().deleteRecursively()
                                                DB.modDao.delete(mod)
                                            } catch (_: Exception) {
                                            }
                                        }
                                        dismiss()
                                    }
                                ) {
                                    Text("Confirm")
                                }
                            }
                        }
                    }
                }

                ModPopup.EditName -> {

                    var name by remember { mutableStateOf(mod.fileName) }
                    val dataApi = LocalDataApi.current

                    ChangeTextPopup(
                        value = name,
                        modifier = Modifier.draggableXY { offset = it },
                        message = { Message("Rename the mod folder.") },
                        onValueChange = { name = it },
                        onCancel = dismiss,
                        onConfirm = {
                            scope.launch(NonCancellable + Dispatchers.IO) {
                                val filePath = Paths.get(
                                    CharacterSync.rootDir.path,
                                    dataApi.game.subPath,
                                    mod.character,
                                    mod.fileName
                                )
                                FileUtils.renameFolder(filePath.toFile(), name)
                                    .onFailure { it.printStackTrace() }
                                    .onSuccess { renamed ->
                                        DB.modDao.update(
                                            mod.copy(fileName = renamed.name)
                                        )
                                    }
                            }
                            dismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TagsList(
    tags: List<Tag>,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    Box {
        val lazyRowState = rememberLazyListState()
        LazyRow(
            modifier.fillMaxWidth(),
            state = lazyRowState,
            contentPadding = PaddingValues(horizontal = 32.dp)
        ) {
            items(
                tags,
                key = { it.name }
            ) { tag ->
                AssistChip(
                    onClick = {},
                    modifier = Modifier.padding(horizontal = 2.dp),
                    label = {
                        Text(tag.name)
                    }
                )
            }
        }
        val iconButton =
            @Composable { onClick: suspend () -> Unit, icon: ImageVector, align: Alignment ->
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

private suspend fun toggleModEnabled(fileName: String, enabled: Boolean) = with(DB.modDao) {
    selectByFileName(fileName)?.let { mod -> update(mod.copy(enabled = enabled)) }
}