package lib.voyager

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.transitions.ScreenTransition
import cafe.adriel.voyager.transitions.ScreenTransitionContent
import kotlinx.coroutines.flow.collectLatest

@Composable
fun CurrentTab() {
    val tabNavigator = LocalTabNavigator.current
    val currentTab = tabNavigator.current

    tabNavigator.saveableState("currentTab") {
        currentTab.Content()
    }
}

interface Tab : Screen {

   fun readResolve(): Any = this

    @Composable
    fun Icon()
}

@OptIn(ExperimentalVoyagerApi::class)
@Composable
fun FadeTransition(
    tabNavigator: TabNavigator,
    modifier: Modifier = Modifier,
    animationSpec: FiniteAnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow),
    content: ScreenTransitionContent = { it.Content() },
) {
    val navigator = tabNavigator.navigator

    tabNavigator.saveableState("currentTab") {
        AnimatedContent(
            targetState = tabNavigator.current,
            transitionSpec = {
                val contentTransform = fadeIn(animationSpec) togetherWith fadeOut(animationSpec)

                val sourceScreenTransition = when (navigator.lastEvent) {
                    StackEvent.Pop, StackEvent.Replace -> initialState
                    else -> targetState
                } as? ScreenTransition

                val screenEnterTransition = sourceScreenTransition?.enter(navigator.lastEvent)
                    ?: contentTransform.targetContentEnter

                val screenExitTransition = sourceScreenTransition?.exit(navigator.lastEvent)
                    ?: contentTransform.initialContentExit

                screenEnterTransition togetherWith screenExitTransition
            },
            modifier = modifier
        ) { screen ->
            content(screen)
        }
    }
}
