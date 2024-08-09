package tab.mod

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import tab.mod.components.ModViewContent

class ModView(
    val idRow: Int,
): Screen {

    override val key: ScreenKey = idRow.toString()

    @Composable
    override fun Content() {

        val navigator = LocalNavigator.currentOrThrow

        ModViewContent(
            idRow,
            onBackPressed = { navigator.pop() },
            Modifier.fillMaxSize()
        )
    }
}
