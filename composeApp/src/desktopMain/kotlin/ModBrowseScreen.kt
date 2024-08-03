import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.transitions.FadeTransition
import core.api.GameBananaApi
import core.api.GenshinApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ModBrowseScreen : Screen {

    @Composable
    override fun Content() {
        Navigator(ModBrowse(GenshinApi.skinCategoryId)) { navigator ->
            FadeTransition(navigator)
        }
    }
}

private class ModView(val idRow: Int): Screen {

    @Composable
    override fun Content() {

        val data by produceState("") {
            value = withContext(Dispatchers.IO) {
                runCatching { GameBananaApi.modContent(idRow) }
                    .fold(
                        onFailure = { it.stackTraceToString() },
                        onSuccess = { it.toString() }
                    )
            }
        }
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text(data)
        }
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
