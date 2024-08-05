package tab

import SearchResult
import androidx.annotation.CallSuper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import lib.voyager.Tab
import lib.voyager.TabNavigator

interface SearchableTab {

    suspend fun results(query: String): List<SearchResult>

    fun onResultSelected(result: SearchResult, navigator: TabNavigator)
}

interface ReselectTab {

    fun onReselect()

    companion object {
        fun compose() = object: ComposeReselectTab {

            override val scope = CoroutineScope(Dispatchers.Main)

            override val events = Channel<Unit>()

            @CallSuper
            override fun onReselect() {
                scope.launch { events.send(Unit) }
            }
        }
    }
}

interface ComposeReselectTab: ReselectTab {
    val scope: CoroutineScope
    val events: Channel<Unit>
}

@Composable
fun ComposeReselectTab.LaunchedOnReselect(collector: FlowCollector<Unit>) {
    val tab = this
    LaunchedEffect(Unit) {
        tab.events.receiveAsFlow().collect(collector)
    }
}