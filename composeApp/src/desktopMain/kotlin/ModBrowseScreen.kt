import BrowseState.Success
import BrowseState.Success.PageLoadState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.FilterChip
import androidx.compose.material.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.core.screen.Screen
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

class ModBrowseScreen : Screen {

    @Composable
    override fun Content() {
        ModBrowseContent()
    }
}

sealed interface BrowseState {
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


class ModBrowseStateHolder(
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

    private val _state = MutableStateFlow<BrowseState>(BrowseState.Loading)
    val state: StateFlow<BrowseState>
        get() = _state

    init {
        _game.asStateFlow()
            .onEach {
                initialize()
            }
            .launchIn(scope)
    }

    fun retry() {
        if (_state.value is BrowseState.Failure) {
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
            _state.value = BrowseState.Failure
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

                        if (this.keys.size > 10) {
                            this.keys.toList()
                                .sortedByDescending { abs(it - page) }
                                .take(5)
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
private fun ModBrowseContent() {

    val scope = rememberCoroutineScope()
    val stateHolder = remember { ModBrowseStateHolder(scope) }

    val modState by stateHolder.state.collectAsState()

    Scaffold { paddingValues ->
        when (val state = modState) {
            BrowseState.Failure -> {
                Box(Modifier.fillMaxSize()) {
                    TextButton(
                        onClick = { stateHolder.retry() },
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text("Retry")
                    }
                }
            }
            BrowseState.Loading -> {
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
fun PageContent(
    state: BrowseState.Success,
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
                LazyVerticalGrid(
                    contentPadding = paddingValues,
                    columns = GridCells.Adaptive(190.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(data.data) { submission ->
                        Column(Modifier.fillMaxSize()) {
                            Text(submission.sName.orEmpty())
                            Text(submission.sProfileUrl.orEmpty())
                            Text(submission.sSingularTitle.orEmpty())
                        }
                    }
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
fun PageNumbersList(
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

        IconButton(
            onClick = {}
        ) {

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