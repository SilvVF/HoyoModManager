import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.NavigationRail
import androidx.compose.material.NavigationRailItem
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import core.api.DataApi
import core.api.GenshinApi
import core.api.StarRailApi
import core.api.ZZZApi
import core.db.DB
import core.model.Game
import core.model.Game.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.AppTheme
import ui.LocalDataApi
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
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
                    else -> {
                        runCatching {
                            val (id, filename) = file.name.split("_")
                            when  {
                                selected.find { it.id == id.toInt() }?.fileName != filename ->
                                    file.deleteRecursively()
                                else -> Unit
                            }
                        }
                            .onFailure { file.deleteRecursively() }
                    }
                }
            }

            selected.forEach { mod ->
                runCatching {
                    val modFile = Paths.get(CharacterSync.rootDir.path, dataApi.game.name, mod.character, mod.fileName).toFile()

                    modFile.copyRecursively(
                        File(exportDir, "${mod.id}_${mod.fileName}"), overwrite = copy
                    )
                }
            }



            if (dataApi.game == Genshin) {
                 val exeFix = exportDir.listFiles()?.find { it.isFile && it.extension == "exe" } ?: return@launch
                 Runtime.getRuntime().exec(
                    "cmd.exe /c cd ${exportDir.path} && start ${exeFix.name}",
                    null,
                    File(CharacterSync.rootDir.path)
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