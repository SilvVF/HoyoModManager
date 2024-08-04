package tab.game

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.ScreenKey
import core.api.GenshinApi
import core.api.StarRailApi
import core.api.ZZZApi
import core.model.Game
import core.model.Game.Genshin
import core.model.Game.StarRail
import core.model.Game.ZZZ
import lib.voyager.Tab
import tab.mod.GameModListScreen
import ui.LocalDataApi

interface GameTab: Tab {

    val game: Game

    override val key: ScreenKey get() = super.key + game.name

    @Composable
    override fun Icon() = game.UiIcon()

    @Composable
    override fun Content() {
        CompositionLocalProvider(
            LocalDataApi provides remember(game) {
                when(game) {
                    Genshin -> GenshinApi
                    StarRail -> StarRailApi
                    ZZZ -> ZZZApi
                }
            }
        ) {
            GameModListScreen(game, Modifier.fillMaxSize())
        }
    }
}
