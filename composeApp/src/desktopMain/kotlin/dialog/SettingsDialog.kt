package dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import com.seiko.imageloader.rememberImagePainter
import core.db.DB
import core.db.Prefs
import core.model.Character
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
                    insert(Prefs(directory?.path))
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