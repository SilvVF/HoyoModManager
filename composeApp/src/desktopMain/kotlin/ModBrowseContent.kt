import BrowseState.*
import BrowseState.Success.PageLoadState
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
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
import core.api.GameBananaApi
import core.api.GenshinApi
import core.api.StarRailApi
import core.api.ZZZApi
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

private sealed interface BrowseState {
    data object Loading: BrowseState

    data  class Success(
        val pageCount: Int,
        val page: Int,
        val mods: Map<Int, PageLoadState>,
        val subCategories: List<CategoryListResponseItem>
    ): BrowseState {

        sealed interface PageLoadState {
            data object Loading: PageLoadState
            data class Success(val data: List<CategoryContentResponse.ARecord>): PageLoadState
            data object Failure: PageLoadState
        }
    }
    data object Failure: BrowseState

    val success: Success?
        get() = this as? Success
}


private class ModBrowseStateHolder(
    private val scope: CoroutineScope,
) {
    private val PER_PAGE = 30
    private fun MutableStateFlow<BrowseState>.updateSuccess(block: (Success) -> Success) {
        return this.update { state ->
            (state as? Success)?.let(block) ?: state
        }
    }

    private val _game = MutableStateFlow(Genshin)

    private val dataApi
        get() = when(_game.value) {
            Genshin -> GenshinApi
            StarRail -> StarRailApi
            ZZZ -> ZZZApi
        }

    private val _state = MutableStateFlow<BrowseState>(Loading)
    val state: StateFlow<BrowseState>
        get() = _state

    init {
        _game.asStateFlow()
            .onEach {
                _state.value = Loading
                initialize()
            }
            .launchIn(scope)
    }

    fun retry() {
        if (_state.value is Failure) {
            scope.launch { initialize() }
        }
    }

    private suspend fun initialize() {
        val res = try {
            GameBananaApi.categoryContent(
                id = dataApi.skinCategoryId,
                perPage = PER_PAGE,
                page = 1
            )
        } catch (e: Exception) {
            print(e.stackTraceToString())
            _state.value = Failure
            return
        }

        _state.value = Success(
            page = 1,
            pageCount = res.aMetadata.nRecordCount / res.aMetadata.nPerpage,
            mods = mapOf(1 to PageLoadState.Success(res.aRecords)),
            subCategories = GameBananaApi.categories(dataApi.skinCategoryId)
        )
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
}

@Composable
fun ModBrowseContent(
    onModClick: (id: Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val stateHolder = remember { ModBrowseStateHolder(scope) }

    val modState by stateHolder.state.collectAsState()

    Scaffold { paddingValues ->
        when (val state = modState) {
            Failure -> {
                Box(Modifier.fillMaxSize()) {
                    TextButton(
                        onClick = { stateHolder.retry() },
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text("Retry")
                    }
                }
            }
            Loading -> {
                Box(Modifier.fillMaxSize()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
            is Success -> {
                PageContent(
                    state,
                    { stateHolder.loadPage(it) },
                    paddingValues,
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
                            Column(Modifier.fillMaxSize().padding(8.dp)) {
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