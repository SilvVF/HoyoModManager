import BrowseState.*
import BrowseState.Success.PageLoadState
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.FilterChip
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.seiko.imageloader.ui.AutoSizeImage
import core.api.DataApi
import core.api.GameBananaApi
import core.api.GenshinApi
import core.api.StarRailApi
import core.api.ZZZApi
import core.model.Game
import core.model.Game.Genshin
import core.model.Game.StarRail
import core.model.Game.ZZZ
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.model.gamebanana.CategoryContentResponse
import net.model.gamebanana.CategoryListResponseItem
import kotlin.math.abs

private sealed class BrowseState(
    open val gbUrl: String
) {
    data class Loading(override val gbUrl: String) : BrowseState(gbUrl)

    data  class Success(
        override val gbUrl: String,
        val pageCount: Int,
        val page: Int,
        val mods: Map<Int, PageLoadState>,
        val subCategories: List<CategoryListResponseItem>
    ): BrowseState(gbUrl) {

        sealed interface PageLoadState {
            data object Loading: PageLoadState
            data class Success(val data: List<CategoryContentResponse.ARecord>): PageLoadState
            data object Failure: PageLoadState
        }
    }

    data class Failure(override val gbUrl: String) : BrowseState(gbUrl)

    val success: Success?
        get() = this as? Success
}


private class ModBrowseStateHolder(
    private val dataApi: DataApi,
    private val categoryId: Int,
    private val scope: CoroutineScope,
) {
    private fun MutableStateFlow<BrowseState>.updateSuccess(block: (Success) -> Success) {
        return this.update { state ->
            (state as? Success)?.let(block) ?: state
        }
    }

    private val _state = MutableStateFlow<BrowseState>(Loading(GB_CAT_URL + categoryId))
    val state: StateFlow<BrowseState> get() = _state

    init {
        scope.launch {
            initialize()
        }
    }

    fun retry() {
        if (_state.value is Failure) {
            scope.launch { initialize() }
        }
    }

    private suspend fun initialize() {
        val res = try {
            GameBananaApi.categoryContent(
                id = categoryId,
                perPage = PER_PAGE,
                page = 1
            )
        } catch (e: Exception) {
            print(e.stackTraceToString())
            _state.update { Failure(it.gbUrl) }
            return
        }

        _state.update {
            Success(
                page = 1,
                gbUrl = it.gbUrl,
                pageCount = res.aMetadata.nRecordCount / res.aMetadata.nPerpage,
                mods = mapOf(1 to PageLoadState.Success(res.aRecords)),
                subCategories = GameBananaApi.categories(dataApi.skinCategoryId)
            )
        }
    }

    fun loadPage(page: Int) {
        scope.launch(Dispatchers.IO) {

            if (_state.value.success?.mods?.get(page) is PageLoadState.Success){
                _state.updateSuccess { state -> state.copy(page = page) }
                return@launch
            }

            _state.updateSuccess { state ->
                state.copy(
                    page = page,
                    mods = state.mods + mapOf(page to PageLoadState.Loading)
                )
            }

            val res = runCatching {
                GameBananaApi.categoryContent(
                    id = dataApi.skinCategoryId,
                    perPage = PER_PAGE,
                    page = page
                )
            }

            _state.updateSuccess { state ->
                state.copy(
                    mods = state.mods.toMutableMap().apply {
                        /*
                            Cache 8 responses in memory duplicate requests are cached by ktor
                            in File storage drop half when hitting limit.
                         */
                        if (this.keys.size > 8) {
                            this.keys.toList()
                                .sortedByDescending { abs(it - page) }
                                .take(4)
                                .forEach { key ->
                                    this.remove(key)
                                }
                        }

                        this[page] = res.fold(
                            onSuccess = { PageLoadState.Success(it.aRecords) },
                            onFailure = { PageLoadState.Failure }
                        )
                    }.toMap(),
                    page = page,
                )
            }
        }
    }

    companion object {
        private const val GB_CAT_URL = "https://gamebanana.com/mods/cats/"
        private const val PER_PAGE = 30
    }
}

@Composable
fun ModBrowseContent(
    categoryId: Int,
    onModClick: (id: Int) -> Unit
) {
    Scaffold { paddingValues ->

        val dataApi = remember { GenshinApi }
        val scope = rememberCoroutineScope()
        val stateHolder = remember(dataApi) { ModBrowseStateHolder(dataApi, categoryId, scope) }

        val modState by stateHolder.state.collectAsState()

        when (val state = modState) {
            is Failure -> {
                Box(Modifier.fillMaxSize()) {
                    TextButton(
                        onClick = { stateHolder.retry() },
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text("Retry")
                    }
                }
            }
            is Loading -> {
                Box(Modifier.fillMaxSize()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
            is Success -> {
                PageContent(
                    state = state,
                    onModClick = onModClick,
                    loadPage = { stateHolder.loadPage(it) },
                    paddingValues = paddingValues,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}


@Composable
private fun PageContent(
    state: Success,
    loadPage: (page: Int) -> Unit,
    onModClick: (id: Int) -> Unit,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        when (val data = state.mods[state.page] ?: return@Column) {
            PageLoadState.Failure -> Box(Modifier.fillMaxSize()) {
                TextButton(
                    onClick = { loadPage(state.page) },
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text("Retry")
                }
            }
            PageLoadState.Loading ->   Box(Modifier.fillMaxSize()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            is PageLoadState.Success -> {
                val gridState = rememberLazyGridState()
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    LazyVerticalGrid(
                        state = gridState,
                        contentPadding = paddingValues,
                        columns = GridCells.Adaptive(280.dp),
                        modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp)
                    ) {
                        items(data.data) { submission ->
                            Column(Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .clickable {
                                    println(submission.idRow?.toString() + "Clicked")
                                    submission.idRow?.let { onModClick(it) }
                                }
                            ) {
                                ModImagePreview(
                                    submission.aPreviewMedia,
                                    Modifier.fillMaxWidth()
                                )
                                val name = remember{

                                    val css = submission.aSubmitter?.sSubjectShaperCssCode
                                    val colorHex = css
                                        ?.split(";")
                                        ?.firstOrNull { it.startsWith("color:") }
                                        ?.removePrefix("color:")

                                    buildAnnotatedString {
                                        withStyle(
                                            SpanStyle(
                                                color = colorHex?.let {
                                                    Color.fromHex(colorHex)
                                                } ?: Color.Unspecified
                                            )
                                        ) {
                                            append(submission.sName.orEmpty())
                                        }
                                    }
                                }

                                Text(name)
                                Text(submission.sProfileUrl.orEmpty())
                                Text(submission.sSingularTitle.orEmpty())
                            }
                        }
                    }
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        adapter = rememberScrollbarAdapter(gridState),
                        style = LocalScrollbarStyle.current.copy(
                            thickness = 8.dp,
                            hoverColor = MaterialTheme.colors.primary,
                            unhoverColor = MaterialTheme.colors.primary
                        )
                    )
                }
            }
        }
        PageNumbersList(
            state.page,
            state.pageCount,
            loadPage,
            Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun ModImagePreview(
    media: CategoryContentResponse.ARecord.APreviewMedia?,
    modifier: Modifier = Modifier
) {
    val imgUrl = remember(media) {
        runCatching {
            val img = media!!.aImages!!.first()

            val baseUrl = img.sBaseUrl!!.replace("\\", "")
            val file = (img.sFile220 ?: img.sFile!!).replace("\\", "")
            "$baseUrl/$file"
        }
            .getOrNull()
    }

    Box(modifier.aspectRatio(16f / 10f)) {
        if (imgUrl != null) {
            AutoSizeImage(
                url = imgUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize().background(Color.Gray))
        }
    }
}

@Composable
private fun PageNumbersList(
    page: Int,
    lastPage: Int,
    goToPage: (page: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val list = remember(page, lastPage) {
            val minPage = (page - 4).coerceAtLeast(1)
            val maxPage = (page + 9 - (page - minPage)).coerceIn(1..lastPage)

            (minPage..maxPage).toList()
        }

        list.fastForEach { num ->
            FilterChip(
                selected = page == num,
                onClick = { goToPage(num) },
                modifier = Modifier.wrapContentSize().padding(2.dp),
            ) {
                Text(
                    num.toString(),
                    Modifier.align(Alignment.CenterVertically)
                )
            }
        }

        IconButton(
            onClick = {}
        ) {

        }
    }
}