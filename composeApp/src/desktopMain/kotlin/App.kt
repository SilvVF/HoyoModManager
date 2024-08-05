import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.core.screen.ScreenKey
import core.api.DataApi
import core.db.DB
import core.model.Game
import core.model.Game.Genshin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import lib.voyager.Tab
import lib.voyager.TabDisposable
import lib.voyager.TabNavigator
import org.jetbrains.compose.ui.tooling.preview.Preview
import tab.ReselectTab
import tab.game.GameTab
import tab.mod.ModTab
import tab.playlist.PlaylistTab
import ui.AppTheme
import ui.LocalDataApi
import java.io.File
import java.nio.file.Paths


sealed interface SyncRequest {
    data object Startup: SyncRequest
    data class UserInitiated(val network: Boolean): SyncRequest
}

private val gameTabs = Game.entries.map { game ->
    object : GameTab {

        override val key: ScreenKey = game.name

        override val game: Game = game
    }
}

private val otherTabs = listOf(
    PlaylistTab,
    ModTab
)

private val tabs = otherTabs + gameTabs

@Composable
@Preview
fun App() {
    AppTheme {
        TabNavigator(
            tab = gameTabs.first(),
            tabDisposable = {
                TabDisposable(it, tabs)
            }
        ) { navigator ->
            Row(Modifier.fillMaxSize()) {
                NavigationRail {

                    val handleSelect = { tab: Tab ->

                        if (navigator.current == tab && tab is ReselectTab) {
                            tab.onReselect()
                        } else {
                            navigator.current = tab
                        }
                    }

                    gameTabs.fastForEach { tab ->
                        NavigationRailItem(
                            selected = when (val screen = navigator.current) {
                                is GameTab -> tab.game == screen.game
                                else -> false
                            },
                            onClick = { handleSelect(tab) },
                            label = { Text(tab.game.name) },
                            icon = { tab.Icon() }
                        )
                    }
                    otherTabs.fastForEach { tab ->
                        NavigationRailItem(
                            selected = navigator.current == tab ,
                            onClick = { handleSelect(tab) },
                            label = { Text(tab.toString()) },
                            icon = { tab.Icon() }
                        )
                    }
                }
                Scaffold(
                    topBar = {

                    }
                ) {
                    lib.voyager.FadeTransition(navigator)
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
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
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