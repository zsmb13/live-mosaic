package com.bumble.puzzyx.node.app

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.bumble.appyx.components.backstack.BackStack
import com.bumble.appyx.components.backstack.BackStackModel
import com.bumble.appyx.components.backstack.operation.replace
import com.bumble.appyx.navigation.composable.AppyxComponent
import com.bumble.appyx.navigation.integration.LocalScreenSize
import com.bumble.appyx.navigation.modality.BuildContext
import com.bumble.appyx.navigation.node.Node
import com.bumble.appyx.navigation.node.ParentNode
import com.bumble.appyx.navigation.node.node
import com.bumble.appyx.utils.multiplatform.Parcelable
import com.bumble.appyx.utils.multiplatform.Parcelize
import com.bumble.puzzyx.appyx.component.backstackclipper.BackStackClipper
import com.bumble.puzzyx.composable.CallToActionScreen
import com.bumble.puzzyx.composable.MessageBoard
import com.bumble.puzzyx.model.Puzzle.PUZZLE1
import com.bumble.puzzyx.node.app.PuzzyxAppNode.NavTarget
import com.bumble.puzzyx.node.app.PuzzyxAppNode.NavTarget.CallToAction
import com.bumble.puzzyx.node.app.PuzzyxAppNode.NavTarget.MessageBoard
import com.bumble.puzzyx.node.app.PuzzyxAppNode.NavTarget.Puzzle1
import com.bumble.puzzyx.node.puzzle1.Puzzle1Node
import com.bumble.puzzyx.ui.DottedMeshShape
import com.bumble.puzzyx.ui.LocalAutoPlayFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

private val screens = listOf(
    Puzzle1,
    CallToAction,
    MessageBoard
)

class PuzzyxAppNode(
    buildContext: BuildContext,
    private val backStack: BackStack<NavTarget> = BackStack(
        model = BackStackModel(
            initialTargets = listOf(screens.first()),
            savedStateMap = buildContext.savedStateMap,
        ),
        motionController = { BackStackClipper(it, shape = { progress -> ClipShape(progress) }) }
    )
) : ParentNode<NavTarget>(
    buildContext = buildContext,
    appyxComponent = backStack
) {
    sealed class NavTarget : Parcelable {
        @Parcelize
        object Puzzle1 : NavTarget()

        @Parcelize
        object CallToAction : NavTarget()

        @Parcelize
        object MessageBoard : NavTarget()
    }


    override fun resolve(navTarget: NavTarget, buildContext: BuildContext): Node =
        when (navTarget) {
            is Puzzle1 -> Puzzle1Node(
                puzzle = PUZZLE1,
                buildContext = buildContext
            )
            is CallToAction -> node(buildContext) { modifier -> CallToActionScreen(modifier) }
            is MessageBoard -> node(buildContext) { modifier -> MessageBoard(modifier) }
        }

    @Composable
    override fun View(modifier: Modifier) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            var autoPlayFlow = remember { MutableStateFlow(true) }

            CompositionLocalProvider(
                LocalAutoPlayFlow provides autoPlayFlow
            ) {
                CurrentScreen()
                Row {
                    ControlsToggle {
                        autoPlayFlow.update { !it }
                    }
                    NextButton()
                }
            }
        }
    }

    @Composable
    private fun CurrentScreen() {
        AppyxComponent(
            appyxComponent = backStack,
            modifier = Modifier.fillMaxSize()
        )
    }

    @Composable
    private fun ControlsToggle(onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable { onClick() }
                .alpha(0.05f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Toggle manual controls",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }

    @Composable
    private fun NextButton() {
        if (!LocalAutoPlayFlow.current.collectAsState().value) {
            var screenIdx by remember { mutableStateOf(0) }

            Button(
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                onClick = {
                    backStack.replace(
                        target = screens[++screenIdx % screens.size],
                        animationSpec = tween(
                            durationMillis = 3000,
                            easing = FastOutLinearInEasing
                        )
                    )
                }
            ) {
                Text("Next")
            }
        }
    }
}

@Composable
private fun ClipShape(progress: Float): Shape {
    val screenSize = LocalScreenSize.current
    val density = LocalDensity.current
    val (meshMin, meshMax) = 15 to 25
    val meshSizeX = if (screenSize.widthDp > screenSize.heightDp) meshMax else meshMin
    val meshSizeY = if (screenSize.widthDp > screenSize.heightDp) meshMin else meshMax
    val maxRadius = remember(screenSize) {
        with(density) {
            max(screenSize.widthDp, screenSize.heightDp).toPx() / meshMin * 1.5f
        }
    }

    val shape by remember(progress) {
        mutableStateOf(
            DottedMeshShape(
                meshSizeX,
                meshSizeY,
                maxRadius,
                progress
            )
        )
    }

    return shape
}
