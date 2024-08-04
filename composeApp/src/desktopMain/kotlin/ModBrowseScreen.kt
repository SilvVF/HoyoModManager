import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.transitions.FadeTransition
import core.api.GameBananaApi
import core.api.GenshinApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID


class ModBrowseScreen : Screen, ReselectTab() {

    @Composable
    override fun Content() {
        Navigator(ModBrowse(GenshinApi.skinCategoryId)) { navigator ->

            LaunchOnReselect {
                navigator.replace(ModBrowse(GenshinApi.skinCategoryId))
            }

            FadeTransition(navigator)
        }
    }
}

private class ModView(val idRow: Int): Screen {

    @Composable
    override fun Content() {
        ModViewContent(
            idRow,
            Modifier.fillMaxSize()
        )
    }
}

private class ModBrowse(
    private val categoryId: Int
): Screen {

    @Composable
    override fun Content() {

        val navigator = LocalNavigator.currentOrThrow

        ModBrowseContent(
            categoryId = categoryId,
            onModClick = { id -> navigator.push(ModView(id)) }
        )
    }
}
