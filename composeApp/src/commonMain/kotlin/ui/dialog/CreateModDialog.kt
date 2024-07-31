package ui.dialog

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.window.Dialog
import com.seiko.imageloader.rememberImagePainter
import core.renameFolder
import core.model.Character
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import kotlinx.coroutines.launch
import ui.widget.ChangeTextPopup
import java.io.File

private sealed interface SelectionState {
    data object Default: SelectionState
    data object SelectingCharacter: SelectionState
    data class  Renaming(val file: File): SelectionState
}

@Composable
fun CreateModDialog(
    onDismissRequest: () -> Unit,
    createMod: (dir: File, character: Character) -> Unit,
    characters: List<Character>,
    initialCharacter: Character? = null
) {
    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        Card {
            Column(modifier = Modifier.fillMaxSize(0.8f), horizontalAlignment = Alignment.CenterHorizontally) {

                val scope = rememberCoroutineScope()
                var expanded by remember { mutableStateOf(false) }
                var selectedIndex by remember {
                    mutableStateOf(
                        initialCharacter?.let {
                            characters.indexOfFirst { it.id == initialCharacter.id }
                        } ?: 0
                    )
                }
                var selectedDir by remember { mutableStateOf<File?>(null) }
                var selectionState by remember { mutableStateOf<SelectionState>(SelectionState.Default) }

                val painter = rememberImagePainter(characters[selectedIndex].avatarUrl)

                // FileKit Compose
                val launcher = rememberDirectoryPickerLauncher(
                    title = "Pick a mod dir",
                ) { directory ->
                    // Handle the picked directory
                    selectedDir = directory?.file
                }

                Image(
                    painter = painter,
                    modifier = Modifier
                        .fillMaxHeight(0.5f)
                        .aspectRatio(3 / 4f),
                    contentDescription = "image",
                    contentScale = ContentScale.Fit,
                )

                Box(modifier = Modifier.wrapContentSize(Alignment.Center)) {
                    val character = characters.getOrNull(selectedIndex) ?: return@Box
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = character.name,
                            style = MaterialTheme.typography.h5,
                            modifier = Modifier.clickable(onClick = { expanded = true })
                        )
                        IconButton(
                            onClick = {
                                selectionState = when(selectionState) {
                                    SelectionState.SelectingCharacter -> SelectionState.Default
                                    else -> SelectionState.SelectingCharacter
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Outlined.ArrowDropDown, contentDescription = null)
                        }
                    }
                }
                AnimatedContent(selectionState, Modifier.weight(1f)) { state ->
                    when (state) {
                        SelectionState.SelectingCharacter -> {
                            LazyColumn(Modifier.fillMaxWidth()) {
                                characters.fastForEachIndexed { index, character ->
                                    item(key = character.name) {
                                        DropdownMenuItem(
                                            onClick = {
                                                selectedIndex = index
                                                expanded = false
                                            },
                                        ) {
                                            Text(text = character.name)
                                        }
                                    }
                                }
                            }
                        }
                        SelectionState.Default -> {
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                TextButton(onClick = { launcher.launch() }) {
                                    Text("Select mod folder")
                                }
                                Text(selectedDir?.path ?: "No directory selected")
                                if (selectedDir != null) {
                                    TextButton(
                                        onClick = { selectionState =
                                            SelectionState.Renaming(selectedDir!!)
                                        }
                                    ) {
                                        Text("Rename")
                                    }
                                }
                            }
                        }
                        is SelectionState.Renaming -> {
                            var text by remember { mutableStateOf(state.file.name) }
                            ChangeTextPopup(
                                value = text,
                                surfaceColor = Color.Transparent,
                                message = { Message("Rename the mod folder.") },
                                onValueChange = { text = it },
                                onCancel = { selectionState = SelectionState.Default },
                                onConfirm = {
                                    scope.launch {
                                        renameFolder(state.file, text)
                                            .onSuccess { renamed ->
                                                selectedDir = renamed
                                            }
                                    }
                                    selectionState = SelectionState.Default
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                Button(
                    onClick = {
                        selectedDir?.let {
                            createMod(it, characters.getOrNull(selectedIndex) ?: return@let)
                        }
                    },
                    enabled = selectedDir != null
                ) {
                    Text("Create")
                }
            }
        }
    }
}