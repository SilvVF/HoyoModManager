package dialog

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
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import com.seiko.imageloader.rememberImagePainter
import core.model.Character
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import java.io.File

@Composable
fun CreateModDialog(
    onDismissRequest: () -> Unit,
    createMod: (dir: File, character: Character) -> Unit,
    characters: List<Character>,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        Card {
            Column(modifier = Modifier.fillMaxSize(0.8f), horizontalAlignment = Alignment.CenterHorizontally) {

                var expanded by remember { mutableStateOf(false) }
                var selectedIndex by remember { mutableStateOf(0) }
                var selectedDir by remember { mutableStateOf<File?>(null) }

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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text(
                            text = characters[selectedIndex].name,
                            style = MaterialTheme.typography.h5,
                            modifier = Modifier.clickable(onClick = { expanded = true })
                        )
                        IconButton(
                            onClick = { expanded = true }
                        ) {
                            Icon(imageVector = Icons.Outlined.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        characters.forEachIndexed { index, character ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedIndex = index
                                    expanded = false
                                }
                            ) {
                                Text(text = character.name)
                            }
                        }
                    }
                }
                TextButton(onClick = { launcher.launch() }) {
                    Text("Select mod folder")
                }
                Text(selectedDir?.path ?: "No directory selected")
                TextButton(
                    onClick = { selectedDir?.let { createMod(it, characters[selectedIndex]) } },
                    enabled = selectedDir != null
                ) {
                    Text("Create")
                }
            }
        }
    }
}