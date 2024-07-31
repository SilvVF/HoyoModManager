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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.seiko.imageloader.ui.AutoSizeImage
import core.db.DB
import core.model.ModWithTags
import core.model.Character
import core.model.Game
import core.model.Mod
import core.model.PlaylistWithMods
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import ui.TagsList
import kotlin.random.Random

private sealed interface PlaylistDialog {
    data object LoadPlaylist: PlaylistDialog
}

@Composable
fun PlaylistScreen(
    modifier: Modifier = Modifier
) {
    val game by remember { mutableStateOf(Game.Genshin) }
    var currentDialog by remember { mutableStateOf<PlaylistDialog?>(null) }

    val mods by produceState<List<ModWithTags>>(emptyList()) {
        snapshotFlow { game }
            .flatMapLatest {
                DB.modDao.observeAllModsWithTags(game.data)
            }
            .collect { modsWithTags ->
                value = modsWithTags
            }
    }

    val playlists by produceState<List<PlaylistWithMods>>(emptyList()) {
        snapshotFlow { game }
            .flatMapLatest {
                DB.playlistDao.subscribeToPlaylistsWithMods(game)
            }
            .collect { playlistWithMods ->
                value = playlistWithMods
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
                    onClick = {},
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
                                Text("No playlists saved.", style = MaterialTheme.typography.h3)
                                Spacer(Modifier.size(22.dp))
                                Button(onClick = {}) {
                                    Text("Create playlist")
                                }
                            }
                            return@Card
                        }
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(playlists) { (playlist, modsWithTags) ->
                                Text(playlist.name)
                                Divider()
                            }
                        }
                    }
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
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = paddingValues,
        modifier = modifier.fillMaxSize()
    ) {
        items(mods, { it.mod.id }) { (mod, tags) ->
            val bg by animateColorAsState(
                targetValue = if (mod.enabled)
                    MaterialTheme.colors.primary.copy(alpha = 0.2f)
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
                        value = DB.characterDao.selectById(mod.characterId, Game.fromByte(mod.game))
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
                                    Brush.verticalGradient(listOf(MaterialTheme.colors.surface, gradientColor))
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
                                DB.modDao.update(mod.copy(enabled = !mod.enabled))
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