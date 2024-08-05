package lib.voyager

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.lifecycle.DisposableEffectIgnoringConfiguration
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import cafe.adriel.voyager.navigator.compositionUniqueId
import cafe.adriel.voyager.transitions.FadeTransition
import cafe.adriel.voyager.transitions.ScreenTransitionContent

public typealias TabNavigatorContent = @Composable (tabNavigator: TabNavigator) -> Unit

public val LocalTabNavigator: ProvidableCompositionLocal<TabNavigator> =
    staticCompositionLocalOf { error("TabNavigator not initialized") }

@OptIn(InternalVoyagerApi::class)
@Composable
fun TabNavigator(
    tab: Tab,
    disposeNestedNavigators: Boolean = false,
    tabDisposable: (@Composable (TabNavigator) -> Unit)? = null,
    key: String = compositionUniqueId(),
    content: TabNavigatorContent = { CurrentTab() }
) {
    Navigator(
        screen = tab,
        disposeBehavior = NavigatorDisposeBehavior(
            disposeNestedNavigators = disposeNestedNavigators,
            disposeSteps = false
        ),
        onBackPressed = null,
        key = key
    ) { navigator ->
        val tabNavigator = remember(navigator) {
            TabNavigator(navigator)
        }

        tabDisposable?.invoke(tabNavigator)

        CompositionLocalProvider(LocalTabNavigator provides tabNavigator) {
            content(tabNavigator)
        }
    }
}

@OptIn(ExperimentalVoyagerApi::class)
@Composable
fun FadeTabTransition(
    navigator: TabNavigator,
    modifier: Modifier = Modifier,
    animationSpec: FiniteAnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow),
    content: ScreenTransitionContent
) {
    FadeTransition(
        navigator = navigator.navigator,
        modifier = modifier,
        disposeScreenAfterTransitionEnd = false,
        animationSpec = animationSpec,
        content = content
    )
}

@OptIn(InternalVoyagerApi::class)
@Composable
fun TabDisposable(navigator: TabNavigator, tabs: List<Tab>) {
    DisposableEffectIgnoringConfiguration(Unit) {
        onDispose {
            tabs.forEach {
                navigator.navigator.dispose(it)
            }
        }
    }
}

class TabNavigator internal constructor(
    internal val navigator: Navigator
) {

    var current: Tab
        get() = navigator.lastItem as Tab
        set(tab) = navigator.replaceAll(tab)

    @Composable
    fun saveableState(
        key: String,
        tab: Tab = current,
        content: @Composable () -> Unit
    ) {
        navigator.saveableState(key, tab, content = content)
    }
}