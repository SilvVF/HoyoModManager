package tab.mod.state

import core.api.DataApi
import core.api.GameBananaApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.model.gamebanana.CategoryContentResponse
import net.model.gamebanana.CategoryListResponseItem
import tab.mod.state.BrowseState.Failure
import tab.mod.state.BrowseState.Loading
import tab.mod.state.BrowseState.Success
import tab.mod.state.BrowseState.Success.PageLoadState
import kotlin.math.abs

sealed class BrowseState(
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


class ModBrowseStateHolder(
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
