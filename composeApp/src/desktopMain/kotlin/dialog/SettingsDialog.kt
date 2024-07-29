package dialog

import LocalDataApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import core.api.DataApi
import core.db.DB
import core.db.MetaData
import core.model.Game
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.imageResource
import java.io.File

private suspend fun updateDir(path: String, game: Game) = with(DB.prefsDao) {
    val prev = select()
    if (prev != null) {
        update(
            prev.copy(
                exportModDir = prev.exportModDir?.toMutableMap()?.apply {
                    this[game.data] = path
                }
            )
        )
    } else {
        insert(
            MetaData(mapOf(game.data to path))
        )
    }
    val dir = File(path)
    val folders = dir.listFiles()?.map { it.name } ?: emptyList()

    DB.modDao.selectAllByGame(game.data).forEach {
        if (it.fileName !in folders) {
            DB.modDao.update(it.copy(enabled = false))
        } else {
            DB.modDao.update(it.copy(enabled = true))
        }
    }
}


@Composable
fun SettingsDialog(
    onDismissRequest: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    val currentDirs by produceState(emptyMap()) {
        DB.prefsDao.observe().collectLatest { prefs ->
            value = buildMap {
                Game.entries.forEach { game ->
                    put(game, prefs?.exportModDir?.get(game.data)?.let(::File))
                }
            }
        }
    }

    var selectedGame by remember { mutableStateOf<Game?>(null) }

    val launcher = rememberDirectoryPickerLauncher(
        title = "Pick a mod dir",
    ) { directory ->
        directory?.path?.let {
            scope.launch { updateDir(it, selectedGame ?: return@launch) }
        }
    }

    val launchPicker = { game: Game ->
        selectedGame = game
        launcher.launch()
    }

    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        Card(Modifier.fillMaxSize(0.8f)) {
            Column(Modifier.padding(22.dp).verticalScroll(rememberScrollState())) {
                Text(
                    "Settings",
                    style = MaterialTheme.typography.h3,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    "Export directories",
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                currentDirs.forEach { (game, file) ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(22)).clickable { launchPicker(game) },
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${game.name}: ${file?.path ?: "not set"}", style = MaterialTheme.typography.h6)
                        IconButton(
                            onClick = { launchPicker(game) }
                        ) {
                            Icon(imageVector = Icons.Outlined.Edit, "Edit")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Divider(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}