package tab

import SearchResult
import androidx.annotation.CallSuper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import lib.voyager.Tab
import lib.voyager.TabNavigator
import kotlin.reflect.KClass

interface SearchableTab {

    suspend fun results(query: String): List<SearchResult>

    fun onResultSelected(result: SearchResult, navigator: TabNavigator)
}

interface ReselectTab {

    suspend fun onReselect()

    companion object {
        fun compose() = object: ComposeReselectTab {

            override val events = Channel<Unit>(UNLIMITED)

            @CallSuper
            override suspend fun onReselect() {
                events.send(Unit)
            }
        }
    }
}

interface ComposeReselectTab: ReselectTab {
    val events: Channel<Unit>
}

@Composable
fun ComposeReselectTab.LaunchedOnReselect(collector: FlowCollector<Unit>) {
    val tab = this
    LaunchedEffect(Unit) {
        tab.events.receiveAsFlow().collect(collector)
    }
}