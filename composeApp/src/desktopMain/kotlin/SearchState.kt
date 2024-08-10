import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.input.TextFieldValue
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import lib.voyager.Tab
import lib.voyager.TabNavigator
import tab.SearchableTab

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

    val parsedQuery by derivedStateOf { splitTagsAndQuery(query.text) }

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
                    launch { createSuggestions(q.trim(), results) }
                    _results.emit(results)
                }
            }
            .catch { println(it.stackTraceToString()) }
            .launchIn(scope)
    }

    private fun splitTagsAndQuery(raw: String): Pair<String, Set<String>> {
        val lastTagIdx = raw.lastIndexOf(':')
        val query = raw.slice(
            lastTagIdx.coerceAtLeast(0)..raw.lastIndex
        )
            .removePrefix(":")
            .trim()

        val tags = if(lastTagIdx == -1) {
            emptySet()
        } else {
            raw.removeSuffix(query)
                .split(':')
                .map(::removeWhitespace)
                .map(::toLower)
                .filter(TAGS::contains)
                .toSet()
        }
        return query to tags
    }

    private suspend fun createSuggestions(query: String, results: List<SearchResult>) {

        if (query.isEmpty()) {
            _autocomplete.emit(emptyList())
            return
        }

        val tagString = query.slice(0..<query.lastIndexOf(':'))

        val searchTags = tagString.split(':').map { it.trim() }.toSet()
        val search = query.takeLastWhile { it != ':' }

        val possibleTags = if (search.isEmpty())
            emptyList()
        else
            TAGS.filter { it.startsWith(search) && !searchTags.contains(it) }

        val completedSearches = if (possibleTags.isNotEmpty()) {
            possibleTags.map { tag ->
                "$tagString${if (tagString.isNotEmpty()) ":" else ""}$tag: "
            }
        } else {
            results.map {
                it.tags.joinToString(":", postfix = ": ") + it.search
            }
        }

        _autocomplete.emit(completedSearches)
    }

    private fun removeWhitespace(string: String) = string.trim(' ')
    private fun toLower(string: String) = string.lowercase()

    private suspend fun getOtherResults(raw: String, currentTab: Tab): List<SearchResult> {

        val (query, tags) = splitTagsAndQuery(raw)

        return TABS_LIST.mapNotNull { tab ->
            runCatching {
                (tab as? SearchableTab)?.results(tags, query, tab == currentTab)
            }
                .getOrNull()
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