import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class ReselectTab: Screen {

    private val scope = CoroutineScope(Dispatchers.Main)

    protected val events = Channel<Unit>()

    fun onReselect() {
        scope.launch { events.send(Unit) }
    }

    @Composable
    fun LaunchOnReselect(collector: FlowCollector<Unit>) {
        LaunchedEffect(events) {
            events.receiveAsFlow().collect(collector)
        }
    }
}