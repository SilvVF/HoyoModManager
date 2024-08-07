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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.time.debounce
import lib.voyager.Tab
import lib.voyager.TabNavigator
import tab.SearchableTab
import java.time.Duration

data class SearchResult(
    val text: String,
    val tab: Tab,
    val tags: List<Pair<String, String>> = emptyList(),
    val searchTag: String? = null,
    val route: Screen? = null
)

class SearchState(
    scope: CoroutineScope,
    private val navigator: TabNavigator,
) {

    private val NAMESPACE_TO_PREFIX = mapOf(
        "c" to "character",
        "m" to "mod",
        "g" to "game",
        "t" to "tag"
    )

    var query by mutableStateOf(TextFieldValue(""))

    private val _autocomplete = MutableStateFlow(emptyList<String>())

    private fun tagSuggestionFlow(
        contains: Set<String>,
        tag: String?,
        keyword: String,
    ) = flow<String> {
        val complete = NAMESPACE_TO_PREFIX.mapNotNull { (key, fullname) ->
            fullname.takeIf {
                fullname.contains(tag.orEmpty()) && tag.isNullOrBlank().not() && !contains.contains(key)
            }
        }

        if (keyword.contains("https")) {
            emit("url")
        }
        complete.forEach {
            emit(it)
        }
    }

    private var lastResultCount by mutableStateOf(0)

    val resultFlow: StateFlow<List<SearchResult>> = combine(
        snapshotFlow { query.text }.onStart { emit("") },
        snapshotFlow { navigator.navigator.lastItemOrNull as? Tab }.onStart { emit(null) },
    ) { query, tab ->
        Pair(query, tab)
    }
        .onEach { (query, _) ->
            createSuggestions(query)
        }
        .debounce(300)
        .mapLatest { (q, tab) ->
            (tab?.let {
                if (q.isEmpty()) {
                    emptyList()
                } else {
                    getOtherResults(q, tab)
                }
            } ?: emptyList()).also { lastResultCount = it.size }
        }
        .stateIn(
            scope,
            SharingStarted.Eagerly,
            emptyList()
        )

    val autoComplete = _autocomplete.asStateFlow()
        .combine(resultFlow) { complete, results ->
            if (results.isEmpty()) {
                 complete.map { it to null }
            } else {
                buildList<Pair<String, Tab?>> {
                    complete.forEach { s ->
                        val tags = s.split(":").filter { it in NAMESPACE_TO_PREFIX.values + NAMESPACE_TO_PREFIX.keys }
                        results.forEach { result ->
                            result.tags.filter { (tag, _) ->
                                tags.contains(tag)
                            }.forEach { (tag, append) ->
                                add("$tag: $append" to result.tab)
                            }
                        }
                    }
                }
            }
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private suspend fun createSuggestions(query: String) {
        val tagsStripped = run {
            val idx = query.lastIndexOf(':')
            if (idx == -1)
                return@run query

            query.slice(idx..query.lastIndex)
        }
        var _query = query
        val contains = mutableSetOf<Pair<String, String>>()


        while(
            NAMESPACE_TO_PREFIX.any { (k, v) -> _query.startsWith(k) || _query.startsWith(v) }
        ) {
            val prefixRange = 0..(_query.indexOfFirst { it == ':' }.takeIf { it != -1 } ?: break)

            val tagPrefix = _query.slice(prefixRange)
            val tag = tagPrefix.removeSuffix(":").lowercase()

            val key = NAMESPACE_TO_PREFIX.containsKey(tag)
            val reversed = NAMESPACE_TO_PREFIX.entries.associate{ (k,v) -> v to k }

            contains.add(
                if (key) tag to NAMESPACE_TO_PREFIX[tag]!! else reversed[tag]!! to tag
            )
            _query = _query.removeRange(prefixRange).trim()
        }

        val suggestions = tagSuggestionFlow(
            contains.map { it.first }.toSet(),
            _query,
            tagsStripped
        )
            .toList()

        _autocomplete.update {
            val q = query.slice(0..query.indexOfLast { it == ':' })

            suggestions.map { "$q${if (q.isEmpty()) "" else " " }$it:" }.ifEmpty {
                NAMESPACE_TO_PREFIX.filter { (k, v) -> k to v !in contains }
                    .values
                    .map { "$it: $query" }
                    .takeIf { query.isNotEmpty() }
                    .orEmpty()
            }
        }
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
}

val LocalSearchState = staticCompositionLocalOf<SearchState> { error("Not provided") }