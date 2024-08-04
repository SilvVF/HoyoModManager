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
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition
import core.api.DataApi
import core.api.GameBananaApi
import core.api.GenshinApi
import core.api.StarRailApi
import core.api.ZZZApi
import core.db.DB
import core.model.Game
import core.model.Game.*
import core.model.Playlist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.AppTheme
import ui.LocalDataApi
import java.io.File
import java.nio.file.Paths
import kotlin.reflect.KClass


sealed interface SyncRequest {
    data object Startup: SyncRequest
    data class UserInitiated(val network: Boolean): SyncRequest
}

@Composable
@Preview
fun App() {
    AppTheme {
        Navigator(screen = GameScreen(Genshin)) { navigator ->
            Row(Modifier.fillMaxSize()) {
                NavigationRail {
                    Game.entries.fastForEach { game ->
                        NavigationRailItem(
                            selected = when (val screen = navigator.lastItemOrNull) {
                                is GameScreen -> game == screen.game
                                else -> false
                            },
                            onClick = {
                                val found = navigator.popUntil {
                                    val screenGame  = (it as? GameScreen)?.game ?: return@popUntil false
                                    screenGame == game
                                }
                                if (!found) {
                                    navigator.push(GameScreen(game))
                                }
                            },
                            label = { Text(game.name) },
                            icon = { game.UiIcon() }
                        )
                    }
                    NavigationRailItem(
                        selected = navigator.lastItemOrNull is PlaylistScreen,
                        onClick = {
                            val found = navigator.popUntil { it is PlaylistScreen }
                            if (!found) {
                                navigator.push(PlaylistScreen())
                            }
                        },
                        label = { Text("Playlists") },
                        icon = { Icon(imageVector = Icons.Outlined.PlayArrow, null) }
                    )
                    NavigationRailItem(
                        selected = navigator.lastItemOrNull is ModBrowseScreen,
                        onClick = {
                            val found = navigator.popUntil { it is ModBrowseScreen }
                            if (!found) {
                                navigator.push(ModBrowseScreen())
                            } else {
                                (navigator.lastItem as? ReselectTab)?.onReselect()
                            }
                        },
                        label = { Text("Browse mods") },
                        icon = { Icon(imageVector = Icons.Outlined.AccountBox, null) }
                    )
                }
                FadeTransition(navigator)
            }
        }
    }
}


class PlaylistScreen: Screen {

    @Composable
    override fun Content() {

        PlaylistScreen(Modifier.fillMaxSize())
    }
}

class GameScreen(val game: Game): Screen {

    override val key: ScreenKey
        get() = super.key + game.name

    @Composable
    override fun Content() {
        CompositionLocalProvider(
            LocalDataApi provides remember(game) {
                when(game) {
                    Genshin -> GenshinApi
                    StarRail -> StarRailApi
                    ZZZ -> ZZZApi
                }
            }
        ) {
            GameModListScreen(game, Modifier.fillMaxSize())
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