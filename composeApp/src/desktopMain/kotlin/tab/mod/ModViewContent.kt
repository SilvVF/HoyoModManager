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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.seiko.imageloader.ui.AutoSizeImage
import core.FileUtils
import core.api.DataApi
import core.api.GameBananaApi
import core.db.DB
import core.model.Character
import core.model.Game
import core.model.Mod
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.NetHelper
import net.model.gamebanana.ModPageResponse
import ui.LocalDataApi
import java.io.File
import java.nio.file.Paths
import javax.xml.crypto.Data
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.pathString


private sealed interface ModInfoState {
    data object Loading: ModInfoState
    data class Success(val data: ModPageResponse): ModInfoState
    data object Failure: ModInfoState
}

@Composable
fun ModViewContent(
    id: Int,
    modifier: Modifier = Modifier
) {

    val retryTrigger = remember { Channel<Unit>() }
    val modInfo by produceState<ModInfoState>(ModInfoState.Loading) {
        retryTrigger.receiveAsFlow().onStart { emit(Unit) }
            .collect {
                value = runCatching {
                    withContext(Dispatchers.IO) { ModInfoState.Success(GameBananaApi.modContent(id)) }
                }
                    .onFailure { println(it.stackTraceToString()) }
                    .getOrDefault(ModInfoState.Failure)
            }
    }

    Scaffold(
        modifier = modifier
    ) { paddingValues ->
        Box(
            Modifier.fillMaxSize().padding(paddingValues),
            Alignment.Center
        ) {
            when (val info = modInfo) {
                ModInfoState.Failure ->
                    TextButton(
                        onClick = { retryTrigger.trySend(Unit) }
                    ) {
                        Text("Retry")
                    }

                ModInfoState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is ModInfoState.Success -> ModViewSuccessContent(info.data)
            }
        }
    }
}

suspend fun downloadFileFromZip(file: ModPageResponse.AFile, game: Game, character: Character): File? {
    return try {
        val downloadUrl = file.sDownloadUrl.replace("\\", "")
        val validExt = listOf("7z", "zip", "rar")

        val ext =  file.sFile.takeLastWhile { it != '.' }.lowercase()
        assert(ext in validExt)

        val res = NetHelper.client.get(downloadUrl) {
            onDownload { bytesSentTotal: Long, contentLength: Long? ->
                println("bytesSentTotal $bytesSentTotal; contentLength $contentLength")
            }
        }

        val inputStream = res.bodyAsChannel().toInputStream()
        val outputPath = Paths.get(
            CharacterSync.rootDir.path,
            game.name,
            character.name,
            file.sFile.removeSuffix(".$ext")
        )

        val path = if (outputPath.exists()) {
            FileUtils.getNewName(outputPath.pathString)
        } else  outputPath.pathString

        val outputDir = File(path).also { it.mkdirs() }

        when (ext) {
            "7z" -> {
                println("extracting using 7z")
                FileUtils.extract7Zip(inputStream, outputDir)
            }
            "zip" -> {
                println("extracting using zip")
                FileUtils.extractZip(inputStream, outputDir)
            }
            "rar" -> {
                println("extracting using rar")
                FileUtils.extractRar(inputStream, outputDir)
            }
        }
        outputDir
    } catch (e: Exception) {
        println(e.stackTraceToString())
        null
    }
}

@Composable
private fun BoxScope.ModViewSuccessContent(
    data: ModPageResponse,
    modifier: Modifier = Modifier
) {
    val dataAPi = LocalDataApi.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val character by produceState<Character?>(null) {
        value = runCatching {
            DB.characterDao.selectClosesMatch(dataAPi.game,  data.aCategory.sName!!)
        }
            .getOrNull()
    }

    Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
        ContentRatings(
            data.aContentRatings,
            modifier.fillMaxWidth()
        )
        ImagePreview(
            data.aPreviewMedia,
            Modifier.fillMaxWidth()
        )

        Column {
            data.aFiles.forEach { file ->
                TextButton(
                    onClick = {
                        scope.launch(Dispatchers.IO + NonCancellable) {
                            character?.let { c ->
                                val modDir = downloadFileFromZip(file, dataAPi.game, c)
                                if (modDir != null) {
                                    DB.modDao.insertOrUpdate(
                                        Mod(
                                            fileName = modDir.name,
                                            game = dataAPi.game.data,
                                            character = c.name,
                                            characterId = c.id,
                                            enabled = false,
                                            modLink = data.sProfileUrl
                                        )
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.padding(6.dp)
                ) {
                    Text("${file.sFile}@${file.sDownloadUrl}")
                }
                Text(character.toString())
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