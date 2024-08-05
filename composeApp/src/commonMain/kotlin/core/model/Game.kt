package core.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable

enum class Game(val data: Byte) {
    Genshin(0x01), 
    StarRail(0x02), 
    ZZZ(0x03);

    val subPath: String = this.name

    @Composable
    fun UiIcon() = when(this) {
        Genshin -> Icon(imageVector = Icons.Outlined.Star, contentDescription = this.name)
        StarRail -> Icon(imageVector = Icons.Outlined.FavoriteBorder, contentDescription = this.name)
        ZZZ -> Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = this.name)
    }

    companion object {

        fun fromByte(byte: Byte) = entries.first { it.data == byte }
    }
}