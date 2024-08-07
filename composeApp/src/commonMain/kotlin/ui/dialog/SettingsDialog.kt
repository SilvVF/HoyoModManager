package ui.dialog

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Dialog
import core.db.AppDatabase
import core.db.LocalDatabase
import core.db.Prefs
import core.db.prefs.getAndSet
import core.model.Game
import core.model.Game.*
import core.rememberDirectoryPickerLauncher
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

private suspend fun updateDir(path: String, game: Game) = AppDatabase.instance.query {

    val pref = when (game) {
        Genshin -> Prefs.genshinDir()
        StarRail -> Prefs.starRailDir()
        ZZZ -> Prefs.zenlessDir()
    }

    pref.set(path)

    val dir = File(path)
    val idToFileName = runCatching {
        buildList {
            for (folder in dir.listFiles()!!) {
                val (modId, fileName) = folder.name.split("_")
                add(modId.toInt() to fileName)
            }
        }
    }
        .getOrDefault(emptyList())



    for((id, _) in idToFileName) {
        val mod = selectModById(id) ?: continue

        updateMod(mod.copy(enabled = true))
    }
}


@Composable
fun SettingsDialog(
    onDismissRequest: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val database = LocalDatabase.current

    val currentDirs by produceState(emptyMap()) {
        with(Prefs) {
            kotlinx.coroutines.flow.combine(
                genshinDir().changes(),
                zenlessDir().changes(),
                starRailDir().changes()
            ) { genshin, zenless, honkai ->
                value = mapOf(
                    Genshin to genshin,
                    ZZZ to zenless,
                    StarRail to honkai
                )
            }
        }
    }

    val ignored by produceState(emptyList()) {
        Prefs.ignoreOnGeneration().changes().collect {
            value = it.toList()
        }
    }

    var selectedGame by remember { mutableStateOf<Game?>(null) }


    val ignoreLauncher = rememberDirectoryPickerLauncher(
        title = "select a dir to ignore",
    ) { directory ->
        directory?.path?.let {
            scope.launch {
                Prefs.ignoreOnGeneration().getAndSet { pref ->
                    pref + it
                }
            }
        }
    }

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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    "Export directories",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                currentDirs.forEach { (game, file) ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(22)).clickable { launchPicker(game) },
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${game.name}: ${file.ifBlank { "not set" }}", style = MaterialTheme.typography.titleSmall)
                        IconButton(
                            onClick = { launchPicker(game) }
                        ) {
                            Icon(imageVector = Icons.Outlined.Edit, "Edit")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22)).clickable { ignoreLauncher.launch() },
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Ignore directories on generation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = { ignoreLauncher.launch() }
                    ) {
                        Icon(imageVector = Icons.Outlined.Edit, "Edit")
                    }
                }
                Column {
                    ignored.fastForEach {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                it,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall
                            )
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        Prefs.ignoreOnGeneration().getAndSet { pref ->
                                            pref - it
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}