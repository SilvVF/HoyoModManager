package tab.playlist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import lib.voyager.Tab

data object PlaylistTab: Tab {

    @Composable
    override fun Icon() {
        androidx.compose.material.Icon(
            imageVector = Icons.Outlined.FavoriteBorder,
            contentDescription = null
        )
    }

    @Composable
    override fun Content() {

        PlaylistScreen(Modifier.fillMaxSize())
    }
}