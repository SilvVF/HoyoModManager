import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.NavigationRail
import androidx.compose.material.NavigationRailItem
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import core.api.DataApi
import core.api.GenshinApi
import core.api.StarRailApi
import core.api.ZZZApi
import core.db.DB
import core.model.Character
import core.model.Game
import core.model.Game.*
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.NetHelper.client
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.AppTheme
import ui.CharacterToggleList
import ui.LocalDataApi
import ui.dialog.CreateModDialog
import ui.dialog.SettingsDialog
import java.io.File
import java.nio.file.Paths


sealed interface SyncRequest {
    data object Startup: SyncRequest
    data class UserInitiated(val network: Boolean): SyncRequest
}

sealed interface Dest {
    data object Playlist: Dest
    data class ModList(val game: Game): Dest
}

@Composable
@Preview
fun App() {
    AppTheme {

        var dest by remember { mutableStateOf<Dest>(Dest.ModList(Genshin)) }

        Row(Modifier.fillMaxSize()) {
            NavigationRail {
                Game.entries.fastForEach { game ->
                    NavigationRailItem(
                        selected = when (val d = dest) {
                            is Dest.ModList -> game == d.game
                            Dest.Playlist -> false
                        },
                        onClick = { dest = Dest.ModList(game) },
                        label = { Text(game.name) },
                        icon = { game.UiIcon() }
                    )
                }
                NavigationRailItem(
                    selected = dest is Dest.Playlist,
                    onClick = { dest = Dest.Playlist },
                    label = { Text("Playlists") },
                    icon = { Icon(imageVector = Icons.Outlined.PlayArrow, null) }
                )
            }
            AnimatedContent(dest, Modifier.fillMaxSize(), { fadeIn() togetherWith fadeOut() }) { targetDest ->
                when (targetDest) {
                    is Dest.ModList ->  CompositionLocalProvider(
                        LocalDataApi provides remember(targetDest.game) {
                            when (targetDest.game) {
                                Genshin -> GenshinApi
                                StarRail -> StarRailApi
                                ZZZ -> ZZZApi
                            }
                        }
                    ) {
                        GameModListScreen(
                            modifier = Modifier.fillMaxSize(),
                            selectedGame = targetDest.game,
                        )
                    }
                    Dest.Playlist -> PlaylistScreen(Modifier.fillMaxSize())
                }
            }
        }
    }
}



@Composable
fun GenerateButton(
    modifier: Modifier = Modifier,
    scope: CoroutineScope = rememberCoroutineScope(),
    dataApi: DataApi = LocalDataApi.current
) {
    var loading by remember { mutableStateOf(false) }

    val generateFiles = { copyAll: Int ->
        val copy = copyAll != 0
        loading = true
        scope.launch(NonCancellable + Dispatchers.IO) {

            val exportDir = File(DB.prefsDao.select()?.exportModDir?.get(dataApi.game.data) ?: return@launch)
            val selected = DB.modDao.selectEnabledForGame(dataApi.game.data)

            val ignore = DB.prefsDao.select()?.keepFilesOnClear.orEmpty()

            exportDir.listFiles()?.forEach { file ->
                when {
                    file.name == "BufferValues" -> Unit
                    file.extension == "exe" -> Unit
                    file.isFile -> Unit
                    ignore.contains(file.path) -> Unit
                    else -> file.deleteRecursively()
                }
            }

            selected.forEach { mod ->
                val modFile = Paths.get(CharacterSync.rootDir.path, dataApi.game.name, mod.character, mod.fileName).toFile()

                if (modFile.exists() && !copy)
                    return@forEach

                modFile.copyRecursively(
                    File(exportDir, "${mod.id}_${mod.fileName}"), overwrite = true
                )
            }
        }.invokeOnCompletion { loading = false }
    }

    repeat(2) {
        ExtendedFloatingActionButton(
            modifier = modifier,
            shape = if(it == 0) {
                RoundedCornerShape(
                    topStart = 50f,
                    bottomStart = 50f
                )
            } else {
                RoundedCornerShape(topEnd = 50f, bottomEnd = 50f)
            },
            onClick = {
                if (loading)
                    return@ExtendedFloatingActionButton

                generateFiles(it)
            },
            icon = {
                if(it != 0)
                    return@ExtendedFloatingActionButton

                if (loading) {
                    CircularProgressIndicator()
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Generate"
                    )
                }
            },
            backgroundColor = MaterialTheme.colors.secondary.copy(
                alpha = if(loading) 0.5f else 1f
            ),
            text = {
                if (it == 0) {
                    Text("Reload")
                } else {
                    Text("Generate")
                }
            }
        )
    }
}