package tab.mod

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import tab.mod.components.ModBrowseContent
import ui.LocalDataApi

class ModBrowse(
    val categoryId: Int,
    val name: String?  = null
): Screen {

    override val key: ScreenKey
        get() = name.orEmpty() + categoryId


    @Composable
    override fun Content() {

        val navigator = LocalNavigator.currentOrThrow
        val dataApi = LocalDataApi.current

        ModBrowseContent(
            categoryId = categoryId,
            onModClick = { id -> navigator.push(ModView(id)) },
            onCategoryClick = { name, id ->
                if (id != dataApi.skinCategoryId) {
                    navigator.popUntil { (it as? ModBrowse)?.categoryId == dataApi.skinCategoryId }
                    navigator.push(ModBrowse(id, name))
                } else {
                    navigator.replace(ModBrowse(id, name))
                }
            }
        )
    }
}