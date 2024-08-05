package tab.playlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.seiko.imageloader.ui.AutoSizeImage
import core.db.LocalDatabase
import core.model.Character
import core.model.Game
import core.model.Mod
import core.model.ModWithTags
import core.model.Playlist
import core.model.PlaylistModCrossRef
import core.model.PlaylistWithModsAndTags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import ui.TagsList
import ui.draggableXY
import ui.widget.ChangeTextPopup
import kotlin.random.Random

private sealed interface PlaylistDialog {
    data object LoadPlaylist: PlaylistDialog
    data class CreatePlaylist(val mods: List<Mod>): PlaylistDialog
    data class RenamePlaylist(val playlist: Playlist): PlaylistDialog
}

@Composable
fun PlaylistScreen(
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val game by remember { mutableStateOf(Game.Genshin) }
    var currentDialog by remember { mutableStateOf<PlaylistDialog?>(null) }
    val database = LocalDatabase.current

    val mods by produceState<List<ModWithTags>>(emptyList()) {
        snapshotFlow { game }
            .flatMapLatest {
                database.observeAllModsWithTags(game.data)
            }
            .collect { modsWithTags ->
                value = modsWithTags
            }
    }

    val playlists by produceState(emptyList()) {
        snapshotFlow { game }
            .flatMapLatest {
               database.subscribeToPlaylistsWithModsAndTags(game)
            }
            .collect { playlistWithMods ->
                value = playlistWithMods.map { (playlist, modsWithTags) ->
                    PlaylistWithModsAndTags(
                        playlist,
                        mods = modsWithTags.map { (mod, tags) ->
                            ModWithTags(mod, tags)
                        }
                    )
                }
            }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            Row {
                ExtendedFloatingActionButton(
                    text = { Text("Load") },
                    icon = { Icon(imageVector = Icons.Outlined.Refresh, null) },
                    onClick = { currentDialog = PlaylistDialog.LoadPlaylist },
                )
                Spacer(Modifier.width(22.dp))
                ExtendedFloatingActionButton(
                    text = { Text("Save") },
                    icon = { Icon(imageVector = Icons.Outlined.Create, null) },
                    onClick = {
                        currentDialog = PlaylistDialog.CreatePlaylist(
                            mods.map { it.mod }.filter { it.enabled }
                        )
                    },
                )
            }
        }
    ) { paddingValues ->
        PlaylistMods(
            paddingValues,
            mods,
            Modifier.fillMaxSize()
        )
        when (val dialog = currentDialog) {
            null -> Unit
            PlaylistDialog.LoadPlaylist -> {
                Dialog(
                    onDismissRequest = { currentDialog = null },
                ) {
                    Card(Modifier.fillMaxSize(0.9f)) {
                        if (playlists.isEmpty()) {
                            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No playlists saved.", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.size(22.dp))
                                Button(onClick = {}) {
                                    Text("Create playlist")
                                }
                            }
                            return@Card
                        }
                        LazyColumn(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                            items(playlists) { (playlist, mods) ->
                                Spacer(Modifier.height(6.dp))
                                Card(Modifier.fillMaxWidth(0.9f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val modNames by remember {
                                            derivedStateOf { mods.joinToString { it.mod.fileName } }
                                        }

                                        Column(Modifier.weight(1f).padding(12.dp)) {
                                            Text(playlist.name, style = MaterialTheme.typography.titleSmall)
                                            Spacer(Modifier.width(12.dp))
                                            Text(modNames, style = MaterialTheme.typography.labelMedium)
                                        }
                                        IconButton(
                                            onClick = {
                                                currentDialog =
                                                    PlaylistDialog.RenamePlaylist(playlist)
                                            }
                                        ) {
                                            Icon(Icons.Outlined.Edit, null)
                                        }
                                        IconButton(
                                            onClick = {
                                                database.launchQuery(scope) {
                                                    delete(playlist)
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Outlined.Delete, null)
                                        }
                                        IconButton(
                                            onClick = {
                                                scope.launch(Dispatchers.IO) {
                                                   database.enableAndDisable(enabled = mods.map { it.mod.id }, game)
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Outlined.Add, null)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is PlaylistDialog.CreatePlaylist -> {
                var text by remember { mutableStateOf("") }
                var offset by remember { mutableStateOf(IntOffset.Zero)}
                Popup(
                    offset = offset,
                    properties = PopupProperties(
                        focusable = true
                    ),
                    onPreviewKeyEvent = { false }, onKeyEvent = { false }) {
                    ChangeTextPopup(
                        value = text,
                        modifier = Modifier.draggableXY { intOffset ->
                            offset = intOffset
                        },
                        onValueChange = { text = it },
                        message = { Message("Create a name for the playlist.") },
                        onConfirm = {
                            database.launchQuery(scope) {
                                try {
                                    val enabled = selectEnabledForGame(game.data)

                                    val id = insert(
                                        Playlist(name = text, game = game)
                                    )

                                    insertAll(
                                        enabled.map { PlaylistModCrossRef(id.toInt(), it.id) }
                                    )
                                } catch (_: Exception) {
                                }
                            }
                            currentDialog = null
                        },
                        onCancel = { currentDialog = null }
                    )
                }
            }

            is PlaylistDialog.RenamePlaylist -> {
                var text by remember { mutableStateOf("") }
                var offset by remember { mutableStateOf(IntOffset.Zero)}
                Popup(
                    offset = offset,
                    properties = PopupProperties(
                        focusable = true
                    ),
                    onPreviewKeyEvent = { false }, onKeyEvent = { false }
                ) {
                    ChangeTextPopup(
                        value = text,
                        modifier = Modifier.draggableXY { intOffset ->
                            offset = intOffset
                        },
                        onValueChange = { text = it },
                        message = { Message("rename the playlist.") },
                        onConfirm = {
                            scope.launch(Dispatchers.IO) {
                                database.query {
                                    update(dialog.playlist.copy(name = text))
                                }
                            }
                            currentDialog = null
                        },
                        onCancel = { currentDialog = null }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistMods(
    paddingValues: PaddingValues,
    mods: List<ModWithTags>,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val database = LocalDatabase.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = paddingValues,
        modifier = modifier.fillMaxSize()
    ) {
        items(mods, { it.mod.id }) { (mod, tags) ->
            val bg by animateColorAsState(
                targetValue = if (mod.enabled)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else
                    Color.Transparent
            )
            Column(
                modifier = Modifier.background(
                    color = bg
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val character by produceState<Character?>(null) {
                        value = database.selectById(mod.characterId, Game.fromByte(mod.game))
                    }
                    val gradientColor = remember(character) {
                        val random = Random(character.hashCode())
                        Color(
                            random.nextInt(255),
                            random.nextInt(255),
                            random.nextInt(255),
                        )
                    }
                    AnimatedVisibility(character != null, Modifier.size(64.dp)) {
                        AutoSizeImage(
                            url = character?.avatarUrl ?: "",
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, gradientColor))
                                ),
                            contentScale = ContentScale.FillWidth,
                            contentDescription = null
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = mod.fileName,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = mod.enabled,
                        onCheckedChange = {
                            scope.launch(Dispatchers.IO) {
                                database.query {
                                    update(mod.copy(enabled = !mod.enabled))
                                }
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