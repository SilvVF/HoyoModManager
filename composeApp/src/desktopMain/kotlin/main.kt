
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.seiko.imageloader.ImageLoader
import com.seiko.imageloader.LocalImageLoader
import com.seiko.imageloader.component.setupDefaultComponents
import core.db.AppDatabase
import core.db.LocalDatabase

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "hmm",
    ) {
        CompositionLocalProvider(
            LocalImageLoader provides remember { generateImageLoader() },
            LocalSnackBarHostState provides remember { SnackbarHostState() },
            LocalDatabase provides AppDatabase.instance
        ) {
           App()
        }
    }
}

@Composable
@ReadOnlyComposable
operator fun PaddingValues.plus(other: PaddingValues): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(layoutDirection) +
                other.calculateStartPadding(layoutDirection),
        end = calculateEndPadding(layoutDirection) +
                other.calculateEndPadding(layoutDirection),
        top = calculateTopPadding() + other.calculateTopPadding(),
        bottom = calculateBottomPadding() + other.calculateBottomPadding(),
    )
}

fun Color.Companion.fromHex(hex: String): Color {
    val s = hex.removePrefix("#")
    val chunkSize = s.length / 3

    val (r, g, b) = s.chunked(chunkSize).map { str ->
        buildString {
            when (str.length) {
                1 -> append(str[0], str[0])
                2 -> append(str[0], str[1])
            }
        }.toInt(16) and 0xFF
    }

    return Color(r, g, b)
}

val LocalSnackBarHostState  = compositionLocalOf<SnackbarHostState> { error("Not provided in context") }

fun generateImageLoader(): ImageLoader {
    return ImageLoader {
        components {
            setupDefaultComponents()
        }
    }
}

fun <T> List<T>.toggle(element: T): List<T> {
    return if(this.contains(element)) this - element else this + element
}


fun <T> SnapshotStateList<T>.toggle(element: T): Boolean {
    return if(this.contains(element)) this.remove(element) else this.add(element)
}
