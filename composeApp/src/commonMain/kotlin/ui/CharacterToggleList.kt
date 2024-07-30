package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Chip
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Menu
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import core.db.DB
import core.db.Mod
import core.db.ModWithTags
import core.model.Character
import core.model.Game
import core.model.Tag
import core.renameFolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ui.widget.ChangeTextPopup
import java.nio.file.Paths
import kotlin.math.roundToInt

@Composable
fun CharacterToggleList(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    characters: List<Character>,
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
            items(characters, key = { it.id }) { character ->

                var expanded by rememberSaveable { mutableStateOf(false) }

                val onBackground = remember { Color(0xff030712) }
                val typeColor = remember { Color(0xff111827) }

                val mods by produceState<List<ModWithTags>>(emptyList()) {
                    withContext(Dispatchers.IO) {
                        DB.modDao.observeModsWithTags(character.name, game.data)
                            .collect { modsWithTags ->
                                withContext(Dispatchers.Main) { value = modsWithTags }
                            }
                    }
                }

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
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                                    mods = mods,
                                    scope = scope,
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
                                    mods = mods,
                                    modifier = Modifier.fillMaxSize(),
                                    scope = scope,
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
                            ModActionDropdownMenu(
                                mod = mod,
                                dropDownExpanded = dropDownExpanded,
                                toggleExpanded = { dropDownExpanded = !dropDownExpanded },
                            )
                            Switch(
                                checked = checked,
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
    Box(modifier) {
        when(popup) {
            ModPopup.AddTag -> {
                var text by remember { mutableStateOf("") }
                ChangeTextPopup(
                    value = text,
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
                                dismiss()
                            }
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }

            ModPopup.EditName -> {

                var name by remember { mutableStateOf(mod.fileName) }
                val dataApi = LocalDataApi.current

                ChangeTextPopup(
                    value = name,
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
                            renameFolder(filePath.toFile(), name)
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

@Composable
private fun TagsList(
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
                Chip(
                    onClick = {},
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text(tag.name)
                }
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