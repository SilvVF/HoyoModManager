import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import com.seiko.imageloader.ui.AutoSizeImage
import core.api.GameBananaApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.model.gamebanana.CategoryContentResponse
import net.model.gamebanana.ModPageResponse

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

@Composable
private fun BoxScope.ModViewSuccessContent(
    data: ModPageResponse,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
        ContentRatings(
            data.aContentRatings,
            modifier.fillMaxWidth()
        )
        ImagePreview(
            data.aPreviewMedia,
            Modifier.fillMaxWidth()
        )
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