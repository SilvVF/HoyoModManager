package tab.mod.state

import CharacterSync
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import core.FileUtils
import core.api.DataApi
import core.db.AppDatabase
import core.model.Character
import core.model.Mod
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.NetHelper
import net.model.gamebanana.ModPageResponse
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.pathString

sealed interface ModDownloadState {
    data object Downloading: ModDownloadState
    data object Idle: ModDownloadState
}

data class Progress(
    val total: Long,
    val complete: Long
) {
    val frac = if(total == 0L) 0f else  complete / total.toFloat()

    companion object {
        val Zero = Progress(0, 0)
    }
}

class ModDownloadStateHolder(
    private val modFile: ModFile,
    private val dataApi: DataApi,
    private val character: Character? = null,
    private val info: ModInfoState.Success,
    private val scope: CoroutineScope,
    private val database: AppDatabase = AppDatabase.instance
) {
    /* These happen at the same time bc the file is read through and input stream when extracting*/
    val downloadProgress = MutableStateFlow(Progress.Zero)
    val unzipProgress = MutableStateFlow(Progress.Zero)

    val errors = mutableStateListOf<String>()

    var state by mutableStateOf<ModDownloadState>(ModDownloadState.Idle)

    fun download() = scope.launch {

        errors.clear()
        state = ModDownloadState.Downloading

        runCatching {
            downloadAndUpdateDB()
        }
            .onFailure {
                println(it.stackTraceToString())
                errors.add(it.localizedMessage)
            }

        state = ModDownloadState.Idle

        downloadProgress.emit(Progress.Zero)
        unzipProgress.emit(Progress.Zero)
    }

    private suspend fun downloadAndUpdateDB() = withContext(Dispatchers.IO) {
        val (file, downloaded) = modFile

        if (downloaded || character == null) return@withContext

        val downloadUrl = file.sDownloadUrl!!.replace("\\", "")
        val ext =  file.sFile!!.takeLastWhile { it != '.' }.lowercase()

        val res = NetHelper.client.get(downloadUrl) {
            onDownload { sent, length ->
                downloadProgress.emit(Progress(length ?: 0, sent))
            }
        }

        val inputStream = res.bodyAsChannel().toInputStream()

        val outputPath = Paths.get(
            CharacterSync.rootDir.path,
            dataApi.game.name,
            character.name,
            file.sFile.removeSuffix(".$ext")
        )

        val path = if (outputPath.exists()) {
            FileUtils.getNewName(outputPath.pathString)
        } else  outputPath.pathString

        val dir = File(path).also { it.mkdirs() }

        FileUtils.extractUsing7z(inputStream, dir) { total, complete ->
            unzipProgress.value = Progress(total, complete)
        }

        database.insertOrUpdate(
            buildMod(dir, dataApi, character, info.data, file)
        )
    }

    private fun buildMod(modDir: File, dataApi: DataApi, c: Character, data: ModPageResponse, file: ModPageResponse.AFile) =
        Mod(
            fileName = modDir.name,
            game = dataApi.game.data,
            character = c.name,
            characterId = c.id,
            enabled = false,
            modLink = data.sProfileUrl,
            gbId = data.idRow,
            gbDownloadLink = file.sDownloadUrl,
            gbFileName = file.sFile,
            previewImages = data.aPreviewMedia.aImages.map {
                val base =  it.sBaseUrl.replace("\\", "")
                base + '/' + (it.sFile).replace("\\", "")
            }
        )
}