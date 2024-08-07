import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.time.debounce
import lib.voyager.Tab
import lib.voyager.TabNavigator
import tab.SearchableTab
import java.net.URL
import java.time.Duration

data class SearchResult(
    val tab: Tab,
    val search: String,
    val tags: Set<String>,
    val route: Screen? = null
)

class SearchState(
    scope: CoroutineScope,
    private val navigator: TabNavigator,
) {

    private val TAGS = setOf(
        CHARACTER_TAG, MOD_TAG, URL_TAG, TAG_TAG
    )

    var query by mutableStateOf(TextFieldValue(""))

    private val _autocomplete = MutableStateFlow(emptyList<String>())
    private val _results = MutableStateFlow(emptyList<SearchResult>())

    val results = _results.asStateFlow()
    val autoComplete = _autocomplete.asStateFlow()

    init {
        combine(
            snapshotFlow { query.text }.onStart { emit("") },
            snapshotFlow { navigator.navigator.lastItemOrNull as? Tab }.onStart { emit(null) },
        ) { query, tab ->
            Pair(query, tab)
        }
            .debounce(300)
            .mapLatest { (q, tab) ->
                q to tab?.let {
                    if (q.isEmpty()) {
                        emptyList()
                    } else {
                        getOtherResults(q, tab)
                    }
                }.orEmpty()
            }
            .onEach { (q, results) ->
                supervisorScope {
                    launch { createSuggestions(q, results) }
                    _results.emit(results)
                }
            }
            .catch { println(it.stackTraceToString()) }
            .launchIn(scope)
    }

    private suspend fun createSuggestions(query: String, results: List<SearchResult>) {

        if (query.isEmpty()) return

        val tagString = query.slice(0..<query.lastIndexOf(':'))

        val searchTags = tagString.split(':').map { it.trim() }.toSet()
        val search = query.takeLastWhile { it != ':' }

        val possibleTags = TAGS.filter { it.startsWith(search) && !searchTags.contains(it) }

        val completedSearches = if (possibleTags.isNotEmpty()) {
            possibleTags.map { tag ->
                "$tagString$tag: "
            }
        } else {
            results.map {
                it.tags.joinToString(":", postfix = ": ") + it.search
            }
        }

        _autocomplete.emit(completedSearches)
    }

    private suspend fun getOtherResults(query: String, currentTab: Tab): List<SearchResult> {
        return TABS_LIST.filter { currentTab != it }.mapNotNull {
            runCatching { (it as? SearchableTab)?.results(query) }.getOrNull()
        }
            .flatten()
    }

    fun update(string: String) {
        query = query.copy(text = string)
    }

    companion object {
        const val CHARACTER_TAG = "char"
        const val MOD_TAG = "mod"
        const val URL_TAG = "url"
        const val TAG_TAG = "tag"
    }
}

val LocalSearchState = staticCompositionLocalOf<SearchState> { error("Not provided") }