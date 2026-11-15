package io.github.vad4nus.lockstep.compose

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import io.github.vad4nus.lockstep.mvvm.navigation.Route
import io.github.vad4nus.lockstep.mvvm.navigation.StackNavigator

/**
 * Renders a [StackNavigator] with Navigation 3: root and path become the back stack, the open sheet
 * becomes a bottom-sheet overlay, and every entry owns its saved state and its view models, also when
 * its route equals one deeper in the stack. Entries for [SheetRoute] keys are declared in
 * [entryProvider] like any other entry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun NavigatorDisplay(
    navigator: StackNavigator,
    modifier: Modifier = Modifier,
    sheetProperties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
    entryProvider: (Route) -> NavEntry<Route>,
) {
    val stack by navigator.stack.collectAsStateWithLifecycle()
    val strategies = remember(navigator) {
        listOf(SheetSceneStrategy(navigator::onSystemDismissSheet), SinglePaneSceneStrategy<Route>())
    }
    val provider = remember(entryProvider, sheetProperties) { sheetAwareProvider(entryProvider, sheetProperties) }
    val entries = remember(stack, provider) {
        stackEntries(listOf(stack.root) + stack.path + listOfNotNull(stack.sheet), provider)
    }
    NavDisplay(
        entries = rememberDecoratedNavEntries(
            entries,
            listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
        ),
        modifier = modifier,
        sceneStrategies = strategies,
        onBack = { navigator.pop() },
    )
}

/**
 * The entries of [routes]. Navigation 3 keeps saved state and view models per content key, and equal routes get equal
 * keys, so a route that repeats an earlier one gets a key of its own: the key of the route paired with the number
 * of earlier occurrences.
 */
internal fun stackEntries(routes: List<Route>, provider: (Route) -> NavEntry<Route>): List<NavEntry<Route>> {
    val seen = mutableMapOf<Route, Int>()
    return routes.map { route ->
        val entry = provider(route)
        val earlier = seen[route] ?: 0
        seen[route] = earlier + 1
        if (earlier == 0) entry else NavEntry(route, entry.contentKey to earlier, entry.metadata) { entry.Content() }
    }
}
