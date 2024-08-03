import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.transitions.FadeTransition

class ModBrowseScreen : Screen {

    @Composable
    override fun Content() {
        Navigator(ModBrowse()) { navigator ->
            FadeTransition(navigator)
        }
    }
}

private class ModView(val idRow: Int): Screen {

    @Composable
    override fun Content() {

    }
}

private class ModBrowse: Screen {

    @Composable
    override fun Content() {

        val navigator = LocalNavigator.currentOrThrow

        ModBrowseContent(
            onModClick = { id -> navigator.push(ModView(id)) }
        )
    }
}

