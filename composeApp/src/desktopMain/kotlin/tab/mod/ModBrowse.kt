package tab.mod

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import tab.mod.components.ModBrowseContent

class ModBrowse(
    private val categoryId: Int
): Screen {

    override val key: ScreenKey
        get() = super.key + categoryId


    @Composable
    override fun Content() {

        val navigator = LocalNavigator.currentOrThrow

        ModBrowseContent(
            categoryId = categoryId,
            onModClick = { id -> navigator.push(ModView(id)) }
        )
    }
}