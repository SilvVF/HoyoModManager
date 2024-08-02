import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.seiko.imageloader.ImageLoader
import com.seiko.imageloader.LocalImageLoader
import com.seiko.imageloader.component.setupDefaultComponents

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "hmm",
    ) {
        CompositionLocalProvider(
            LocalImageLoader provides remember { generateImageLoader() },
            LocalSnackBarHostState provides remember { SnackbarHostState() }
        ) {
           App()
        }
    }
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
