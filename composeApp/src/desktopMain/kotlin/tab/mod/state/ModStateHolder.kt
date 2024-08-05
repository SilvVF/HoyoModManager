package tab.mod.state

import core.api.DataApi
import core.api.GameBananaApi
import core.db.AppDatabase
import core.model.Character
import kotlinx.coroutines.CoroutineScope
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
import net.model.gamebanana.ModPageResponse

data class ModState(
    val info: ModInfoState = ModInfoState.Loading,
    val character: Character? = null,
    val files: List<ModFile> = emptyList(),
)

data class ModFile(
    val file: ModPageResponse.AFile,
    val downloaded: Boolean
)

sealed interface ModInfoState {
    data object Loading: ModInfoState
    data class Success(val data: ModPageResponse): ModInfoState
    data object Failure: ModInfoState

    val success: Success?
        get() = this as? Success
}


class ModStateHolder(
    private val rowId: Int,
    private val dataApi: DataApi,
    private val scope: CoroutineScope,
    private val database: AppDatabase = AppDatabase.instance
) {
    private val _state = MutableStateFlow(ModState())
    val state: StateFlow<ModState> get() = _state.asStateFlow()

    init {
        state.map { it.info.success?.data?.aCategory?.sName }
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { name ->
                val match = database.selectClosesMatch(dataApi.game, name)
                _state.update { it.copy(character = match) }
            }
            .launchIn(scope)

        state.map { it.info.success?.data?.aFiles }.filterNotNull().distinctUntilChanged()
            .combine(
                database.subscribeToModByGbId(rowId)
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