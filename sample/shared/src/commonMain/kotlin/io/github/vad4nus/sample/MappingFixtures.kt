package io.github.vad4nus.sample

import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.StringDesc
import io.github.vad4nus.lockstep.UiFlow
import io.github.vad4nus.lockstep.UiStateFlow
import io.github.vad4nus.lockstep.asUiFlow
import io.github.vad4nus.lockstep.asUiStateFlow
import io.github.vad4nus.lockstep.mvvm.navigation.Route
import io.github.vad4nus.lockstep.mvvm.navigation.SheetRoute
import io.github.vad4nus.lockstep.mvvm.navigation.StackNavigator
import io.github.vad4nus.sample.orders.OrdersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

data class MappingFixtureState(
    val addresses: List<Address> = listOf(Address("Oslo", 150)),
    val maybeAddresses: List<Address>? = null,
    val title: StringDesc = StringDesc.Raw("Hello"),
    val maybeTitle: StringDesc? = null,
)

class MappingFixtureViewModel : BaseViewModel<MappingFixtureState, ProfileAction>(MappingFixtureState()) {
    fun onAddresses(addresses: List<Address>) = updateState { copy(addresses = addresses) }

    fun onTitle(title: String) = updateState { copy(title = StringDesc.Raw(title)) }
}

interface FixtureAppRoute : Route

sealed interface FixtureRoute : FixtureAppRoute {
    data object Home : FixtureRoute
    data class Item(val payload: Address) : FixtureRoute
    data class Sheet(val id: Int) : FixtureRoute, SheetRoute
}

object ForeignRoute : Route

object FixtureNavigation {
    val navigator = StackNavigator(FixtureRoute.Home)
    val state: UiStateFlow<MappingFixtureState> = MutableStateFlow(MappingFixtureState()).asUiStateFlow()
}

interface FixtureStateHolder {
    val state: UiStateFlow<MappingFixtureState>?
}

enum class FixtureMode { Compact, Full }

/** Flows of values that are not data classes; the iOS tests read them through the generated accessors. */
class ValueFlows {
    private val counter = MutableStateFlow(1)
    private val selection = MutableStateFlow<Address?>(null)
    val count: UiStateFlow<Int> = counter.asUiStateFlow()
    val total: UiStateFlow<Long?> = MutableStateFlow<Long?>(null).asUiStateFlow()
    val enabled: UiStateFlow<Boolean> = MutableStateFlow(true).asUiStateFlow()
    val title: UiStateFlow<String> = MutableStateFlow("Hello").asUiStateFlow()
    val maybeTitle: UiStateFlow<String?> = MutableStateFlow<String?>(null).asUiStateFlow()
    val label: UiStateFlow<StringDesc> = MutableStateFlow<StringDesc>(StringDesc.Raw("Label")).asUiStateFlow()
    val mode: UiStateFlow<FixtureMode> = MutableStateFlow(FixtureMode.Compact).asUiStateFlow()
    val tags: UiStateFlow<List<String>> = MutableStateFlow(listOf("a", "b")).asUiStateFlow()
    val scores: UiStateFlow<Map<String, Int>> = MutableStateFlow(mapOf("a" to 1)).asUiStateFlow()
    val addresses: UiStateFlow<List<Address>> = MutableStateFlow(listOf(Address("Oslo", 150))).asUiStateFlow()
    val maybeAddresses: UiStateFlow<List<Address>?> = MutableStateFlow<List<Address>?>(null).asUiStateFlow()
    val selected: UiStateFlow<Address?> = selection.asUiStateFlow()
    val repository: UiStateFlow<OrdersRepository> = MutableStateFlow(OrdersRepository()).asUiStateFlow()
    val holder: UiStateFlow<FixtureStateHolder> = MutableStateFlow<FixtureStateHolder>(NoState).asUiStateFlow()
    val clicks: UiFlow<Int> = flowOf(1, 2, 3).asUiFlow()
    val ticks: UiFlow<Unit> = flowOf(Unit, Unit).asUiFlow()

    fun increment() {
        counter.value++
    }

    fun select(address: Address?) {
        selection.value = address
    }
}

private object NoState : FixtureStateHolder {
    override val state: UiStateFlow<MappingFixtureState>? = null
}
