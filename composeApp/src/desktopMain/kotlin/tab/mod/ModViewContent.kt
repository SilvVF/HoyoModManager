package tab.mod

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import com.seiko.imageloader.ui.AutoSizeImage
import core.model.Character
import kotlinx.coroutines.launch
import net.model.gamebanana.ModPageResponse
import tab.mod.state.ModDownloadStateHolder
import tab.mod.state.ModFile
import tab.mod.state.ModInfoState
import tab.mod.state.ModStateHolder
import ui.LocalDataApi


@Composable
fun ModViewContent(
    id: Int,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dataApi = LocalDataApi.current
    val scope = rememberCoroutineScope()
    val stateHolder = remember { ModStateHolder(id, dataApi, scope) }
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
                        onClick = { stateHolder.refresh() }
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
        Column(Modifier.fillMaxWidth(0.95f).align(Alignment.CenterHorizontally)) {
            Row {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.fillMaxWidth(0.6f), Alignment.CenterStart) {
                        HoverText(
                            "name",
                            onClick = {},
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Box(Modifier.fillMaxWidth(0.2f), Alignment.CenterStart) {
                        HoverText(
                            "upload date",
                            onClick = {},
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Box(Modifier.weight(1f), Alignment.CenterEnd) {
                        HoverText(
                            "actions",
                            onClick = {},
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            HorizontalDivider()
            Spacer(Modifier.height(2.dp))
            files.fastForEach { modFile ->

                val (file, downloaded) = modFile
                val stateHolder = remember(modFile) { ModDownloadStateHolder(modFile, dataApi, character, info, scope) }

                val downloadProgress by stateHolder.downloadProgress.collectAsState()
                val unzipProgress by stateHolder.unzipProgress.collectAsState()

                ModFileListItem(
                    downloadState = stateHolder.state,
                    fileName = file.sFile.orEmpty(),
                    fileTags = remember(file) {
                        with (file) {
                            buildList {
                                listOfNotNull(
                                    sClamAvResult,
                                    sAvastAvResult,
                                    sAnalysisResultCode
                                )
                                    .forEach { add(it) }
                            }
                        }
                    },
                    description = file.sDescription.orEmpty(),
                    downloaded = downloaded,
                    downloadProgress = downloadProgress,
                    unzipProgress = unzipProgress,
                    download = { stateHolder.download() },
                    uploadEpochSecond = modFile.file.tsDateAdded,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        val density = LocalDensity.current
        Spacer(
            Modifier.height(
                with(density) {
                    WindowInsets.systemBars.getBottom(density).toDp()
                }
            )
        )
    }
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(scrollState),
        modifier = Modifier.align(Alignment.CenterEnd),
        style = LocalScrollbarStyle.current.copy(
            thickness = 8.dp,
            hoverColor = MaterialTheme.colorScheme.primary,
            unhoverColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun HoverText(
    text: String,
    onClick: () -> Unit,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    var active by remember { mutableStateOf(false) }
    val color = LocalContentColor.current
    val colorAnim by  animateColorAsState(
        if (active) color else color.copy(alpha = 0.6f)
    )

    Text(
        text = text,
        color = colorAnim,
        modifier = modifier
            .onPointerEvent(PointerEventType.Enter) { active = true }
            .onPointerEvent(PointerEventType.Exit) { active = false }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                null
            ) {
                onClick()
            },
        style = style
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
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
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
            AssistChip(
                onClick = {},
                label = {
                    Text(rating)
                }
            )
        }
    }
}