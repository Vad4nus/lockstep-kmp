@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.vad4nus.lockstep.compose

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import io.github.vad4nus.lockstep.mvvm.navigation.Route
import io.github.vad4nus.lockstep.mvvm.navigation.SheetRoute

internal class SheetInfo(val route: Route, val properties: ModalBottomSheetProperties)

internal object SheetKey : NavMetadataKey<SheetInfo>

internal fun sheetEntry(entries: List<NavEntry<Route>>): SheetInfo? = entries.lastOrNull()?.metadata?.get(SheetKey)

internal fun sheetAwareProvider(
    provider: (Route) -> NavEntry<Route>,
    properties: ModalBottomSheetProperties,
): (Route) -> NavEntry<Route> = { key ->
    val entry = provider(key)
    if (key !is SheetRoute) {
        entry
    } else {
        NavEntry(key, entry.contentKey, entry.metadata + metadata { put(SheetKey, SheetInfo(key, properties)) }) {
            entry.Content()
        }
    }
}

internal class SheetSceneStrategy(private val onDismiss: (Route) -> Unit) : SceneStrategy<Route> {

    override fun SceneStrategyScope<Route>.calculateScene(entries: List<NavEntry<Route>>): Scene<Route>? {
        if (sheetEntry(entries) == null) return null
        val entry = entries.last()
        return SheetScene(entry.contentKey, entry, entries.dropLast(1), onDismiss)
    }
}

internal class SheetScene(
    override val key: Any,
    private val entry: NavEntry<Route>,
    private val below: List<NavEntry<Route>>,
    private val onDismiss: (Route) -> Unit,
) : OverlayScene<Route> {

    private val info: SheetInfo = checkNotNull(entry.metadata.get(SheetKey))

    override val entries: List<NavEntry<Route>> = listOf(entry)

    override val previousEntries: List<NavEntry<Route>> = below

    override val overlaidEntries: List<NavEntry<Route>> = below

    private var sheetState: SheetState? = null

    override val content: @Composable () -> Unit = {
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        DisposableEffect(state) {
            sheetState = state
            onDispose { sheetState = null }
        }
        ModalBottomSheet(
            onDismissRequest = { onDismiss(info.route) },
            sheetState = state,
            properties = info.properties,
        ) {
            entry.Content()
        }
    }

    override suspend fun onRemove() {
        sheetState?.hide()
    }

    override fun equals(other: Any?): Boolean =
        other is SheetScene &&
            key == other.key &&
            entry == other.entry &&
            below == other.below

    override fun hashCode(): Int = key.hashCode() * 31 + entry.hashCode()
}
