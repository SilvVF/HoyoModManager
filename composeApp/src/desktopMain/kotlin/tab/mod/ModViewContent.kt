package tab.mod

import CharacterSync
import OS
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Create
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import com.seiko.imageloader.ui.AutoSizeImage
import core.FileUtils
import core.api.DataApi
import core.api.GameBananaApi
import core.db.DB
import core.model.Character
import core.model.Mod
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.NetHelper
import net.model.gamebanana.ModPageResponse
import ui.LocalDataApi
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.pathString


private data class ModState(
    val info: ModInfoState = ModInfoState.Loading,
    val character: Character? = null,
    val files: List<ModFile> = emptyList(),
)

private sealed interface ModDownloadState {
    data object Downloading: ModDownloadState
    data object Idle: ModDownloadState
}

private data class ModFile(
    val file: ModPageResponse.AFile,
    val downloaded: Boolean
)

private sealed interface ModInfoState {
    data object Loading: ModInfoState
    data class Success(val data: ModPageResponse): ModInfoState
    data object Failure: ModInfoState

    val success: Success?
        get() = this as? Success
}

private data class Progress(
    val total: Long,
    val complete: Long
) {
    val frac = if(total == 0L) 0f else  complete / total.toFloat()
}

private class ModDownloadStateHolder(
    val modFile: ModFile,
    val dataApi: DataApi,
    val character: Character? = null,
    val info: ModInfoState.Success,
    val scope: CoroutineScope,
) {
    /* These happen at the same time bc the file is read through and input stream when extracting*/
    val downloadProgress = MutableStateFlow(Progress(0L, 0L))
    val unzipProgress = MutableStateFlow(Progress(0L, 0L))

    val errors = mutableStateListOf<String>()

    var state by mutableStateOf<ModDownloadState>(ModDownloadState.Idle)

    fun download() = scope.launch {
        state = ModDownloadState.Downloading

       runCatching {
           downloadAndUpdateDB()
        }
           .onFailure {
               println(it.stackTraceToString())
               errors.add(it.localizedMessage)
           }

        state = ModDownloadState.Idle

        downloadProgress.emit(Progress(0L, 0L))
        unzipProgress.emit(Progress(0L, 0L))
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

        DB.modDao.insertOrUpdate(
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

private class ModStateHolder(
    private val rowId: Int,
    private val dataApi: DataApi,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(ModState())
    val state: StateFlow<ModState> get() = _state.asStateFlow()

    init {
        state.map { it.info.success?.data?.aCategory?.sName }
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { name ->
                val match = DB.characterDao.selectClosesMatch(dataApi.game, name)
                _state.update { it.copy(character = match) }
            }
            .launchIn(scope)

        state.map { it.info.success?.data?.aFiles }.filterNotNull().distinctUntilChanged()
            .combine(
                DB.modDao.observeModsByGbRowId(rowId)
            ) { files, mods ->

                val links = mods.mapNotNull { it.gbDownloadLink }

                _state.update { state ->
                    state.copy(
                        files = files.map {
                            ModFile(it, links.contains(it.sDownloadUrl))
                        }
                    )
                }
            }
            .launchIn(scope)

        scope.launch { initialize() }
    }

    suspend fun initialize() {
        _state.value = runCatching {
            GameBananaApi.modContent(rowId)
        }
            .fold(
                onSuccess = { ModState(ModInfoState.Success(it)) },
                onFailure = { ModState(ModInfoState.Failure) }
            )
    }

    fun refresh() {
        scope.launch {
            _state.value = ModState(ModInfoState.Loading)
            initialize()
        }
    }
}

@Composable
fun ModViewContent(
    id: Int,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val dataApi = LocalDataApi.current
    val scope = rememberCoroutineScope()
    val stateHolder = remember { ModStateHolder(id, dataApi, scope) }
    val retryTrigger = remember { Channel<Unit>() }

    val state by stateHolder.state.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(state.info.success?.data?.sName.orEmpty()) },
                navigationIcon = {
                    IconButton(
                        onClick = onBackPressed
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            Modifier.fillMaxSize().padding(paddingValues),
            Alignment.Center
        ) {
            when (val info = state.info) {
                ModInfoState.Failure ->
                    TextButton(
                        onClick = { retryTrigger.trySend(Unit) }
                    ) {
                        Text("Retry")
                    }
                ModInfoState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is ModInfoState.Success -> ModViewSuccessContent(
                    info = info,
                    files = state.files,
                    character = state.character,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.ModViewSuccessContent(
    info: ModInfoState.Success,
    files: List<ModFile>,
    character: Character?,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val dataApi = LocalDataApi.current

    Column(modifier.fillMaxSize().verticalScroll(scrollState)) {
        ContentRatings(
            info.data.aContentRatings,
            modifier.fillMaxWidth()
        )
        ImagePreview(
            info.data.aPreviewMedia,
            Modifier.fillMaxWidth()
        )
        Column {
            files.fastForEach { modFile ->

                val (file, downloaded) = modFile
                val downloadState = remember(modFile) { ModDownloadStateHolder(modFile, dataApi, character, info, scope) }

                Row(
                    Modifier.width(IntrinsicSize.Max)
                ) {
                    Text("${file.sFile}@${file.sDownloadUrl}")
                    when (downloadState.state) {
                        ModDownloadState.Idle -> {
                            if (!downloaded) {
                                IconButton(
                                    onClick = downloadState::download
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Create,
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                        ModDownloadState.Downloading  -> {

                            val downloadProgress by downloadState.unzipProgress.collectAsState()
                            val unzipProgress by downloadState.unzipProgress.collectAsState()

                            val totalProgress by remember(downloadProgress, unzipProgress) {
                                derivedStateOf {
                                    (downloadProgress.frac + unzipProgress.frac / 2f).coerceIn(0f..1f)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    progress = totalProgress
                                )
                            }

                            val unzipString = remember(unzipProgress) {
                                val complete = OS.humanReadableByteCountBin(unzipProgress.complete)
                                val total = OS.humanReadableByteCountBin(unzipProgress.total)

                                "Unzipping:  $complete / $total"
                            }

                            val downloadString = remember(downloadProgress) {
                                val complete = OS.humanReadableByteCountBin(downloadProgress.complete)
                                val total = OS.humanReadableByteCountBin(downloadProgress.total)

                                "Downloading:  $complete / $total"
                            }

                            Column {
                                Text(downloadString)
                                Text(unzipString)
                            }
                        }
                    }
                }
            }
            Divider()
        }
    }
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(scrollState),
        modifier = Modifier.align(Alignment.CenterEnd),
        style = LocalScrollbarStyle.current.copy(
            thickness = 8.dp,
            hoverColor = MaterialTheme.colors.primary,
            unhoverColor = MaterialTheme.colors.primary
        )
    )
}

private data class ImagePreviewItem(
    val mainUrl: String,
    val smallUrl: String,
)

@Composable
private fun ImagePreview(
    media: ModPageResponse.APreviewMedia,
    modifier: Modifier = Modifier
) {

    val imageList = remember(media) {
        media.aImages.map {
            val base =  it.sBaseUrl.replace("\\", "")
            ImagePreviewItem(
                mainUrl = base + '/' + (it.sFile).replace("\\", ""),
                smallUrl = base + '/' +  (it.sFile100 ?: it.sFile).replace("\\", "")
            )
        }
    }

    val state = rememberPagerState { imageList.size }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier) {
        HorizontalPager(
            state = state,
            pageSize = PageSize.Fixed(600.dp),
            modifier = Modifier.width(600.dp).align(Alignment.CenterHorizontally)
        ) { page ->

            val item = imageList.getOrNull(page) ?: return@HorizontalPager

            AutoSizeImage(
                url = item.mainUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(16f / 10f)
            )
        }
        FlowRow(Modifier.padding(22.dp).fillMaxWidth()) {
            imageList.fastForEachIndexed { i, item ->
                AutoSizeImage(
                    url = item.smallUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(120.dp)
                        .aspectRatio(16f / 10f)
                        .then(
                            if (state.currentPage == i) {
                                Modifier.border(2.dp, MaterialTheme.colors.primary)
                            } else Modifier
                        )
                        .clickable {
                            scope.launch { state.animateScrollToPage(i) }
                        }

                )
            }
        }
    }
}

@Composable
fun ContentRatings(
    ratings: Map<String, String>,
    modifier: Modifier = Modifier
) {
    FlowRow(modifier) {
        ratings.values.forEach { rating ->
            FilterChip(
                selected = false,
                onClick = {},
            ) {
                Text(rating)
            }
        }
    }
}