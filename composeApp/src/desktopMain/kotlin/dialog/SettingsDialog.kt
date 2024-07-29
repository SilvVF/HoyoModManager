package dialog

import LocalDataApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import core.db.DB
import core.db.MetaData
import core.model.Game
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SettingsDialog(
    onDismissRequest: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val dataApi = LocalDataApi.current

    val currentDir by produceState<File?>(null) {
        DB.prefsDao.observe().collectLatest { prefs ->
            value = File(prefs?.exportModDir ?: return@collectLatest)
        }
    }

    // FileKit Compose
    val launcher = rememberDirectoryPickerLauncher(
        title = "Pick a mod dir",
    ) { directory ->
        scope.launch(Dispatchers.IO) {
            with(DB.prefsDao) {
                val prev = select()
                if (prev != null)
                    update(prev.copy(exportModDir = directory?.path))
                else
                    insert(MetaData(directory?.path))
            }
            val dir = directory?.path?.let { File(it) } ?: return@launch
            val folders = dir.listFiles()?.map { it.name } ?: emptyList()

            DB.modDao.selectAllByGame(dataApi.game.data).forEach {
                if (it.fileName !in folders) {
                    DB.modDao.update(it.copy(enabled = false))
                } else {
                    DB.modDao.update(it.copy(enabled = true))
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        Card(Modifier.fillMaxSize(0.8f)) {
            Column(Modifier.clickable { launcher.launch() }) {
                Text("Export mod dir:")
                Text(currentDir?.path ?: "not set")
            }
        }
    }
}